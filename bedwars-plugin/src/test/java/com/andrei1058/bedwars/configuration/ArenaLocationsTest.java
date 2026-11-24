package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArenaLocationsTest {
    @TempDir Path temp;

    @Test void loadsModernGeneratorListsAndWritesOnlyCoordinates() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        World world = mock(World.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("acropolis")).thenReturn(world);
            ConfigManager config = new ConfigManager(plugin, "acropolis", temp.toString());
            config.getYml().set("Team.Red.Iron", List.of("10.5,64,20.5", "[-2.25, 70, 9, 180, 30]"));
            List<Location> generators = config.getArenaLocations("Team.Red.Iron");
            assertEquals(2, generators.size());
            assertSame(world, generators.get(0).getWorld());
            assertEquals(-2.5, generators.get(1).getX());
            assertEquals(0, generators.get(1).getYaw());
            assertEquals(0, generators.get(1).getPitch());
            config.saveArenaLoc("Team.Red.Spawn", new Location(world, 1.25, 64.75, -3.5, 90, 20));
            config.reload();
            assertEquals("1.5,64.0,-3.5", config.getString("Team.Red.Spawn"));
            assertEquals(64.0, config.getArenaLoc("Team.Red.Spawn").getY());
            assertEquals("1.5,64.0,-3.5", config.stringLocationArenaFormat(new Location(world, 1.25, 64.75, -3.5, 180, 90)));
            when(world.getName()).thenReturn("lobby");
            config.saveConfigLoc("lobbyLoc", new Location(world, 1, 2, 3, 90, 20));
            assertEquals(90, config.getConfigLoc("lobbyLoc").getYaw());
            assertEquals(20, config.getConfigLoc("lobbyLoc").getPitch());
        }
    }

    @Test void migratesAllKnownMarkersWithoutChangingCustomValuesAndIsIdempotent() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : List.of("Team.Red.Spawn", "Team.Red.Shop", "Team.Red.Upgrade", "Team.Red.Bed", "Team.Red.kill-drops-loc", "waiting.Loc", "waiting.Pos1", "spectator-loc"))
            yaml.set(key, "-1.25,64.5,2.75,180,30");
        for (String key : List.of("Team.Red.Iron", "Team.Red.Gold", "Team.Red.Emerald", "generator.Diamond", "generator.Emerald"))
            yaml.set(key, List.of("1,2,3,90,10", "4,5,6"));
        yaml.set("custom", "1,2,3,90,10");
        ArenaConfig.migrateArenaLocations(yaml);
        assertEquals("-1.5,64.0,2.5", yaml.getString("Team.Red.Spawn"));
        assertEquals("-1.5,64.0,2.5", yaml.getString("waiting.Pos1"));
        assertEquals(List.of("1.5,2.0,3.5", "4.5,5.0,6.5"), yaml.getStringList("Team.Red.Iron"));
        assertEquals(List.of("1.5,2.0,3.5", "4.5,5.0,6.5"), yaml.getStringList("generator.Diamond"));
        assertEquals("1,2,3,90,10", yaml.getString("custom"));
        String once = yaml.saveToString();
        ArenaConfig.migrateArenaLocations(yaml);
        assertEquals(once, yaml.saveToString());
    }

    @Test void rejectsIncompleteOrNonFiniteCoordinatesWithExplicitMessage() {
        for (String bad : List.of("1,2", "1,2,3,4", "NaN,2,3", "Infinity,2,3", "1,,3"))
            assertThrows(IllegalArgumentException.class, () -> ConfigManager.normalizeArenaLocationString(bad));
    }
    @Test void playerAndNpcFacingAreSavedSeparatelyFromCenteredCoordinates() {
        Plugin plugin = mock(Plugin.class); when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        World world = mock(World.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("arena")).thenReturn(world);
            ConfigManager config = new ConfigManager(plugin, "arena", temp.toString());
            config.saveNpcArenaLocation("Team.Red.Shop", "Team.Red.Shop-Facing",
                    new Location(world, 1.25, 64.5, 2.75, -135, 12));
            config.saveNpcArenaLocation("Team.Red.Upgrade", "Team.Red.Upgrade-Facing",
                    new Location(world, 1.25, 64.5, 2.75, 46, 12));
            config.savePlayerArenaLocation("Team.Red.Spawn", "Team.Red.Spawn-Facing",
                    new Location(world, 1.25, 64.5, 2.75, 46, 12));
            assertEquals("1.5,64.0,2.5", config.getString("Team.Red.Shop"));
            assertEquals(-90.0, config.getDouble("Team.Red.Shop-Facing"));
            assertEquals(90.0, config.getDouble("Team.Red.Upgrade-Facing"));
            assertEquals("90.0,0.0", config.getString("Team.Red.Spawn-Facing"));
            ArenaConfig.migrateArenaLocations(config.getYml());
            assertEquals("1.5,64.0,2.5", config.getString("Team.Red.Upgrade"));
        }
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.normalizeArenaLocationString("NaN,2,3,0,0"));
    }

    @Test void migratesLegacyEmbeddedFacingAndUsesTeamSpawnForUnconfiguredNpcs() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Team.Red.Spawn", "10,64,0,45,30");
        yaml.set("Team.Red.Shop", "0,64,0,46,18");
        yaml.set("Team.Red.Upgrade", "0,64,0");
        yaml.set("waiting.Loc", "1,70,2,181,20");
        yaml.set("spectator-loc", "2,70,3,-44,40");

        ArenaConfig.migrateArenaLocations(yaml);

        assertEquals("90.0,0.0", yaml.getString("Team.Red.Spawn-Facing"));
        assertEquals(90.0, yaml.getDouble("Team.Red.Shop-Facing"));
        assertEquals(-90.0, yaml.getDouble("Team.Red.Upgrade-Facing"));
        assertEquals("-180.0,0.0", yaml.getString("waiting.Facing"));
        assertEquals("0.0,0.0", yaml.getString("spectator-facing"));
        assertEquals("0.5,64.0,0.5", yaml.getString("Team.Red.Shop"));

        String once = yaml.saveToString();
        ArenaConfig.migrateArenaLocations(yaml);
        assertEquals(once, yaml.saveToString());
    }

    @Test void explicitModernFacingOverridesEmbeddedFacingAndDefaultsPlayersToZero() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Team.Red.Spawn", "10,64,0,180,30");
        yaml.set("Team.Red.Spawn-Facing", "-100,30");
        yaml.set("Team.Red.Shop", "0,64,0,180,30");
        yaml.set("Team.Red.Shop-Facing", 44);
        yaml.set("waiting.Loc", "1,70,2");
        ArenaConfig.migrateArenaLocations(yaml);
        assertEquals("-90.0,0.0", yaml.getString("Team.Red.Spawn-Facing"));
        assertEquals(0, yaml.getDouble("Team.Red.Shop-Facing"));
        assertEquals("0.0,0.0", yaml.getString("waiting.Facing"));
        String once = yaml.saveToString();
        ArenaConfig.migrateArenaLocations(yaml);
        assertEquals(once, yaml.saveToString());
    }
}
