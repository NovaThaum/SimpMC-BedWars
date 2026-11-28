package com.andrei1058.bedwars.arena.tasks;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameEndItems;
import com.andrei1058.bedwars.configuration.ArenaConfig;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.configuration.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRestartingTaskTest {
    @org.junit.jupiter.api.io.TempDir Path directory;

    @Test
    void givesReturnItemToEveryRemainingPlayerWhenSettlementStarts() {
        MainConfig previousConfig = BedWars.config;
        BedWars previousPlugin = BedWars.plugin;
        BedWars.plugin = mock(BedWars.class);
        when(BedWars.plugin.getDataFolder()).thenReturn(directory.toFile());
        when(BedWars.plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        BedWars.config = mock(MainConfig.class);
        Arena arena = mock(Arena.class);
        ArenaConfig arenaConfig = mock(ArenaConfig.class);
        when(arena.getConfig()).thenReturn(arenaConfig);
        Player winner = mock(Player.class);
        Player teammate = mock(Player.class);
        Player spectator = mock(Player.class);
        when(arena.getPlayers()).thenReturn(List.of(winner, teammate));
        when(arena.getSpectators()).thenReturn(List.of(spectator));
        try (var bukkit = mockStatic(Bukkit.class); var sounds = mockStatic(Sounds.class);
             var items = mockStatic(GameEndItems.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));

            new GameRestartingTask(arena);

            items.verify(() -> GameEndItems.give(winner));
            items.verify(() -> GameEndItems.give(teammate));
            items.verify(() -> GameEndItems.give(spectator), never());
        } finally {
            BedWars.config = previousConfig;
            BedWars.plugin = previousPlugin;
        }
    }

    @Test
    void announcesOnlyTheRequestedChatMilestones() {
        List<Integer> announcements = IntStream.iterate(65, value -> value >= 0, value -> value - 1)
                .filter(GameRestartingTask::shouldAnnounceRestartCountdown)
                .boxed()
                .toList();

        assertEquals(List.of(60, 30, 15, 10, 5, 4, 3, 2, 1, 0), announcements);
    }

    @Test
    void unloadsOnlyAfterEvacuationAndTeleportsComplete() {
        assertFalse(GameRestartingTask.canUnloadArenaWorld(false, 0, 0));
        assertFalse(GameRestartingTask.canUnloadArenaWorld(true, 1, 0));
        assertFalse(GameRestartingTask.canUnloadArenaWorld(true, 0, 1));
        assertTrue(GameRestartingTask.canUnloadArenaWorld(true, 0, 0));
    }
}
