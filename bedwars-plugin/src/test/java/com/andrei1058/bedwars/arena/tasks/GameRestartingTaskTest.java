package com.andrei1058.bedwars.arena.tasks;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.api.messages.Messages;
import com.andrei1058.bedwars.api.arena.shop.ShopHolo;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameEndItems;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.configuration.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class GameRestartingTaskTest {
    @Test void countsConfiguredSecondsAndWaitsForParticipantsAndSpectatorsToLeave() {
        MainConfig previous = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        when(BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_RESTART)).thenReturn(5);
        Arena arena = mock(Arena.class, RETURNS_DEEP_STUBS);
        Player winner = mock(Player.class), spectator = mock(Player.class);
        when(arena.getPlayers()).thenReturn(List.of(winner));
        when(arena.getSpectators()).thenReturn(List.of(spectator));
        when(arena.getWorld().getPlayers()).thenReturn(List.of(winner, spectator));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask scheduled = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(any(), any(Runnable.class), eq(0L), eq(20L))).thenReturn(scheduled);
        try (var bukkit = mockStatic(Bukkit.class); var sounds = mockStatic(Sounds.class);
             var items = mockStatic(GameEndItems.class); var messages = mockStatic(ChineseMessages.class);
             var holos = mockStatic(ShopHolo.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            for (Player player : List.of(winner, spectator)) {
                messages.when(() -> ChineseMessages.getMsg(player, Messages.ARENA_RESTART_COUNTDOWN))
                        .thenReturn("重置 {time}");
            }
            GameRestartingTask task = new GameRestartingTask(arena);
            assertEquals(5, task.getRestarting());
            for (int i = 0; i < 5; i++) task.run();
            verify(winner).sendMessage("重置 5");
            verify(spectator).sendMessage("重置 1");
            verify(arena, never()).removePlayer(any(), anyBoolean());
            task.run();
            verify(arena).removePlayer(eq(winner), anyBoolean());
            verify(arena).removeSpectator(eq(spectator), anyBoolean());
            verify(arena, never()).restart();
            task.run();
            verify(arena, never()).restart();
            when(arena.getWorld().getPlayers()).thenReturn(List.of());
            task.run();
            verify(arena).restart();
            verify(scheduled).cancel();
        } finally { BedWars.config = previous; }
    }
}
