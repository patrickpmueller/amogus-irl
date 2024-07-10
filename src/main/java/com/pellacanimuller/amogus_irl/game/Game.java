package com.pellacanimuller.amogus_irl.game;

import com.pellacanimuller.amogus_irl.game.players.Crewmate;
import com.pellacanimuller.amogus_irl.game.players.Healer;
import com.pellacanimuller.amogus_irl.game.players.Impostor;
import com.pellacanimuller.amogus_irl.game.players.Player;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import com.pellacanimuller.amogus_irl.util.TomlSettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.*;

enum GameState {
    LOBBY,
    INGAME,
    MEETING
}

enum Role {
    IMPOSTOR,
    CREWMATE,
    HEALER
}

public class Game {
    private final static Logger log = LogManager.getLogger(Game.class);

    public int MAX_PLAYERS;
    public int TASKS_PER_PLAYER;
    public int IMPOSTOR_COUNT;
    public int CREWMATE_COUNT;
    public int HEALER_COUNT;
    public int TASK_COUNT;

    public List<Player> players;
    public List<Player> alive;
    public Meeting currentMeeting = null;
    private GameState gameState = GameState.LOBBY;
    public Task[] tasks;
    private GameWSServer wsServer;

    public Game() {
        updateSettings(TomlSettingsManager.readSettingsAsMap());

        players = new ArrayList<>(MAX_PLAYERS);

        // Create Tasks array
        tasks = new Task[TASK_COUNT];

        for (int i = 0; i < TASK_COUNT; i++) {
           tasks[i] = new Task();
           tasks[i].id = Integer.toString(i);
        }
    }


    public boolean gameRunning() {
        return gameState != GameState.LOBBY;
    }

    public void startGame() {
        if (IMPOSTOR_COUNT + CREWMATE_COUNT + HEALER_COUNT != players.size()) {
            log.info("Cannot start game, wrong player count");
            return;
        }
        if (wsServer == null) {
            log.info("Cannot start game, wsServer not initialized.");
            return;
        }
        if (TASKS_PER_PLAYER > tasks.length) {
            log.info("Cannot start game, more tpp than available.");
            return;
        }

        if (gameState != GameState.LOBBY) {
            log.info("Cannot start game, already running");
            return;
        }

        List<Role> roles_available = new ArrayList<>();
        for (int i = 0; i < IMPOSTOR_COUNT; i++) {
            roles_available.add(Role.IMPOSTOR);
        }
        for (int i = 0; i < CREWMATE_COUNT; i++) {
            roles_available.add(Role.CREWMATE);
        }
        for (int i = 0; i < HEALER_COUNT; i++) {
            roles_available.add(Role.HEALER);
        }

        Random rand = new Random();
        players.replaceAll(old_player -> {
            Set<Task> task_set = new HashSet<>(TASKS_PER_PLAYER);
            int i = 0;
            while (i < TASKS_PER_PLAYER) {
               if (task_set.add(tasks[rand.nextInt(tasks.length)])) {
                   i++;
                }
            }

            int index = rand.nextInt(roles_available.size());
            Role role = roles_available.get(index);
            roles_available.remove(index);

            Player new_player = switch (role) {
                case CREWMATE -> new Crewmate(old_player, task_set);
                case HEALER -> new Healer(old_player, task_set);
                case IMPOSTOR -> new Impostor(old_player);
                case null -> {
                    log.error("Could not start game, no role in array");
                    throw new RuntimeException();
                }
            };
            wsServer.updateAttachment(old_player, new_player);
            return new_player;
        });

        alive = new ArrayList<>(players);

        wsServer.broadcast("[{\"type\":\"startGame\", \"roles\": " + getRolesAsJson() + "}]");

        gameState = GameState.INGAME;
    }

    public void acknowledgeServerStarted(GameWSServer server) {
        wsServer = server;
    }

    public Player addPlayer() throws IllegalStateException {
        return addExistingPlayer(new Player(""));
    }

    public Player addExistingPlayer(Player existing) {
        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("Cannot add more players, lobby full already");
        }

        Player player = existing.copy();
        players.add(player);
        return player;
    }

    // Removes player from list
    public void removePlayer(Player player) {
        // Decrement count and remove player
        players.remove(player);
    }

    public Player getPlayer(String playerID) throws IndexOutOfBoundsException {
        // Iterate through players
        for (Player player : players) {
            // Right player is found
            if (player.id.equals(playerID)) {
                return player;
            }
        }
        // Player not found
        throw new IndexOutOfBoundsException("Player " + playerID + " not found");
    }

    public void startMeeting(Player starter, String deathID) {
        // Do not allow meeting to start if not ingame
        if (gameState != GameState.INGAME) {
            throw new IllegalStateException(new IllegalAccessException("Cannot start meeting, not ingame"));
        }

        // start meeting depending on if it is report or emergency
        if (Objects.equals(deathID, "emergency")) {
            log.debug("Starting emergency meeting");
            currentMeeting = new Meeting(this, null);
        } else {
            log.debug("Starting death report meeting, death of {}", deathID);
            currentMeeting = new Meeting(this, getPlayer(deathID));
        }
        gameState = GameState.MEETING;
    }

    public void completeTask(Player player, String taskID) {
        // Do not allow task completions during meeting
        if (gameState != GameState.INGAME) {
            throw new IllegalStateException(new IllegalAccessException("Cannot complete task, not ingame"));
        }
        // Only crewmates can do tasks
        if (!(player instanceof Crewmate))
            return;

        // Find task object relating to ID
        for (Task task : tasks) {
            if (task.id.equals(taskID)) {
                // complete task
                Crewmate crewmate = (Crewmate) player;
                crewmate.completeTask(task);
                log.debug("Task {} completed", taskID);
            }
        }
    }

    public void updateSettings(Map<String, Object> settings) {
        TomlSettingsManager.flattenMap(settings, ".").forEach((key, value) -> {
            switch (value) {
                case Long l -> {
                    int i = l.intValue();
                    switch (key) {
                        case "roles.impostors" -> IMPOSTOR_COUNT = i;
                        case "roles.crewmates" -> CREWMATE_COUNT = i;
                        case "roles.healers" -> HEALER_COUNT = i;
                        case "tasks.total" -> TASK_COUNT = i;
                        case "tasks.perPlayer" -> TASKS_PER_PLAYER = i;
                        case "maxPlayers" -> MAX_PLAYERS = i;
                        default -> log.error("Cannot parse int value: {}", key);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + value);
            }
        });
    }

    private String getRolesAsJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        players.forEach((player) -> {
            switch (player) {
                case Healer _ -> builder.add(player.id, "healer");
                case Crewmate _ -> builder.add(player.id, "crewmate");
                case Impostor _ -> builder.add(player.id, "impostor");
                default -> throw new IllegalStateException("Unexpected value: " + player);
            }
        });

        return builder.build().toString();
    }
}
