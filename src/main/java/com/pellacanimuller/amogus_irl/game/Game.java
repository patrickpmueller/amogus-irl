package com.pellacanimuller.amogus_irl.game;

import com.pellacanimuller.amogus_irl.game.players.Crewmate;
import com.pellacanimuller.amogus_irl.game.players.Healer;
import com.pellacanimuller.amogus_irl.game.players.Impostor;
import com.pellacanimuller.amogus_irl.game.players.Player;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import com.pellacanimuller.amogus_irl.util.TomlSettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Represents a game instance with settings and player management for the Amogus IRL game.
 */
public class Game {


    public enum GameState {
        LOBBY,
        INGAME,
        MEETING
    }

    public enum Role {
        IMPOSTOR,
        CREWMATE,
        HEALER
    }


    private final static Logger log = LogManager.getLogger(Game.class);

    /**
     * Maximum number of players allowed in the game.
     */
    public int MAX_PLAYERS;

    /**
     * Number of tasks assigned to each player.
     */
    public int TASKS_PER_PLAYER;

    /**
     * Number of impostors in the game.
     */
    public int IMPOSTOR_COUNT;

    /**
     * Number of crewmates in the game.
     */
    public int CREWMATE_COUNT;

    /**
     * Number of healers in the game.
     */
    public int HEALER_COUNT;

    /**
     * Total number of tasks in the game.
     */
    public int TASK_COUNT;

    /**
     * Duration of a meeting in the game.
     */
    public int MEETING_DURATION;

    /**
     * List of all players in the game.
     */
    public List<Player> players;

    /**
     * List of players who are alive in the game.
     */
    public List<Player> alive;

    /**
     * The current meeting happening in the game. Null if no meeting is ongoing.
     */
    public Meeting currentMeeting = null;

    /**
     * The current state of the game.
     */
    public GameState gameState = GameState.LOBBY;

    /**
     * Array of tasks in the game.
     */
    public Task[] tasks;

    /**
     * WebSocket server for the game.
     */
    private GameWSServer wsServer;


    /**
     * Number of tasksets crewmates have to complete to win the game.
     */
    private int tasksetsToWin;

    /**
     * Constructs a new game instance, initializing settings and tasks.
     */
    public Game() {
        Map<String, Object> settings = TomlSettingsManager.readSettingsAsMap();
        updateSettings(settings);

        players = new ArrayList<>(MAX_PLAYERS);

        // Create Tasks array
        tasks = new Task[TASK_COUNT];

        for (int i = 0; i < TASK_COUNT; i++) {
            tasks[i] = new Task();
            tasks[i].id = Integer.toString(i + 1);
        }

        tasksetsToWin = CREWMATE_COUNT + HEALER_COUNT;
    }

    /**
     * Returns true if the game is running, false otherwise.
     *
     * @return true if the game is running, false otherwise.
     */
    public boolean gameRunning() {
        return gameState != GameState.LOBBY;
    }

    public void checkWinConditions() {
        if (tasksetsToWin == 0) {
           endGame("crewmates");
           return;
        }

        long crewmates = alive.stream().filter(p -> p instanceof Crewmate).count();

        long impostors = alive.stream().filter(p -> p instanceof Impostor).count();

        if (impostors >= crewmates) {
           endGame("impostors");
        }
        if (impostors == 0) {
            endGame("crewmates");
        }
    }

    private void endGame(String winners) {
        wsServer.broadcast("[{\"type\": \"endGame\", \"winners\": \"" + winners + "\"}]");
        wsServer.resetGame(new Game(), true);
    }

    public void finishedTaskSet() {
        tasksetsToWin--;
    }

    /**
     * Kills a player in the game.
     *
     * @param player The player to kill.
     */
    public void healPlayer(Player player, Healer healer) {
        if (healer != null && alive.contains(healer)) {
            alive.add(player);
        }
    }

    /**
     * Ends the current meeting and resets the game state.
     */
    public void endMeeting(Player winner) {
        gameState = GameState.INGAME;
        currentMeeting = null;
        this.alive.remove(winner);

        wsServer.broadcast("[{\"type\": \"result\", \"data\": \"" + winner.id + "\"}]");
    }

