package com.pellacanimuller.amogus_irl.net;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.game.players.Player;
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

/**
 * GameWSServer
 */
public class GameWSServer extends WebSocketServer {
    public final int MAX_PLAYERS = 8; // TODO read config
    private final Game game;
    private final static Logger log = LogManager.getLogger( GameWSServer.class );

    public GameWSServer(InetSocketAddress addr, Game game) {
        super(addr);
        this.game = game;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Player player = game.addPlayer();
        conn.setAttachment(player);
        log.debug("Player connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        game.removePlayer(conn.getAttachment());
        log.debug("Player disconnected");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Error on connection {}, Stack Trace: \n{}", conn.getRemoteSocketAddress(), ex.getStackTrace());
        // TODO error handling
    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        log.debug("Message '{}' recieved", msg);
        Player player = conn.getAttachment();
        try (JsonReader reader = Json.createReader(new StringReader(msg))) {
            JsonArray actions = reader.readArray();
            actions.forEach(
                arg -> {
                    JsonObject actionObj = arg.asJsonObject();
                    String action = actionObj.getString("action");
                    log.debug("Action '{}' parsed. Trying to fulfil action.", action);
                    switch (action) {
                        case "vote":
                        {
                            String target = actionObj.getString("target");
                            target = target.strip();
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
                            if (game.currentMeeting != null)
                                game.currentMeeting.vote(voted);
                            else
                                log.info("No meeting running, but a vote was requested");
                            break;
                        }
                        case "setup":
                        {
                            ((Player) conn.getAttachment()).id = actionObj.getString("playerID");
                        }
                        case "taskCompleted":
                        {
                            String id = actionObj.getString("id");
                            game.completeTask(player, id);
                            break;
                        }
                        case "meeting":
                        {
                            String death = actionObj.getString("death");
                            game.startMeeting(player, death);
                            break;
                        }
                        case "kill":
                        {
                            String target = actionObj.getString("player");
                            game.alive.remove(game.getPlayer(target));
                            break;
                        }
                        case "start_game":
                        {
                            game.startGame();
                            break;
                        }
                        default:
                            log.debug("Action '{}' not recognised.", action);
                            break;
                    }
                }
            );
        }
    }

    @Override
    public void onStart() { 
        log.info("SERVER STARTED");
    }
}