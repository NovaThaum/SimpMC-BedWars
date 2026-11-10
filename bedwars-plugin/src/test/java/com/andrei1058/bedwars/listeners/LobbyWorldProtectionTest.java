package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;
import com.andrei1058.bedwars.arena.GameRules;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Difficulty;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.mockito.MockedStatic;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LobbyWorldProtectionTest {
    @Test
    public void onlyConfiguredLobbyAndArenaWorldsAreManaged() {
        assertTrue(BedWarsWorldEnvironment.isManagedWorldName("Lobby", "lobby", false));
        assertTrue(BedWarsWorldEnvironment.isManagedWorldName("survival", "lobby", true));
        assertFalse(BedWarsWorldEnvironment.isManagedWorldName("survival", "lobby", false));
        assertFalse(BedWarsWorldEnvironment.isManagedWorldName("world", "", false));
    }

    @Test
    public void lobbyNameComparisonIsCaseInsensitiveButEmptyNamesAreNotLobby() {
        assertTrue(BedWarsWorldEnvironment.isLobbyWorldName("LOBBY", "lobby"));
        assertFalse(BedWarsWorldEnvironment.isLobbyWorldName("lobby", ""));
        assertFalse(BedWarsWorldEnvironment.isLobbyWorldName(null, "lobby"));
    }

    @Test
    public void recognizesPlayerAndConsoleDifficultyCommands() {
        assertTrue(LobbyWorldProtection.isDifficultyCommand("/difficulty hard"));
        assertTrue(LobbyWorldProtection.isDifficultyCommand("minecraft:difficulty peaceful"));
        assertFalse(LobbyWorldProtection.isDifficultyCommand("/weather clear"));
    }

    @Test
    public void explosionsAreCancelledOnlyInLobby() {
        World lobby = mock(World.class);
        World survival = mock(World.class);
        Block block = mock(Block.class);
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(block);
        EntityExplodeEvent event = new EntityExplodeEvent(null, new Location(lobby, 0, 0, 0), blocks, 1F);
        EntityExplodeEvent other = new EntityExplodeEvent(null, new Location(survival, 0, 0, 0), new ArrayList<>(blocks), 1F);
        try (MockedStatic<BedWarsWorldEnvironment> environment = mockStatic(BedWarsWorldEnvironment.class)) {
            environment.when(() -> BedWarsWorldEnvironment.isLobbyWorld(lobby)).thenReturn(true);
            LobbyWorldProtection protection = new LobbyWorldProtection();
            protection.onEntityExplode(event);
            protection.onEntityExplode(other);
            assertTrue(event.isCancelled());
            assertTrue(event.blockList().isEmpty());
            assertFalse(other.isCancelled());
            assertEquals(1, other.blockList().size());
        }
    }

    @Test
    public void lobbyDamageAndHungerAreBlocked() {
        World lobby = mock(World.class);
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(lobby);
        EntityDamageEvent damage = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.FIRE, 4);
        FoodLevelChangeEvent food = new FoodLevelChangeEvent(player, 7);
        try (MockedStatic<BedWarsWorldEnvironment> environment = mockStatic(BedWarsWorldEnvironment.class)) {
            environment.when(() -> BedWarsWorldEnvironment.isLobbyWorld(lobby)).thenReturn(true);
            LobbyWorldProtection protection = new LobbyWorldProtection();
            protection.onDamage(damage);
            protection.onFoodChange(food);
            assertTrue(damage.isCancelled());
            assertTrue(food.isCancelled());
            verify(player).setFoodLevel(20);
            verify(player).setSaturation(20F);
        }
    }

    @Test
    public void buildSessionsCanEditButOtherPlayersCannot() {
        World lobby = mock(World.class);
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(lobby);
        Player builder = mock(Player.class);
        Player visitor = mock(Player.class);
        BreakPlace.addBuildSession(builder);
        try (MockedStatic<BedWarsWorldEnvironment> environment = mockStatic(BedWarsWorldEnvironment.class)) {
            environment.when(() -> BedWarsWorldEnvironment.isLobbyWorld(lobby)).thenReturn(true);
            BlockBreakEvent allowed = new BlockBreakEvent(block, builder);
            BlockBreakEvent denied = new BlockBreakEvent(block, visitor);
            LobbyWorldProtection protection = new LobbyWorldProtection();
            protection.onBreak(allowed);
            protection.onBreak(denied);
            assertFalse(allowed.isCancelled());
            assertTrue(denied.isCancelled());
        } finally {
            BreakPlace.removeBuildSession(builder);
        }
    }

    @Test
    public void pluginSpawnedNpcsArePreserved() {
        World lobby = mock(World.class);
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getLocation()).thenReturn(new Location(lobby, 0, 0, 0));
        CreatureSpawnEvent natural = new CreatureSpawnEvent(entity, CreatureSpawnEvent.SpawnReason.NATURAL);
        CreatureSpawnEvent custom = new CreatureSpawnEvent(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
        try (MockedStatic<BedWarsWorldEnvironment> environment = mockStatic(BedWarsWorldEnvironment.class)) {
            environment.when(() -> BedWarsWorldEnvironment.isLobbyWorld(lobby)).thenReturn(true);
            LobbyWorldProtection protection = new LobbyWorldProtection();
            protection.onCreatureSpawn(natural);
            protection.onCreatureSpawn(custom);
            assertTrue(natural.isCancelled());
            assertFalse(custom.isCancelled());
        }
    }

    @Test
    public void rulesSkipUnsupportedKeysAndDoNotDisableArenaPvp() {
        World arena = mock(World.class);
        when(arena.isGameRule(anyString())).thenReturn(true);
        when(arena.isGameRule("doWeatherCycle")).thenReturn(false);
        GameRules.enforceArenaEnvironment(arena);
        verify(arena, never()).setGameRuleValue(eq("doWeatherCycle"), anyString());
        verify(arena).setGameRuleValue("randomTickSpeed", "0");
        verify(arena).setTime(6000L);
        verify(arena, never()).setPVP(anyBoolean());
        verify(arena, never()).setDifficulty(any());
        World lobby = mock(World.class);
        GameRules.enforceLobbyEnvironment(lobby);
        verify(lobby).setDifficulty(Difficulty.PEACEFUL);
        verify(lobby).setPVP(false);
        GameRules.enforceLobbyEnvironment(null);
    }

    @Test
    public void worldScopeExcludesQueuedArenasSetupAndSharedSurvival() {
        com.andrei1058.bedwars.configuration.MainConfig original = com.andrei1058.bedwars.BedWars.config;
        com.andrei1058.bedwars.BedWars.config = mock(com.andrei1058.bedwars.configuration.MainConfig.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        com.andrei1058.bedwars.api.arena.IArena queued = mock(com.andrei1058.bedwars.api.arena.IArena.class);
        when(queued.getWorldName()).thenReturn("world");
        com.andrei1058.bedwars.arena.SetupSession setup = mock(com.andrei1058.bedwars.arena.SetupSession.class);
        when(setup.getWorldName()).thenReturn("world");
        try (MockedStatic<com.andrei1058.bedwars.BedWars> bedwars = mockStatic(com.andrei1058.bedwars.BedWars.class);
             MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bedwars.when(com.andrei1058.bedwars.BedWars::getLobbyWorld).thenReturn("");
            bedwars.when(com.andrei1058.bedwars.BedWars::getServerType).thenReturn(com.andrei1058.bedwars.api.server.ServerType.SHARED);
            bukkit.when(org.bukkit.Bukkit::getWorlds).thenReturn(java.util.Collections.singletonList(world));
            assertFalse(BedWarsWorldEnvironment.isLobbyWorld(world));
            assertFalse(BedWarsWorldEnvironment.isManagedWorld(world));
            bedwars.when(com.andrei1058.bedwars.BedWars::getServerType).thenReturn(com.andrei1058.bedwars.api.server.ServerType.MULTIARENA);
            assertTrue(BedWarsWorldEnvironment.isLobbyWorld(world));
            com.andrei1058.bedwars.arena.Arena.getEnableQueue().add(queued);
            try {
                assertFalse(BedWarsWorldEnvironment.isLobbyWorld(world));
                assertTrue(BedWarsWorldEnvironment.isManagedWorld(world));
            } finally {
                com.andrei1058.bedwars.arena.Arena.getEnableQueue().remove(queued);
            }
            com.andrei1058.bedwars.arena.SetupSession.getSetupSessions().add(setup);
            try {
                assertFalse(BedWarsWorldEnvironment.isLobbyWorld(world));
                assertFalse(BedWarsWorldEnvironment.isManagedWorld(world));
            } finally {
                com.andrei1058.bedwars.arena.SetupSession.getSetupSessions().remove(setup);
            }
        } finally {
            com.andrei1058.bedwars.BedWars.config = original;
        }
    }

    @Test
    public void externalTeleportCannotBypassArenaRegistration() {
        com.andrei1058.bedwars.configuration.MainConfig original = com.andrei1058.bedwars.BedWars.config;
        com.andrei1058.bedwars.BedWars.config = mock(com.andrei1058.bedwars.configuration.MainConfig.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("arena");
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        com.andrei1058.bedwars.api.arena.IArena destination = mock(com.andrei1058.bedwars.api.arena.IArena.class);
        try (MockedStatic<com.andrei1058.bedwars.arena.Arena> arenas = mockStatic(com.andrei1058.bedwars.arena.Arena.class)) {
            arenas.when(() -> com.andrei1058.bedwars.arena.Arena.getArenaByIdentifier("arena")).thenReturn(destination);
            ArenaWorldProtection protection = new ArenaWorldProtection();
            org.bukkit.event.player.PlayerTeleportEvent denied = new org.bukkit.event.player.PlayerTeleportEvent(player, new Location(world, 0, 0, 0), new Location(world, 1, 1, 1));
            protection.onTeleport(denied);
            assertTrue(denied.isCancelled());
            arenas.when(() -> com.andrei1058.bedwars.arena.Arena.getArenaByPlayer(player)).thenReturn(destination);
            org.bukkit.event.player.PlayerTeleportEvent allowed = new org.bukkit.event.player.PlayerTeleportEvent(player, new Location(world, 0, 0, 0), new Location(world, 1, 1, 1));
            protection.onTeleport(allowed);
            assertFalse(allowed.isCancelled());
        } finally {
            com.andrei1058.bedwars.BedWars.config = original;
        }
    }
}
