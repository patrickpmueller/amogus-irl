package com.pellacanimuller.amogus_irl.game;

import com.pellacanimuller.amogus_irl.game.players.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

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
     * Timer for managing the duration of the meeting.
     */
    private final Timer timer;

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

        // Start the timer
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endMeeting();
            }
        }, game.MEETING_DURATION * 1000L);
    }

    /**
     * Casts a vote for the given player.
     *
     * @param vote The player to vote for.
     * @param voter The player who is casting the vote.
     */
    public void vote(Player vote, Player voter) {
        if (!game.alive.contains(voter)) {
            log.debug("Cannot vote, player not alive: {}", voter.id);
            return;
        }

        if (!voters.add(voter)) {
            log.debug("Already voted: {}", voter.id);
            return;
        }

        if (vote == null) {
            log.debug("Vote: Skipped");
            votes.replace(null, votes.get(null) + 1);
            return;
        }

        if (!game.alive.contains(vote)) {
            log.debug("Invalid vote: {}", vote.id);
            return;
        }

        log.debug("Vote: {}", vote.id);
        votes.replace(vote, votes.get(vote) + 1);

        if (votes.size() == game.alive.size()) {
            endMeeting();
        }

    }

    /**
     * Ends the meeting and determines the outcome based on the number of votes each player received.
     */
    private void endMeeting() {
        log.debug("Meeting ended");
        timer.cancel();
        Player winner = votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(maxEntry ->
                        votes.values().stream()
                                .filter(value -> value.equals(maxEntry.getValue()))
                                .count() == 1
                )
                .map(Map.Entry::getKey)
                .orElse(null);

        game.endMeeting(winner);
    }
}
