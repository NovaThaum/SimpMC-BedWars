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
    @Test void repairsModernShopMaterialsAndPreservesPricesNamesAndUnknownValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        String[][] cases = {{"WHITE_WOOL", "WOOL", "0"}, {"RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "14"},
            {"GREEN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "13"}, {"ORANGE_TERRACOTTA", "STAINED_CLAY", "1"},
            {"BLUE_BED", "BED", "0"}, {"PLAYER_HEAD", "SKULL_ITEM", "3"}};
        for (int n = 0; n < cases.length; n++) {
            yaml.set("item" + n + ".material", cases[n][0]); yaml.set("item" + n + ".data", 7);
            yaml.set("item" + n + ".cost", 37); yaml.set("item" + n + ".name", "自定义");
        }
        yaml.set("specials.silverfish.material", "SNOWBALL");
        yaml.set("settings.empty-slot-material", "RED_STAINED_GLASS_PANE");
        yaml.set("custom.material", "MY_CUSTOM_ITEM"); yaml.set("custom.data", 42);
        LegacyBedItems.migrate(yaml);
        for (int n = 0; n < cases.length; n++) {
            assertEquals(cases[n][1], yaml.getString("item" + n + ".material"));
            assertEquals(Integer.parseInt(cases[n][2]), yaml.getInt("item" + n + ".data"));
            assertEquals(37, yaml.getInt("item" + n + ".cost")); assertEquals("自定义", yaml.getString("item" + n + ".name"));
        }
        assertEquals("SNOW_BALL", yaml.getString("specials.silverfish.material"));
        assertEquals(14, yaml.getInt("settings.empty-slot-data"));
        assertEquals("MY_CUSTOM_ITEM", yaml.getString("custom.material")); assertEquals(42, yaml.getInt("custom.data"));
        String once = yaml.saveToString(); LegacyBedItems.migrate(yaml); assertEquals(once, yaml.saveToString());
    }
    @Test void resolvesEveryMaterialFromReportedModernShopAgainstActual188Enum() {
        for (String name : new String[]{"ARROW", "BLAZE_ROD", "BOW", "BREWING_STAND", "CHAINMAIL_BOOTS", "CHAINMAIL_LEGGINGS", "CHEST", "DIAMOND_AXE", "DIAMOND_BOOTS", "DIAMOND_LEGGINGS", "DIAMOND_PICKAXE", "DIAMOND_SWORD", "EGG", "ENDER_PEARL", "END_STONE", "FIRE_CHARGE", "GLASS", "GOLDEN_APPLE", "GOLDEN_AXE", "GOLDEN_PICKAXE", "GOLDEN_SWORD", "GRAY_STAINED_GLASS_PANE", "GREEN_STAINED_GLASS_PANE", "IRON_AXE", "IRON_BOOTS", "IRON_GOLEM_SPAWN_EGG", "IRON_LEGGINGS", "IRON_PICKAXE", "IRON_SWORD", "LADDER", "MILK_BUCKET", "NETHER_STAR", "OAK_WOOD", "OBSIDIAN", "ORANGE_TERRACOTTA", "PAPER", "POTION", "RED_STAINED_GLASS_PANE", "SHEARS", "SNOWBALL", "SPONGE", "STICK", "STONE_PICKAXE", "STONE_SWORD", "TNT", "WATER_BUCKET", "WHITE_WOOL", "WOODEN_AXE", "WOODEN_PICKAXE"}) {
            var item = com.andrei1058.bedwars.compat.LegacyMaterials.resolve(name);
            assertNotNull(org.bukkit.Material.matchMaterial(item.material()), name + " remains invalid: " + item.material());
        }
    }
}
