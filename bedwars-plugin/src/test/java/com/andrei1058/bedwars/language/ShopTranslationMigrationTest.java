package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShopTranslationMigrationTest {
    @Test
    void movesCustomLegacyTowerValuesToStableId() {
        YamlConfiguration yml = new YamlConfiguration();
        String oldName = Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY).replace("%content%", "Compact Pop-up Tower");
        String oldLore = Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY).replace("%content%", "Compact Pop-up Tower");
        String newName = Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY).replace("%content%", "tower");
        String newLore = Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY).replace("%content%", "tower");
        yml.set(oldName, "{color}自定义塔");
        yml.set(oldLore, List.of("自定义说明"));

        Language.migrateLegacyTowerShopItem(yml);

        assertEquals("{color}自定义塔", yml.getString(newName));
        assertEquals(List.of("自定义说明"), yml.getStringList(newLore));
        assertNull(yml.get(oldName));
        assertNull(yml.get(oldLore));
    }
}
