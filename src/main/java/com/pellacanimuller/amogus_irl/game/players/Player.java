package com.pellacanimuller.amogus_irl.game.players;

import java.util.Objects;

/**
 * Represents a player in the game.
 */
public class Player {
    /**
     * The unique identifier of the player.
     */
    public String id;

    /**
     * Creates a new player with the given ID.
     *
     * @param id The unique identifier of the player.
     */
    public Player(String id) {
        this.id = id;
    }

    /**
     * Checks if this player is equal to another object.
     *
     * @param o The object to compare to.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return Objects.equals(id, player.id);
    }

    /**
     * Returns a hash code value for the player.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Creates a copy of the player.
     *
     * @return A new player with the same ID as this player.
     */
    public Player copy() {
        return new Player(id);
    }
}
