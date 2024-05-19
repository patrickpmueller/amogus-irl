package com.pellacanimuller.amogus_irl.game.players;

import com.pellacanimuller.amogus_irl.game.Task;

import java.util.HashSet;
import java.util.Set;

public class Crewmate extends Player {
    public Set<Task> tasks = new HashSet<>(); // TODO read task count; fill set

    public void completeTask(Task task) {
        for (Task elem : tasks) {
            if (elem == task) {
                tasks.remove(task);
                break;
            }
        }
        if (tasks.isEmpty()) {
            // Run callback for completed tasks
        }
    }
}
