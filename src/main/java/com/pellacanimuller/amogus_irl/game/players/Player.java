package com.pellacanimuller.amogus_irl.game.players;

import java.util.Objects;

public class Player {
    public String id;

    public Player(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Player copy() {
        return new Player(id);
    }
}
