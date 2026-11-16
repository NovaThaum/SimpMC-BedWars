package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.compat.LegacyMaterials;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.ArrayList;

/** 启动时修正已迁移配置中的现代材质名和 1.8 物品数据。 */
public final class LegacyBedItems {
    private LegacyBedItems() { }
    public static void migrate(YamlConfiguration yaml) {
        for (String path : new ArrayList<>(yaml.getKeys(true))) {
            if (path.startsWith("start-items-per-group.") && yaml.isList(path)) {
                yaml.set(path, yaml.getStringList(path).stream().map(value -> {
                    String[] parts = value.split(",", 2);
                    return LegacyMaterials.resolve(parts[0]).material() + (parts.length == 2 ? "," + parts[1] : "");
                }).toList());
            }
            if (!path.endsWith("material") || !yaml.isString(path)) continue;
            String original = yaml.getString(path);
            LegacyMaterials.Item item = LegacyMaterials.resolve(original);
            if (!item.material().equals(original)) yaml.set(path, item.material());
            if (item.data() != null) yaml.set(path.substring(0, path.length() - "material".length()) + "data", item.data());
        }
    }
}