    /**
     * Starts the game, assigning roles and tasks to players, and broadcasting game start to clients.
     * Pre-conditions: Enough players, WebSocket server initialized, tasks available, and game not already running.
     * Post-conditions: Game state set to INGAME, roles assigned to players, and game start broadcast.
     * @throws IllegalStateException If pre-conditions are not met.
     */
    public void startGame() throws IllegalStateException {
        if (IMPOSTOR_COUNT + CREWMATE_COUNT + HEALER_COUNT != players.size()) {
            throw new IllegalStateException("Cannot start game, wrong player count");
        }
        if (wsServer == null) {
            throw new IllegalStateException("Cannot start game, wsServer not initialized.");
        }
        if (TASKS_PER_PLAYER > tasks.length) {
            throw new IllegalStateException("Cannot start game, more tpp than available.");
        }
        if (gameState != GameState.LOBBY) {
            throw new IllegalStateException("Cannot start game, already running");
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
            WebSocket conn = wsServer.getConnectionByPlayer(old_player);
            Set<Task> task_set = new HashSet<>(TASKS_PER_PLAYER);
            int i = 0;
            while (i < TASKS_PER_PLAYER) {
                if (task_set.add(tasks[rand.nextInt(tasks.length)])) {
                    i++;
                }
            }

            conn.send("[{\"type\":\"tasks\", \"data\": " + Arrays.toString(task_set.stream().map(task -> "\"" + task.id + "\"").toArray()) + "}]");

            int index = rand.nextInt(roles_available.size());
            Role role = roles_available.get(index);
            roles_available.remove(index);

            Player newPlayer;

            switch (role) {
                case CREWMATE -> newPlayer = new Crewmate(old_player, task_set);
                case HEALER -> newPlayer = new Healer(old_player, task_set);
                case IMPOSTOR -> newPlayer = new Impostor(old_player, task_set);
                case null -> {
                    log.error("Could not start game, no role in array");
                    throw new RuntimeException();
                }
            }
            conn.setAttachment(newPlayer);
            return newPlayer;
        });

        alive = new ArrayList<>(players);

        wsServer.broadcast("[{\"type\":\"startGame\", \"data\": " + getRolesAsJson() + "}]");

