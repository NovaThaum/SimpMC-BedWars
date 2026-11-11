package com.andrei1058.bedwars.configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ViolationConfigMigrationTest {
    @Test void persistsDefaultsWithinStatisticsAndKeepsOverridesAndUnrelatedTargets() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("match-statistics.enabled",false); yaml.set("match-statistics.violations.match-leave-threshold",72);
        yaml.set("match-statistics.violations.cross-team-item-transfer",false);
        yaml.set("match-statistics.queue-capacity",456); yaml.set("match-statistics.extension.custom","keep");
        yaml.set("lobbyServer","login");
        ViolationConfigMigration.migrate(yaml);
        String saved=yaml.saveToString();
        YamlConfiguration loaded=new YamlConfiguration(); loaded.loadFromString(saved);
        assertFalse(loaded.getBoolean("match-statistics.enabled"));
        assertTrue(loaded.getBoolean("match-statistics.violations.enabled"));
        assertEquals(72,loaded.getInt("match-statistics.violations.match-leave-threshold"));
        assertFalse(loaded.getBoolean("match-statistics.violations.cross-team-item-transfer"));
        assertEquals(List.of(10,20,50,100),loaded.getIntegerList("match-statistics.violations.warning-thresholds"));
        assertEquals("keep",loaded.getString("match-statistics.extension.custom"));
        assertEquals("login",loaded.getString("lobbyServer"));
        assertTrue(saved.indexOf("violations:")<saved.indexOf("queue-capacity:"));
        ViolationConfigMigration.migrate(loaded); assertEquals(saved,loaded.saveToString());
    }
}
