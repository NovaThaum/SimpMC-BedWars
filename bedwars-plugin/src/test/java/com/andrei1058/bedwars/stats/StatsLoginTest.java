package com.andrei1058.bedwars.stats;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.database.Database;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatsLoginTest {
    @Test void failedPreloadRejectsLoginAndNeverPublishesEmptyStats() throws Exception {
        StatsManager previous = BedWars.statsManager;
        BedWars plugin = BedWars.plugin;
        BedWars.statsManager = mock(StatsManager.class);
        BedWars.plugin = mock(BedWars.class);
        when(BedWars.plugin.getLogger()).thenReturn(mock(Logger.class));
        var field = BedWars.class.getDeclaredField("remoteDatabase"); field.setAccessible(true);
        Object oldDatabase = field.get(null);
        Database database = mock(Database.class); field.set(null, database);
        UUID uuid = UUID.randomUUID();
        when(database.fetchStats(uuid)).thenThrow(new IllegalStateException("test database failure"));
        try {
            var event = new AsyncPlayerPreLoginEvent("Test", InetAddress.getLoopbackAddress(), uuid);
            new StatsListener().onAsyncPreLoginEvent(event);
            assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, event.getLoginResult());
            verify(BedWars.statsManager, never()).put(any(), any());
            verify(BedWars.statsManager).remove(uuid);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "banned");
            clearInvocations(database);
            new StatsListener().onAsyncPreLoginEvent(event);
            verifyNoInteractions(database);
        } finally {
            field.set(null, oldDatabase); BedWars.statsManager = previous; BedWars.plugin = plugin;
        }
    }

    @Test void finalUuidMustBeLoadedAndExistingDenialIsPreserved() {
        StatsManager previous = BedWars.statsManager;
        BedWars plugin = BedWars.plugin;
        BedWars.statsManager = mock(StatsManager.class); BedWars.plugin = mock(BedWars.class);
        when(BedWars.plugin.getLogger()).thenReturn(mock(Logger.class));
        Player player = mock(Player.class); UUID uuid = UUID.randomUUID(); when(player.getUniqueId()).thenReturn(uuid);
        try {
            var event = new PlayerLoginEvent(player);
            new StatsListener().onPlayerLoginEvent(event);
            assertEquals(PlayerLoginEvent.Result.KICK_OTHER, event.getResult());
            when(BedWars.statsManager.getUnsafe(uuid)).thenReturn(new PlayerStats(uuid));
            event.allow(); new StatsListener().onPlayerLoginEvent(event);
            assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "banned");
            new StatsListener().onPlayerLoginEvent(event);
            assertEquals(PlayerLoginEvent.Result.KICK_BANNED, event.getResult());
            verify(BedWars.statsManager).remove(uuid);
        } finally { BedWars.statsManager = previous; BedWars.plugin = plugin; }
    }
}
