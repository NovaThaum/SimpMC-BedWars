package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyBedItemsTest {
    @Test void repairsMigratedBedsAcrossInventoryAndShopWithoutChangingOtherItems() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String root : new String[]{"lobby-items.leave", "pre-game-items.leave", "spectator-items.leave", "stats-gui.beds-destroyed", "utility.content.buy-items.bed"}) {
            yaml.set(root + ".material", "BED"); yaml.set(root + ".data", 14);
            yaml.set(root + ".name", "自定义名称"); yaml.set(root + ".amount", 2);
        }
        yaml.set("wool.material", "WOOL"); yaml.set("wool.data", 14);
        LegacyBedItems.migrate(yaml);
        for (String root : new String[]{"lobby-items.leave", "pre-game-items.leave", "spectator-items.leave", "stats-gui.beds-destroyed", "utility.content.buy-items.bed"}) {
            assertEquals(0, yaml.getInt(root + ".data"));
            assertEquals("自定义名称", yaml.getString(root + ".name")); assertEquals(2, yaml.getInt(root + ".amount"));
        }
        assertEquals(14, yaml.getInt("wool.data"));
        String once = yaml.saveToString(); LegacyBedItems.migrate(yaml); assertEquals(once, yaml.saveToString());
        YamlConfiguration reloaded = new YamlConfiguration(); reloaded.loadFromString(once);
        assertEquals(0, reloaded.getInt("lobby-items.leave.data"));
    }
}
