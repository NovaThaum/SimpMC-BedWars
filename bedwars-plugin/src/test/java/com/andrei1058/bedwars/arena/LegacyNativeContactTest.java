package com.andrei1058.bedwars.arena;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.VersionSupport;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;
class LegacyNativeContactTest {
    @Test void enablesNativePickupForActivePlayersAndDisablesItForRespawningAndSpectators() throws Exception {
        VersionSupport previous = BedWars.nms; BedWars.nms = mock(VersionSupport.class);
        try {
            Arena arena = mock(Arena.class, CALLS_REAL_METHODS);
            Player active = mock(Player.class), respawning = mock(Player.class), spectator = mock(Player.class);
            var players = Arena.class.getDeclaredField("players"); players.setAccessible(true);
            players.set(arena, new ArrayList<>(List.of(active, respawning, spectator)));
            var sessions = Arena.class.getDeclaredField("respawnSessions"); sessions.setAccessible(true);
            ConcurrentHashMap<Player,Integer> waiting = new ConcurrentHashMap<>(); waiting.put(respawning, 3); sessions.set(arena, waiting);
            doReturn(true).when(arena).isPlayer(any(Player.class)); doReturn(false).when(arena).isSpectator(any(Player.class));
            doReturn(true).when(arena).isSpectator(spectator);
            var update = Arena.class.getDeclaredMethod("updatePlayingCollision", boolean.class); update.setAccessible(true);
            update.invoke(arena, true);
            verify(BedWars.nms).setCollide(active, arena, true); verify(BedWars.nms).setCollide(respawning, arena, false); verify(BedWars.nms).setCollide(spectator, arena, false);
            reset(BedWars.nms); waiting.remove(respawning); update.invoke(arena, true);
            verify(BedWars.nms).setCollide(respawning, arena, true);
            reset(BedWars.nms); update.invoke(arena, false);
            verify(BedWars.nms).setCollide(active, arena, false); verify(BedWars.nms).setCollide(respawning, arena, false);
        } finally { BedWars.nms = previous; }
    }
}
