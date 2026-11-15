package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.ArrayList;

/** 修复旧迁移工具写入的床物品颜色数据；1.8 床物品只有 data=0。 */
public final class LegacyBedItems {
    private LegacyBedItems() { }
    public static void migrate(YamlConfiguration yaml) {
        for (String path : new ArrayList<>(yaml.getKeys(true))) {
            if (!path.endsWith(".material")) continue;
            String material = yaml.getString(path, "");
            if (!material.equalsIgnoreCase("BED")) continue;
            String data = path.substring(0, path.length() - "material".length()) + "data";
            yaml.set(data, 0);
        }
    }
}
