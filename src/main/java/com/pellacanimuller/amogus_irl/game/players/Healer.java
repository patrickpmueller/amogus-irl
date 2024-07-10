package com.pellacanimuller.amogus_irl.game.players;

import com.pellacanimuller.amogus_irl.game.Task;

import java.util.Set;

public class Healer extends Crewmate {
   public Healer(Player player, Set<Task> tasks) {
       super(player, tasks);
   }

    @Override
    public Player copy() {
        return super.copy();
    }
}
