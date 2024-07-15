package com.pellacanimuller.amogus_irl.game;

import java.util.Objects;

/**
 * Represents a task in the game.
 */
public class Task {
    /**
     * The unique identifier for the task.
     */
    public String id;

    /**
     * Checks if two Task objects are equal.
     *
     * @param o The object to compare.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return Objects.equals(id, task.id);
    }

    /**
     * Returns the hash code for this Task object.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
