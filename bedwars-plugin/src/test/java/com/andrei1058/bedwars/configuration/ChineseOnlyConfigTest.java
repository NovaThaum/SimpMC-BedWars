package com.andrei1058.bedwars.configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ChineseOnlyConfigTest {
    @Test void retiresOnlyLanguageSelectionAndPreservesAdministratorConfiguration() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "en");
        config.set("disabled-languages", List.of("zh_cn"));
        config.set("lobbyServer", "login");
        config.set("custom", 123);
        MainConfig.migrateChineseOnly(config);
        MainConfig.migrateChineseOnly(config);
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(config.saveToString());
        assertFalse(reloaded.contains("language"));
        assertFalse(reloaded.contains("disabled-languages"));
        assertEquals("login", reloaded.getString("lobbyServer"));
        assertEquals(123, reloaded.getInt("custom"));
    }
}
