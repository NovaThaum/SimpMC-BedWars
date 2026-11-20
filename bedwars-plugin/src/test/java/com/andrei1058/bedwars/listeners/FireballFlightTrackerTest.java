package com.andrei1058.bedwars.listeners;

import org.bukkit.*;
import org.bukkit.entity.Fireball;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FireballFlightTrackerTest {
    @Test void removesFireballAfterAccumulatedPathLengthAndStopsItsTask() {
        Plugin plugin = mock(Plugin.class); Fireball fireball = mock(Fireball.class);
        World world = mock(World.class);
        when(fireball.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(fireball.isValid()).thenReturn(true);
        AtomicReference<Location> position = new AtomicReference<>(new Location(world, 3, 64, 0));
        when(fireball.getLocation(any(Location.class))).thenAnswer(call -> position.get());
        BukkitScheduler scheduler = mock(BukkitScheduler.class); BukkitTask task = mock(BukkitTask.class);
        when(task.getTaskId()).thenReturn(37);
        AtomicReference<Runnable> tick = new AtomicReference<>();
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(1L), eq(1L))).thenAnswer(call -> {
            tick.set(call.getArgument(1)); return task;
        });
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            FireballFlightTracker.start(plugin, fireball, 6D);
            tick.get().run(); verify(fireball, never()).remove();
            position.set(new Location(world, 0, 64, 0)); tick.get().run();
            verify(fireball).remove(); verify(scheduler).cancelTask(37);
        }
    }
}
