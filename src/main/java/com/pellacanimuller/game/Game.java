package com.pellacanimuller.game;

import com.pellacanimuller.amogus_irl.game.players.Crewmate;
import com.pellacanimuller.amogus_irl.game.players.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Game {
    private final static Logger log = LogManager.getLogger(Game.class);

    public final int MAX_PLAYERS = 8; // TODO read config
    public Set<Player> players = new HashSet<>(MAX_PLAYERS);
    public Set<Player> alive = new HashSet<>(MAX_PLAYERS);
    public int playerCount = 0;
    public Meeting currentMeeting = null;
    public Task[] tasks;


    public Game(int ntasks) {
        // Create Tasks array
        tasks = new Task[ntasks];
        log.warn("Tasks not initialised");
        // TODO create tasks array
    }

    public Player addPlayer() {
        // Create player and increment count
        playerCount++;
        Player player = new Player();
        // Add and return player
        players.add(player);
        return player;
    }

    // Removes player from list
    public void removePlayer(Player player) {
        // Decrement count and remove player
        playerCount--;
        players.remove(player);
    }

    public Player getPlayer(String playerID) throws IndexOutOfBoundsException {
        // Iterate through players
        for (Player player : players) {
            // Right player is found
            if (player.id == playerID) {
                return player;
            }
        }
        // Player not found
        throw new IndexOutOfBoundsException("Player " + playerID + " not found");
    }

    public void startMeeting(Player starter, String deathID) {
        // Do not allow two meetings at the same time
        if (currentMeeting != null) {
            throw new IllegalStateException(new IllegalAccessException("Cannot start meeting, meeting ongoing"));
        }

        // start meeting depending on if it is report or emergency
        if (Objects.equals(deathID, "emergency")) {
            log.debug("Starting emergency meeting");
            currentMeeting = new Meeting(this, null);
        } else {
            log.debug("Starting death report meeting, death of " + deathID);
            currentMeeting = new Meeting(this, getPlayer(deathID));
        }
    }

    public void completeTask(Player player, String taskID) {
        // Do not allow task completions during meeting
        if (currentMeeting != null) {
            throw new IllegalStateException(new IllegalAccessException("Cannot complete task, meeting ongoing"));
        }
        // Only crewmates can do tasks
        if (!(player instanceof Crewmate))
            return;

        // Find task object relating to ID
        for (Task task : tasks) {
            if (task.id == taskID) {
                // complete task
                Crewmate crewmate = (Crewmate) player;
                crewmate.completeTask(task);
                log.debug("Task " + taskID + "completed");
            }
        }
    }
}
