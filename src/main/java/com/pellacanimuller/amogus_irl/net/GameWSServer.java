package com.pellacanimuller.amogus_irl.net;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.game.players.Player;
import com.pellacanimuller.amogus_irl.util.TomlSettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * GameWSServer
 */
public class GameWSServer extends WebSocketServer {
    private Game game;
    private final static Logger log = LogManager.getLogger( GameWSServer.class );

    public GameWSServer(InetSocketAddress addr, Game game) {
        super(addr);
        this.game = game;
        game.acknowledgeServerStarted(this);
    }

    public void resetGame(Game game) {
        this.game = game;
        this.getConnections()
                .forEach(conn -> conn.setAttachment(game.addExistingPlayer(conn.getAttachment())));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            Player player = game.addPlayer();
            conn.setAttachment(player);
            log.info("Player connected");
        } catch (IllegalStateException e) {
           log.info("Cannot add another player, lobby full");
           conn.close();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        game.removePlayer(conn.getAttachment());
        log.info("Player disconnected");
        broadcast("[{\"type\": \"playerlist\",\"data\": [\"" + getConnections().stream().map(con -> ((Player) con.getAttachment()).id).collect(Collectors.joining("\",\"")) + "\"]}]");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Error on connection {}, Stack Trace: \n{}", conn.getRemoteSocketAddress(), ex.getStackTrace());
        // TODO error handling
    }

    public void updateAttachment(Player old_player, Player new_player) {
        getConnections().forEach(conn -> {
            if (Objects.equals(conn.getAttachment(), old_player)) {
                conn.setAttachment(new_player);
            }
        });
    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        log.debug("Message '{}' received", msg);
        Player player = conn.getAttachment();
        try (JsonReader reader = Json.createReader(new StringReader(msg))) {
            JsonArray actions = reader.readArray();
            actions.forEach(
                arg -> {
                    JsonObject actionObj = arg.asJsonObject();
                    String action = actionObj.getString("action");
                    log.info("Action '{}' parsed. Trying to fulfil action.", action);
                    switch (action) {
                        case "vote" -> {
                            String target = actionObj.getString("target").strip();
                            Player voted;
                            if (Objects.equals(target, "skip")) {
                                voted = null;
                            } else {
                                try {
                                    voted = game.getPlayer(target);
                                } catch (Exception e) {
                                    log.error(e);
                                    return;
                                }
                            }
                            if (game.currentMeeting != null) {
                                game.currentMeeting.vote(voted);
                            } else {
                                log.info("No meeting running, but a vote was requested");
                            }
                        }
                        case "setup" -> {
                            String playerID = actionObj.getString("playerID");
                            if (!Objects.equals(playerID, "in_settings")) {
                                ((Player) conn.getAttachment()).id = playerID;
                            }
                            broadcastInfo();
                        }
                        case "taskCompleted" -> {
                            String id = actionObj.getString("id");
                            game.completeTask(player, id);
                        }
                        case "meeting" -> {
                            String death = actionObj.getString("death");
                            game.startMeeting(player, death);
                        }
                        case "kill" -> {
                            String target = actionObj.getString("player");
                            game.alive.remove(game.getPlayer(target));
                        }
                        case "startGame" -> game.startGame();
                        case "changeSettings" -> {
                            try {
                                TomlSettingsManager.changeSettingsFromJson(actionObj.getJsonObject("settings"), game);
                            } catch (NumberFormatException e) {
                                log.error(e.getStackTrace());
                            }
                            broadcastInfo();
                        }
                        default -> log.info("Action '{}' not recognised.", action);
                    }
                }
            );
        }
    }

    @Override
    public void onStart() { 
        log.info("SERVER STARTED");
    }

    private void broadcastInfo() {
        broadcast("[{\"type\": \"playerlist\",\"data\": [\"" + getConnections().stream().map(con -> ((Player) con.getAttachment()).id).collect(Collectors.joining("\",\"")) + "\"]}," +
                "{\"type\": \"settings\", \"data\": " + TomlSettingsManager.readSettingsAsJson() + "}]");
    }
}