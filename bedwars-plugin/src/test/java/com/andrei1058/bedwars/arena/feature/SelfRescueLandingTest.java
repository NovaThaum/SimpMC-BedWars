package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SelfRescueLandingTest {
    @Test void overlapPolicyDetectsPlayerBodyIntersectingGeneratedBlock() {
        assertTrue(SelfRescuePlatform.overlapsPlayerBody(.5, 5.5, .5, 0, 5, 0));
    }

    @Test void overlapPolicyAllowsTouchingButNotIntersectingBlockFaces() {
        assertFalse(SelfRescuePlatform.overlapsPlayerBody(.5, 6.0, .5, 0, 5, 0));
        assertFalse(SelfRescuePlatform.overlapsPlayerBody(.5, 3.2, .5, 0, 5, 0));
        assertFalse(SelfRescuePlatform.overlapsPlayerBody(-.3, 5.5, .5, 0, 5, 0));
    }

    MainConfig original; MockedStatic<Arena> arenas; MockedStatic<Bukkit> bukkit;
    IArena arena; Player player; World world; SelfRescuePlatform platform;
    AtomicLong clock; Map<String, Block> blocks;
    @BeforeEach void setup() throws Exception {
        original = BedWars.config; BedWars.config = mock(MainConfig.class);
        arenas = mockStatic(Arena.class); bukkit = mockStatic(Bukkit.class);
        arena = mock(IArena.class); player = mock(Player.class); world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID()); when(world.getName()).thenReturn("arena");
        when(world.getMaxHeight()).thenReturn(256); when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world); when(player.getLocation()).thenReturn(new Location(world, .5, 8, .5));
        when(arena.getStatus()).thenReturn(GameState.playing); when(arena.isPlayer(player)).thenReturn(true);
        when(arena.getWorld()).thenReturn(world); arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mock(BukkitTask.class));
        blocks = new HashMap<>();
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call -> {
            int x = call.getArgument(0), y = call.getArgument(1), z = call.getArgument(2);
            return blocks.computeIfAbsent(x+":"+y+":"+z, key -> {
                Block block = mock(Block.class); when(block.getWorld()).thenReturn(world);
                when(block.getX()).thenReturn(x); when(block.getY()).thenReturn(y); when(block.getZ()).thenReturn(z);
                when(block.getType()).thenReturn(Material.AIR);
                doAnswer(set -> { when(block.getType()).thenReturn(set.getArgument(0)); return null; })
                        .when(block).setType(any(Material.class), anyBoolean());
                return block;
            });
        });
        clock = new AtomicLong(1); platform = new SelfRescuePlatform(clock::get);
        var deploy = SelfRescuePlatform.class.getDeclaredMethod("deploy", IArena.class, Player.class, Location.class, int.class, boolean.class);
        deploy.setAccessible(true); deploy.invoke(platform, arena, player, player.getLocation(), 0, true);
    }
    @AfterEach void cleanup() { arenas.close(); bukkit.close(); BedWars.config = original; }
    EntityDamageEvent fall(double x, double y, double z) {
        when(player.getLocation()).thenReturn(new Location(world,x,y,z));
        EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.FALL, 8);
        platform.protectLanding(event); return event;
    }
    @Test void deploymentResetsFallAndProtectsOnlyFirstRescueLandingAboveSurface() {
        verify(player).setFallDistance(0);
        assertTrue(fall(.5, 3, .5).isCancelled());
        assertFalse(fall(.5, 3, .5).isCancelled());
    }
    @Test void expiresAndDoesNotProtectBelowPlatformOrAboveDeployment() {
        clock.addAndGet(5_000_000_000L);
        assertFalse(fall(.5, 3, .5).isCancelled());
        assertFalse(fall(.5, -1, .5).isCancelled());
        assertFalse(fall(.5, 10, .5).isCancelled());
    }
    @Test void actualPlatformContactIncludesFootEdgeButNotHolesOrNearbyFalls() {
        clock.addAndGet(6_000_000_000L);
        assertTrue(fall(1.1, 1, .5).isCancelled());
        assertFalse(fall(-1.5, 1, -1.5).isCancelled());
        assertFalse(fall(.5, 1.5, .5).isCancelled());
        assertTrue(fall(2.2, 1, .5).isCancelled());
        assertFalse(fall(2.31, 1, .5).isCancelled());
    }
    @Test void groundedMoveConsumesLandingStateWithoutExtendingAreaImmunity() {
        when(player.isOnGround()).thenReturn(true);
        Location position = new Location(world,.5,3,.5);
        platform.onMove(new PlayerMoveEvent(player, position, position.clone()));
        assertFalse(fall(.5,3,.5).isCancelled());
    }
    @Test void spectatorAndRemovedPlatformCannotProtect() throws Exception {
        when(arena.isSpectator(player)).thenReturn(true);
        assertFalse(fall(.5,1,.5).isCancelled());
        when(arena.isSpectator(player)).thenReturn(false);
        var remove = SelfRescuePlatform.class.getDeclaredMethod("removePlatform", long.class);
        remove.setAccessible(true); remove.invoke(platform,1L);
        assertFalse(fall(.5,1,.5).isCancelled());
    }
}
