package com.pellacanimuller.amogus_irl.game.players;

import com.pellacanimuller.amogus_irl.game.Task;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a crewmate in the game.
 */
public class Crewmate extends Player {
    /**
     * The tasks that the crewmate has completed.
     */
    public Set<Task> tasks;

    /**
     * Creates a new Crewmate with the given player and tasks.
     *
     * @param player The player to base the crewmate on.
     * @param tasks The tasks that the crewmate has completed.
     */
    public Crewmate(Player player, Set<Task> tasks) {
        super(player.id);
        this.tasks = tasks;
    }

    /**
     * Completes the given task if it is in the crewmate's task list.
     *
     * @param task The task to complete.
     */
    public void completeTask(Task task, Runnable onComplete) {
        for (Task elem : tasks) {
            if (elem == task) {
                tasks.remove(task);
                break;
            }
        }
        if (tasks.isEmpty()) {
            onComplete.run();
        }
    }

    /**
     * Returns a copy of this crewmate.
     *
     * @return A copy of this crewmate.
     */
    @Override
    public Player copy() {
        return new Crewmate(super.copy(), new HashSet<>(this.tasks));
    }

    /**
     * Adds the given task to the crewmate's task list.
     *
     * @param task The task to add.
     */
    public void incompleteTask(Task task, Runnable onIncomplete) {
        if (tasks.isEmpty()) {
            onIncomplete.run();
        }
        tasks.add(task);

    }
}