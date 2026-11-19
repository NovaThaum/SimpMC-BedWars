package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.stats.PlayerStats;
import com.andrei1058.bedwars.stats.StatsManager;
import com.andrei1058.spigot.sidebar.PlaceholderProvider;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LobbyStatsPlaceholdersTest {
    @Test void providersSurviveMissingReplacedAndRemovedCacheWithoutCreatingFakeStats() {
        StatsManager previous = BedWars.statsManager;
        StatsManager manager = mock(StatsManager.class);
        BedWars.statsManager = manager;
        UUID uuid = UUID.randomUUID();
        AtomicReference<PlayerStats> current = new AtomicReference<>();
        when(manager.getUnsafe(uuid)).thenAnswer(call -> current.get());
        try {
            var providers = new ConcurrentLinkedQueue<PlaceholderProvider>();
            assertDoesNotThrow(() -> BwSidebar.addLobbyStatsPlaceholders(providers, uuid));
            assertEquals(8, providers.size());
            providers.forEach(p -> assertEquals("加载中", p.getValue()));
            PlayerStats loaded = new PlayerStats(uuid);
            loaded.setKills(15); loaded.setWins(3);
            current.set(loaded);
            assertEquals("15", providers.peek().getValue());
            loaded.setKills(16);
            assertEquals("16", providers.peek().getValue());
            PlayerStats replacement = new PlayerStats(uuid); replacement.setKills(42);
            current.set(replacement);
            assertEquals("42", providers.peek().getValue());
            current.set(null);
            providers.forEach(p -> assertEquals("加载中", p.getValue()));
            verify(manager, never()).get(any());
            verify(manager, never()).put(any(), any());
        } finally { BedWars.statsManager = previous; }
    }
}
