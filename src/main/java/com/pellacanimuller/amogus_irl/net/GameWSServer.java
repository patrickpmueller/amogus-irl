package com.pellacanimuller.amogus_irl.net;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.game.players.Healer;
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
 * @author @pellacanimuller
 *
 */
public class GameWSServer extends WebSocketServer {
    private Game game;
    private final static Logger log = LogManager.getLogger( GameWSServer.class );

    public GameWSServer(InetSocketAddress addr, Game game) {
        super(addr);
        this.game = game;
        game.acknowledgeServerStarted(this);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() throws InterruptedException {
        super.stop();
    }

    /**
     * Resets the game.
     *
     * @param game the new game
     * @param isHard if true, resets the player list
     */
    public void resetGame(Game game, boolean isHard) {
        this.game.destroy();
        this.game = game;
        game.acknowledgeServerStarted(this);
        if (!isHard) {
            this.getConnections()
                    .forEach(conn -> conn.setAttachment(
                            Objects.equals(((Player) conn.getAttachment()).id, "")
                                    ? conn.getAttachment() : game.addExistingPlayer(conn.getAttachment())));
        } else {
            this.getConnections().forEach(conn -> conn.setAttachment(null));
        }
        log.info("GAME RESET");
        broadcastInfo();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            conn.setAttachment(null);
            log.info("Player connected");
        } catch (IllegalStateException e) {
            log.info("Cannot add another player, lobby full");
            conn.close();
        }
        broadcastInfo();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (game.gameRunning()) {
            if (conn.getAttachment() == null) {
                log.info("Lost connection to player, address {}", conn.getRemoteSocketAddress());
            } else {
                log.info("Lost connection to playerID {}, address {}", ((Player) conn.getAttachment()).id, conn.getRemoteSocketAddress());
            }
        } else {
            game.removePlayer(conn.getAttachment());
            log.info("Player at address: {} disconnected", conn.getRemoteSocketAddress());
            broadcastInfo();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.debug("Error on connection {}, Stack Trace: \n{}", conn.getRemoteSocketAddress(), ex.getStackTrace());
        conn.close();
    }

    public WebSocket getConnectionByPlayer(Player player) {
        return getConnections().stream().filter(conn -> Objects.equals(conn.getAttachment(), player)).findFirst().orElse(null);
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
                        log.debug("Action '{}' parsed. Trying to fulfil action.", action);
                        switch (action) {
                            case "vote" -> {
                                String target = actionObj.getString("target").strip();
                                Player vote;
                                if (Objects.equals(target, "skip")) {
                                    vote = null;
                                } else {
                                    try {
                                        vote = game.getPlayer(target);
                                    } catch (Exception e) {
                                        log.error(e.getMessage());
                                        return;
                                    }
                                }
                                if (game.currentMeeting != null) {
                                    game.currentMeeting.vote(vote, player);
                                } else {
                                    log.info("No meeting running, but a vote was requested");
                                }
                            }
                            case "setup" -> {
                                String playerID = actionObj.getString("playerID");
                                if (Objects.equals(playerID, "")) {
                                    break;
                                }
                                if (game.gameRunning()) {
                                    for (Player currentPlayer : game.players) {
                                        if (Objects.equals(currentPlayer.id, playerID)) {
                                            conn.setAttachment(currentPlayer);
                                        }
                                    }
                                } else {
                                    try {
                                        conn.setAttachment(game.addPlayer(playerID));
                                    } catch (IllegalStateException e) {
                                        log.info(e.getMessage());
                                    }
                                }
                                broadcastInfo();
                            }
                            case "taskCompleted" -> {
                                String id = actionObj.getString("taskID");
                                try {
                                    game.completeTask(player, id);
                                } catch (IllegalStateException e) {
                                    log.info(e.getMessage());
                                }
                            }
                            case "taskUncompleted" -> {
                                String id = actionObj.getString("taskID");
                                game.incompleteTask(player, id);
                            }
                            case "meeting" -> {
                                String death = actionObj.getString("death");
                                try {
                                    game.startMeeting(player, death);
                                } catch (IllegalStateException e) {
                                    log.info(e.getMessage());
                                }
                            }
                            case "heal" -> {
                                String target = actionObj.getString("playerID");
                                if (player instanceof Healer) {
                                    game.healPlayer(game.getPlayer(target), (Healer) player);
                                }
                            }
                            case "startGame" -> {
                                try {
                                    game.startGame();
                                } catch (IllegalStateException e) {
                                    log.info(e.getMessage());
                                }
                            }
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

    public void broadcastInfo() {
        broadcast("[{\"type\": \"playerlist\",\"data\": [\"" + getConnections().stream()
                .filter(conn -> conn.getAttachment() != null)
                .map(con -> ((Player) con.getAttachment()).id)
                .distinct()
                .collect(Collectors.joining("\",\"")) + "\"]}," +
                "{\"type\": \"settings\", \"data\": " + TomlSettingsManager.readSettingsAsJson() + "}]");
    }
}