package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
