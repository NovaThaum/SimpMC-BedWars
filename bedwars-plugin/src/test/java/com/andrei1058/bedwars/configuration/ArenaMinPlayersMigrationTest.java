package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArenaMinPlayersMigrationTest {
    @Test
    void migratesLegacyMinimumOnlyWhenNewKeyWasAbsent() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("minInTeam", 6);

        ArenaConfig.migrateMinimumPlayers(config, false, 6);

        assertEquals(6, config.getInt("minPlayers"));
        assertFalse(config.contains("minInTeam"));
    }

    @Test
    void preservesExistingMinimumAndClampsItToTwo() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("minPlayers", 7);
        config.set("minInTeam", 4);

        ArenaConfig.migrateMinimumPlayers(config, true, 4);

        assertEquals(7, config.getInt("minPlayers"));
        assertFalse(config.contains("minInTeam"));
        config.set("minPlayers", 1);
        ArenaConfig.migrateMinimumPlayers(config, true, null);
        assertEquals(2, config.getInt("minPlayers"));
    }
}
