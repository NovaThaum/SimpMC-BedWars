package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MainConfigRestartMigrationTest {

    @Test
    void migratesHistoricalDefaultButPreservesCustomValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(ConfigPath.GENERAL_CONFIGURATION_RESTART, 45);

        MainConfig.migrateRestartCountdownDefault(config);

        assertEquals(30, config.getInt(ConfigPath.GENERAL_CONFIGURATION_RESTART));
        config.set(ConfigPath.GENERAL_CONFIGURATION_RESTART, 75);
        MainConfig.migrateRestartCountdownDefault(config);
        assertEquals(75, config.getInt(ConfigPath.GENERAL_CONFIGURATION_RESTART));
    }

    @Test
    void fixedFireballMigrationRemovesValuesAndDefaultsButKeepsOtherSettings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("fireball.speed-multiplier", 99);
        config.addDefault("fireball.cooldown", 20);
        config.set("lobbyServer", "login");
        MainConfig.migrateFixedFireball(config);
        assertNull(config.get("fireball"));
        assertEquals("login", config.getString("lobbyServer"));
        MainConfig.migrateFixedFireball(config);
        assertNull(config.get("fireball"));
    }
}
