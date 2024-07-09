package com.pellacanimuller.amogus_irl.game;

import com.pellacanimuller.amogus_irl.game.players.Crewmate;
import com.pellacanimuller.amogus_irl.game.players.Healer;
import com.pellacanimuller.amogus_irl.game.players.Impostor;
import com.pellacanimuller.amogus_irl.game.players.Player;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public int MAX_PLAYERS = 8; // TODO read config
    public int TASKS_PER_PLAYER = 4;
    public int IMPOSTOR_COUNT = 1;
    public int CREWMATE_COUNT = 2;
    public int HEALER_COUNT = 1;
    public int TASK_COUNT = 8;

    public List<Player> players = new ArrayList<>(MAX_PLAYERS);
    public List<Player> alive = new ArrayList<>(MAX_PLAYERS);
    public Meeting currentMeeting = null;
    private GameState gameState = GameState.LOBBY;
    public Task[] tasks;
    private GameWSServer wsServer;

    private static final long SEC = 1000;


    public Game() {
        // Create Tasks array
        tasks = new Task[TASK_COUNT];

        for (int i = 0; i < TASK_COUNT; i++) {
           tasks[i] = new Task();
           tasks[i].id = Integer.toString(i);
        }
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

        gameState = GameState.INGAME;
    }

    public void acknowledgeServerStarted(GameWSServer server) {
        wsServer = server;
    }

    public Player addPlayer() {
        // Create player and increment count
        Player player = new Player("");
        // Add and return player
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

    public void updateSetting(String key, String value) {
        switch (key) {
            case "impostors" -> IMPOSTOR_COUNT = Integer.parseInt(value);
            case "crewmates" -> CREWMATE_COUNT = Integer.parseInt(value);
            case "healers" -> HEALER_COUNT = Integer.parseInt(value);
            case "total" -> TASK_COUNT = Integer.parseInt(value);
            case "perPlayer" -> TASKS_PER_PLAYER = Integer.parseInt(value);
            case "maxPlayers" -> MAX_PLAYERS = Integer.parseInt(value);
            default -> log.error("Cannot parse {}", key);
        }
    }
}
