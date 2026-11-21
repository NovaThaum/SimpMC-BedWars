package com.andrei1058.bedwars.stats;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameplayStatsTest {
    @Test void bedBreakRegularKillAndFinalKillUpdateSeparateCareerCounters() {
        StatsManager previous = BedWars.statsManager;
        BedWars.statsManager = mock(StatsManager.class);
        Player killer = mock(Player.class), victim = mock(Player.class);
        UUID attackerId = UUID.randomUUID(), victimId = UUID.randomUUID();
        when(killer.getUniqueId()).thenReturn(attackerId); when(victim.getUniqueId()).thenReturn(victimId);
        PlayerStats attack = new PlayerStats(attackerId), defense = new PlayerStats(victimId);
        when(BedWars.statsManager.get(attackerId)).thenReturn(attack);
        when(BedWars.statsManager.get(victimId)).thenReturn(defense);
        when(BedWars.statsManager.getUnsafe(attackerId)).thenReturn(attack);
        StatsListener listener = new StatsListener();
        try {
            PlayerBedBreakEvent bed = mock(PlayerBedBreakEvent.class); when(bed.getPlayer()).thenReturn(killer);
            listener.onBedBreak(bed);
            PlayerKillEvent kill = mock(PlayerKillEvent.class);
            when(kill.getVictim()).thenReturn(victim); when(kill.getKiller()).thenReturn(killer);
            when(kill.getCause()).thenReturn(PlayerKillEvent.PlayerKillCause.PVP);
            listener.onPlayerKill(kill);
            when(kill.getCause()).thenReturn(PlayerKillEvent.PlayerKillCause.PVP_FINAL_KILL);
            listener.onPlayerKill(kill);
            assertEquals(1, attack.getBedsDestroyed()); assertEquals(1, attack.getKills());
            assertEquals(1, attack.getFinalKills()); assertEquals(2, attack.getTotalKills());
            assertEquals(1, defense.getDeaths()); assertEquals(1, defense.getFinalDeaths());
            assertEquals(1, defense.getLosses()); assertEquals(1, defense.getGamesPlayed());
        } finally { BedWars.statsManager = previous; }
    }
}