        gameState = GameState.INGAME;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkWinConditions, 0, 2500, TimeUnit.MILLISECONDS);
    }

    /**
     * Acknowledges that the WebSocket server for the game has started.
     * @param server The WebSocket server instance.
     */
    public void acknowledgeServerStarted(GameWSServer server) {
        wsServer = server;
    }

    /**
     * Adds a player to the game by ID.
     * @param playerID The ID of the player to add.
     * @return The added Player instance.
     * @throws IllegalStateException If player with the same ID already exists or lobby is full.
     */
    public Player addPlayer(String playerID) throws IllegalStateException {
        if (playerID.isEmpty()) {
            return null;
        }
        if (players.stream().anyMatch(player -> Objects.equals(player.id, playerID))) {
            throw new IllegalStateException("Player with ID " + playerID + " already exists");
        }

        return addExistingPlayer(new Player(playerID));
    }

    /**
     * Adds an existing player instance to the game.
     * @param existing The existing Player instance to add.
     * @return The added Player instance.
     * @throws IllegalStateException If lobby is full or existing player ID is empty.
     */
    public Player addExistingPlayer(Player existing) {
        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("Cannot add more players, lobby full already");
        }
        if (existing.id.isEmpty()) {
            throw new IllegalStateException("Cannot add player with empty id");
        }

        Player player = existing.copy();
        players.add(player);
        return player;
    }

    /**
     * Removes a player from the game.
     * @param player The Player instance to remove.
     */
    public void removePlayer(Player player) {
        players.remove(player);
        alive.remove(player);
    }

    /**
     * Retrieves a player by their ID.
     * @param playerID The ID of the player to retrieve.
     * @return The Player instance corresponding to the ID.
     * @throws IndexOutOfBoundsException If player with given ID is not found.
     */
    public Player getPlayer(String playerID) throws IndexOutOfBoundsException {
        for (Player player : players) {
            if (playerID.equals(player.id)) {
                return player;
            }
        }
        throw new IndexOutOfBoundsException("Player " + playerID + " not found");
    }

    /**
     * Starts a meeting in the game, either emergency or report meeting.
     * Pre-conditions: Game state must be INGAME, starter must be alive, and player or emergency ID must exist.
     * Post-conditions: Game state set to MEETING, current meeting initiated.
     * @param starter The Player initiating the meeting.
     * @param deathID The ID of the player or emergency to start the meeting for.
     */
    public void startMeeting(Player starter, String deathID) {
        if (gameState != GameState.INGAME) {
            throw new IllegalStateException("Cannot start meeting, not ingame");
        }

        if (!alive.contains(starter)) {
            throw new IllegalStateException("Cannot start meeting, starter not alive");
        }
        Optional<Player> deathOptional = players.stream()
                .filter(player -> deathID.equals(player.id))
                .findFirst();

        if (deathOptional.isEmpty() && !deathID.contains("emergency")) {
            throw new IllegalStateException("Cannot start meeting, player doesn't exist");
        }

        if (Objects.equals(deathID, "emergency")) {
            log.debug("Starting emergency meeting");
            currentMeeting = new Meeting(this, null);
        } else {
            log.debug("Starting death report meeting, death of {}", deathID);
            currentMeeting = new Meeting(this, getPlayer(deathID));
        }

        wsServer.broadcast("[{ \"type\": \"meeting\", \"data\": \"" + deathID +  "\" }]");
        gameState = GameState.MEETING;
    }

    /**
     * Completes a task for a crewmate player.
     * Pre-conditions: Game state must be INGAME, and player must be an instance of Crewmate.
     * @param player The Crewmate player completing the task.
     * @param taskID The ID of the task to complete.
     */
    public void completeTask(Player player, String taskID) {
        if (gameState != GameState.INGAME) {
            throw new IllegalStateException(new IllegalAccessException("Cannot complete task, not ingame"));
        }
        if (!(player instanceof Crewmate)) {
            return;
        }

        for (Task task : tasks) {
            if (task.id.equals(taskID)) {
                Crewmate crewmate = (Crewmate) player;
                crewmate.completeTask(task, this::finishedTaskSet);
                log.debug("Task {} completed", taskID);
            }
        }
    }

    /**
     * Marks a task as incomplete for a crewmate player.
     * Pre-conditions: Player must be an instance of Crewmate.
     * @param player The Crewmate player marking the task as incomplete.
     * @param taskID The ID of the task to mark as incomplete.
     */
    public void incompleteTask(Player player, String taskID) {
        if (!(player instanceof Crewmate)) {
            return;
        }

        for (Task task : tasks) {
            if (task.id.equals(taskID)) {
                Crewmate crewmate = (Crewmate) player;
                crewmate.incompleteTask(task);
                log.debug("Task {} marked as incomplete", taskID);
            }
        }
    }

    /**
     * Updates game settings based on provided map of settings.
     * @param settings The map containing updated game settings.
     * @throws IllegalStateException If unexpected value type encountered in settings map.
     */
    public void updateSettings(Map<String, Object> settings) {
        TomlSettingsManager.flattenMap(settings, ".").forEach((key, value) -> {
            if (Objects.requireNonNull(value) instanceof Integer i) {
                switch (key) {
                    case "roles.impostors" -> IMPOSTOR_COUNT = i;
                    case "roles.crewmates" -> CREWMATE_COUNT = i;
                    case "roles.healers" -> HEALER_COUNT = i;
                    case "tasks.total" -> TASK_COUNT = i;
                    case "tasks.perPlayer" -> TASKS_PER_PLAYER = i;
                    case "maxPlayers" -> MAX_PLAYERS = i;
                    case "meeting.duration" -> MEETING_DURATION = i;
                    default -> log.error("Cannot parse int value: {}", key);
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + value);
            }
        });
    }

    /**
     * Generates and returns roles of players in JSON format.
     * @return JSON representation of roles assigned to players.
     */
    private String getRolesAsJson() {
        JsonArrayBuilder array = Json.createArrayBuilder();

        players.forEach((player) -> {
            JsonObjectBuilder obj = Json.createObjectBuilder();
            switch (player) {
                case Healer _ -> obj.add("player", player.id).add("role", "healer");
                case Crewmate _ -> obj.add("player", player.id).add("role", "crewmate");
                case Impostor _ -> obj.add("player", player.id).add("role", "impostor");
                default -> throw new IllegalStateException("Unexpected value: " + player);
            }
            array.add(obj.build());
        });

        return array.build().toString();
    }
}
