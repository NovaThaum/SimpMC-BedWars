package com.andrei1058.bedwars.support.papi;

import com.andrei1058.bedwars.stats.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PAPISupportTest {
    @Test
    void leaderboardKdRequiresMoreThanOneHundredGames() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID());
        stats.setGamesPlayed(100);
        stats.setKills(20);
        assertEquals("N/A", PAPISupport.qualifiedKdLeaderboardValue(stats));

        stats.setGamesPlayed(101);
        assertEquals("20.0", PAPISupport.qualifiedKdLeaderboardValue(stats));
    }

    @Test
    void leaderboardKdCountsFinalDeathsButNotFinalKills() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID());
        stats.setGamesPlayed(101);
        stats.setKills(12);
        stats.setFinalKills(500);
        stats.setDeaths(3);
        stats.setFinalDeaths(1);

        assertEquals("3.0", PAPISupport.qualifiedKdLeaderboardValue(stats));
    }

    @Test
    void zeroDeathsLeavesRegularKillsAsKd() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID());
        stats.setGamesPlayed(101);
        stats.setKills(7);
        stats.setFinalKills(100);

        assertEquals("7.0", PAPISupport.qualifiedKdLeaderboardValue(stats));
        assertEquals("101", PAPISupport.gamesPlayedLeaderboardValue(stats));
    }
}
