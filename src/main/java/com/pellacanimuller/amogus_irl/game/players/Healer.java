package com.pellacanimuller.amogus_irl.game.players;

import com.pellacanimuller.amogus_irl.game.Task;

import java.util.Set;

/**
 * Represents a Healer player in the game.
 */
public class Healer extends Crewmate {

    /**
     * Constructs a new Healer player.
     *
     * @param player the Player object representing the player
     * @param tasks the set of Task objects representing the tasks
     */
    public Healer(Player player, Set<Task> tasks) {
        super(player, tasks);
    }

    /**
     * Creates a copy of the Healer player.
     *
     * @return a copy of the Healer player
     */
    @Override
    public Player copy() {
        return super.copy();
    }
}


