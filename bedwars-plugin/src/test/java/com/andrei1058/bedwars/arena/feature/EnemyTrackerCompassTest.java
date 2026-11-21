package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.api.messages.Messages;
import com.andrei1058.bedwars.api.server.VersionSupport;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnemyTrackerCompassTest {
    VersionSupport previous;
    IArena arena;
    World world;
    ITeam red, blue;
    Player viewer, enemy, teammate;
    EnemyTrackerCompass tracker;
    final Set<UUID> current = new HashSet<>();

    @BeforeEach void setup() throws Exception {
        previous = BedWars.nms;
        BedWars.nms = mock(VersionSupport.class);
        targets().clear();
        arena = mock(IArena.class); world = mock(World.class);
        red = mock(ITeam.class); blue = mock(ITeam.class);
        when(blue.getColor()).thenReturn(TeamColor.BLUE);
        when(blue.getDisplayName()).thenReturn("蓝队");
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.getWorld()).thenReturn(world);
        viewer = player("Alice", 0, red); enemy = player("Bob", 5, blue);
        teammate = player("Friend", 1, red);
        when(arena.getPlayers()).thenReturn(List.of(viewer, enemy, teammate));
        tracker = new EnemyTrackerCompass();
    }
    @AfterEach void restore() throws Exception { targets().clear(); BedWars.nms = previous; }
    @SuppressWarnings("unchecked") Map<UUID, Player> targets() throws Exception {
        var field = EnemyTrackerCompass.class.getDeclaredField("trackedTargets");
        field.setAccessible(true); return (Map<UUID, Player>) field.get(null);
    }
    Player player(String name, int x, ITeam team) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true); when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, x, 64, 0));
        when(arena.getTeam(player)).thenReturn(team);
        when(arena.isPlayer(player)).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack compass = new ItemStack(Material.COMPASS);
        when(inventory.getContents()).thenReturn(new ItemStack[]{compass});
        when(BedWars.nms.isCustomBedWarsItem(same(compass))).thenReturn(true);
        when(BedWars.nms.getCustomData(same(compass))).thenReturn("ENEMY_TRACKER_COMPASS");
        return player;
    }
    @Test void givingCompassMovesExistingHotbarItemToFreeSlot() {
        ItemStack displaced = new ItemStack(Material.STONE);
        PlayerInventory inventory = viewer.getInventory();
        when(inventory.getItem(8)).thenReturn(displaced);
        when(inventory.firstEmpty()).thenReturn(4);
        when(BedWars.nms.addCustomData(any(), eq("ENEMY_TRACKER_COMPASS")))
                .thenAnswer(call -> call.getArgument(0));
        EnemyTrackerCompass.giveTo(viewer);
        verify(inventory).setItem(0, null);
        verify(inventory).setItem(eq(4), same(displaced));
        var item = org.mockito.ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).setItem(eq(8), item.capture());
        assertEquals(Material.COMPASS, item.getValue().getType());
        verify(viewer).updateInventory();
    }
    @Test void givingCompassDropsDisplacedItemWhenInventoryIsFull() {
        ItemStack displaced = new ItemStack(Material.STONE);
        when(viewer.getInventory().getItem(8)).thenReturn(displaced);
        when(viewer.getInventory().firstEmpty()).thenReturn(-1);
        EnemyTrackerCompass.giveTo(viewer);
        verify(world).dropItemNaturally(eq(viewer.getLocation()), same(displaced));
    }
    @Test void soloPlayerGetsEnemyNameAndDistanceFromSameTargetAsNeedle() {
        tracker.updateArena(arena, current);
        assertSame(enemy, EnemyTrackerCompass.getTrackedTarget(viewer));
        verify(viewer).setCompassTarget(enemy.getLocation());
        try (var messages = mockStatic(ChineseMessages.class)) {
            messages.when(() -> ChineseMessages.getMsg(viewer, Messages.FORMATTING_ACTION_BAR_TRACKING))
                    .thenReturn("&f正在追踪：{player} {team} - 距离：{distance}m");
            EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
            verify(BedWars.nms).playAction(viewer, "§f正在追踪：§9Bob §9蓝队 - 距离：§95m");
        }
    }
    @Test void selectsNearestEnemyRatherThanCloserTeammate() {
        Player farther = player("Far", 20, blue);
        when(arena.getPlayers()).thenReturn(List.of(viewer, farther, enemy, teammate));
        tracker.updateArena(arena, current);
        assertSame(enemy, EnemyTrackerCompass.getTrackedTarget(viewer));
    }
    @Test void excludesPotionAndArmorHiddenEnemiesAndClearsPreviousTarget() {
        tracker.updateArena(arena, current);
        when(enemy.hasPotionEffect(PotionEffectType.INVISIBILITY)).thenReturn(true);
        tracker.updateArena(arena, current);
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
        when(enemy.hasPotionEffect(PotionEffectType.INVISIBILITY)).thenReturn(false);
        when(arena.getShowTime()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>(Map.of(enemy, 10)));
        tracker.updateArena(arena, current);
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
        EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
        verify(BedWars.nms, never()).playAction(any(), anyString());
    }
    @Test void excludesRespawnSpectatorDeadOfflineAndOtherWorld() {
        for (int state = 0; state < 5; state++) {
            when(arena.isReSpawning(enemy)).thenReturn(state == 0);
            when(arena.isSpectator(enemy)).thenReturn(state == 1);
            when(enemy.isDead()).thenReturn(state == 2);
            when(enemy.isOnline()).thenReturn(state != 3);
            when(enemy.getWorld()).thenReturn(state == 4 ? mock(World.class) : world);
            tracker.updateArena(arena, current);
            assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
        }
    }
    @Test void clearsTrackerCacheWhenCompassIsMissingOrMatchStopsOrArenaUnloads() {
        tracker.updateArenas(List.of(arena));
        assertSame(enemy, EnemyTrackerCompass.getTrackedTarget(viewer));
        ItemStack[] items = viewer.getInventory().getContents();
        when(viewer.getInventory().getContents()).thenReturn(new ItemStack[0]);
        tracker.updateArenas(List.of(arena));
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
        when(viewer.getInventory().getContents()).thenReturn(items);
        tracker.updateArenas(List.of(arena));
        when(arena.getStatus()).thenReturn(GameState.restarting);
        tracker.updateArenas(List.of(arena));
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
        when(arena.getStatus()).thenReturn(GameState.playing);
        tracker.updateArenas(List.of(arena)); tracker.updateArenas(List.of());
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
    }
    @Test void departedEnemyIsNotTrackedEvenIfStillInSameWorld() {
        tracker.updateArena(arena, current);
        when(arena.isPlayer(enemy)).thenReturn(false);
        EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
        verify(BedWars.nms, never()).playAction(any(), anyString());
        tracker.updateArena(arena, current);
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
    }
    @Test void noEnemyUsesIdleTargetAndDoesNotShowTrackingText() {
        when(arena.getPlayers()).thenReturn(List.of(viewer, teammate));
        tracker.updateArena(arena, current);
        assertNull(EnemyTrackerCompass.getTrackedTarget(viewer));
        verify(viewer).setCompassTarget(new Location(world, 16, 64, 0));
        EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
        verify(BedWars.nms, never()).playAction(any(), anyString());
    }
    @Test void actionBarRejectsStaleTargetAfterWorldChangeRespawnOrGameEnd() {
        tracker.updateArena(arena, current);
        when(enemy.getWorld()).thenReturn(mock(World.class));
        EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
        when(enemy.getWorld()).thenReturn(world); when(arena.isReSpawning(viewer)).thenReturn(true);
        EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
        when(arena.isReSpawning(viewer)).thenReturn(false); when(arena.getStatus()).thenReturn(GameState.restarting);
        EnemyTrackerCompass.showTrackingActionBar(arena, viewer);
        verify(BedWars.nms, never()).playAction(any(), anyString());
    }
}

