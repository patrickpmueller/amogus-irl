package com.pellacanimuller.amogus_irl.game.players;

import com.pellacanimuller.amogus_irl.game.Task;

import java.util.HashSet;
import java.util.Set;

/**
 * A class representing an Impostor player in the game.
 */
public class Impostor extends Player {

    /**
     * The set of tasks that the Impostor is mocking.
     */
    public Set<Task> mockTasks;

    /**
     * Constructs a new Impostor player.
     *
     * @param player the player that the Impostor is based on
     * @param tasks the set of tasks that the Impostor is mocking
     */
    public Impostor(Player player, Set<Task> tasks) {
        super(player.id);
        this.mockTasks = tasks;
    }

    /**
     * Creates a copy of the Impostor player.
     *
     * @return a copy of the Impostor player
     */
    @Override
    public Player copy() {
        return new Impostor(super.copy(), new HashSet<>(this.mockTasks));
    }
}
