package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将违规策略补到统计配置节内部，保留旧值、未知扩展项及其他配置节。 */
final class ViolationConfigMigration {
    private ViolationConfigMigration() { }
    static void migrate(YamlConfiguration yaml) {
        ConfigurationSection old = yaml.getConfigurationSection("match-statistics");
        Map<String, Object> values = old == null ? new LinkedHashMap<>() : new LinkedHashMap<>(old.getValues(true));
        LinkedHashMap<String, Object> template = new LinkedHashMap<>();
        template.put("enabled", true);
        template.put("timezone", "Asia/Shanghai");
        template.put("report-interval-seconds", 300);
        template.put("finish-grace-ticks", 40);
        template.put("violations.enabled", true);
        template.put("violations.match-leave-threshold", 25);
        template.put("violations.cross-team-item-transfer", true);
        template.put("violations.warning-thresholds", List.of(10, 20, 50, 100));
        template.put("queue-capacity", 10000);
        template.put("retry-delay-seconds", 5);
        yaml.set("match-statistics", null);
        for (Map.Entry<String, Object> entry : template.entrySet())
            yaml.set("match-statistics." + entry.getKey(), values.getOrDefault(entry.getKey(), entry.getValue()));
        for (Map.Entry<String, Object> entry : values.entrySet())
            if (!(entry.getValue() instanceof ConfigurationSection) && !template.containsKey(entry.getKey()))
                yaml.set("match-statistics." + entry.getKey(), entry.getValue());
    }
}
