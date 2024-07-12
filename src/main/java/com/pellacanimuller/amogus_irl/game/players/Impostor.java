package com.pellacanimuller.amogus_irl.game.players;

import com.pellacanimuller.amogus_irl.game.Task;

import java.util.Set;

public class Impostor extends Player {
    public Set<Task> mockTasks;

    public Impostor(Player player, Set<Task> tasks) {
        super(player.id);
        this.mockTasks = tasks;
    }

    @Override
    public Player copy() {
        return super.copy();
    }
}
