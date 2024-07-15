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
 * WebSocket server for managing game connections and interactions.
 * Handles game state, player connections, and game-related messages.
 *
 * @author @pellacanimuller
 */
public class GameWSServer extends WebSocketServer {
    /**
     * The game instance associated with this WebSocket server.
     */
    private Game game;

    /**
     * The logger for the GameWSServer class.
     */
    private final static Logger log = LogManager.getLogger(GameWSServer.class);

    /**
     * Constructs a new GameWSServer.
     *
     * @param addr the socket address to bind to.
     * @param game the game instance to manage.
     */
    public GameWSServer(InetSocketAddress addr, Game game) {
        super(addr);
        this.game = game;
        game.acknowledgeServerStarted(this);
    }

    /**
     * Starts the WebSocket server and sets the running state to true.
     */
    @Override
    public void start() {
        super.start();
    }

    /**
     * Stops the WebSocket server and sets the running state to false.
     *
     * @throws InterruptedException if the stop operation is interrupted.
     */
    @Override
    public void stop() throws InterruptedException {
        super.stop();
    }

    /**
     * Resets the game with the given game instance.
     *
     * @param game   the new game instance.
     * @param isHard if true, resets connections; if false, retains existing players.
     */
    public void resetGame(Game game, boolean isHard) {
        this.game = game;
        game.acknowledgeServerStarted(this);
        if (!isHard) {
            this.getConnections().forEach(conn -> conn.setAttachment(
                    Objects.equals(((Player) conn.getAttachment()).id, "")
                            ? conn.getAttachment() : game.addExistingPlayer(conn.getAttachment())));
        } else {
            this.getConnections().forEach(conn -> conn.setAttachment(null));
        }
        log.info("GAME RESET");
        broadcastInfo();
    }

    /**
     * Handles a new player connection.
     *
     * @param conn      the WebSocket connection.
     * @param handshake the client handshake.
     */
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

    /**
     * Handles a player disconnection.
     *
     * @param conn   the WebSocket connection.
     * @param code   the close code.
     * @param reason the reason for closing.
     * @param remote true if closed by remote host.
     */
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

    /**
     * Handles errors on the WebSocket connection.
     *
     * @param conn the WebSocket connection.
     * @param ex   the exception thrown.
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Error on connection {}, Stack Trace: \n{}", conn.getRemoteSocketAddress(), ex.getStackTrace());
        // TODO error handling
        conn.close();
    }

    /**
     * Retrieves the WebSocket connection associated with a given player.
     *
     * @param player the player.
     * @return the WebSocket connection, or null if not found.
     */
    public WebSocket getConnectionByPlayer(Player player) {
        return getConnections().stream().filter(conn -> Objects.equals(conn.getAttachment(), player)).findFirst().orElse(null);
    }

    /**
     * Handles incoming messages from players.
     *
     * @param conn the WebSocket connection.
     * @param msg  the message received.
     */
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
                            if (game.gameRunning()) {
                                game.players.forEach(currentPlayer -> {
                                    if (Objects.equals(currentPlayer.id, playerID)) {
                                        conn.setAttachment(currentPlayer);
                                    }
                                });
                                sendTasks(conn);
                            } else {
                                if (Objects.equals(playerID, "in_settings")) {
                                    game.removePlayer(conn.getAttachment());
                                }
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

    private void sendTasks(WebSocket socket) {
        Task[] tasks;
        if (socket.getAttachment() instanceof Impostor) {
            tasks = ((Impostor) socket.getAttachment()).mockTasks.toArray(new Task[0]);
        } else {
            tasks = ((Crewmate) socket.getAttachment()).tasks.toArray(new Task[0]);
        }
    }

    /**
     * Logs the start of the server.
     */
    @Override
    public void onStart() {
        log.info("SERVER STARTED");
    }

    /**
     * Broadcasts the current game information to all connected players.
     */
    private void broadcastInfo() {
        broadcast("[{\"type\": \"playerlist\",\"data\": [\"" + getConnections().stream().map(con -> ((Player) con.getAttachment()).id).collect(Collectors.joining("\",\"")) + "\"]}," +
                "{\"type\": \"settings\", \"data\": " + TomlSettingsManager.readSettingsAsJson() + "}]");
    }
}
