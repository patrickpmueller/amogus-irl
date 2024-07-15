package com.pellacanimuller.amogus_irl.game;

import com.pellacanimuller.amogus_irl.game.players.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a meeting within the game.
 */
public class Meeting {
    /**
     * Map to store the votes for each player.
     */
    public Map<Player, Integer> votes;

    /**
     * The game associated with the meeting.
     */
    public final Game game;

    /**
     * The players that have voted.
     */
    public Set<Player> voters;

    /**
     * The player's death that caused the meeting.
     */
    public final Player death;

    /**
     * The logger instance for this class.
      */
    private static final Logger log = LogManager.getLogger(Meeting.class);
    

    /**
     * Constructs a new meeting with the given game and death player.
     *
     * @param game The game associated with the meeting.
     * @param death The player who is voted to be eliminated in the meeting.
     */
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
    /**
     * Casts a vote for the given player.
     *
     * @param vote The player to vote for.
     * @param voter The player who is casting the vote.
     */
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
