package com.pellacanimuller.amogus_irl.game;

import com.pellacanimuller.amogus_irl.game.players.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Meeting {
    public Map<Player, Integer> votes;
    public final Game game;
    public Set<Player> voters;
    public final Player death;
    private static final Logger log = LogManager.getLogger(Meeting.class);
    
    public Meeting(Game game, Player death) {
        // Copy into global vars
        this.game = game;
        this.death = death;

        // Players that have voted
        voters = new HashSet<>();

        // Vote tally
        votes = new HashMap<>(game.alive.size() + 1);
        for (Player player : game.alive) {
            votes.put(player, 0);
        }

        // 'Skip' is the last element, null player
        votes.put(null, 0);
    }

    public void vote(Player player) {
        // If player is dead, do not allow voting
        if (!game.alive.contains(player)) 
            return;

        // Add player to voters list and check if it is in there
        if (!voters.add(player))
            return;
        

        // 'Skip' if player == null
        if (player == null) {
            log.debug("Vote: Skipped");
            votes.replace(null, votes.get(null));
        } else {
            // Add the votes to the map
            log.debug("Vote: " + player.id);
            votes.replace(player, votes.get(player));
        }
    }
}
