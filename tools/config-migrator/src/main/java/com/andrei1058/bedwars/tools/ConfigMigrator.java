package com.andrei1058.bedwars.tools;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Merges a modern BedWars YAML file into a legacy 1.8 template without overwriting the source. */
public final class ConfigMigrator {
    private static final Map<String, String> MATERIALS = Map.ofEntries(
            Map.entry("PLAYER_HEAD", "SKULL_ITEM"), Map.entry("SKELETON_SKULL", "SKULL_ITEM"),
            Map.entry("RED_BED", "BED"), Map.entry("BLUE_BED", "BED"), Map.entry("GREEN_BED", "BED"),
            Map.entry("YELLOW_BED", "BED"), Map.entry("LIME_CONCRETE", "STAINED_GLASS_PANE"),
            Map.entry("YELLOW_CONCRETE", "STAINED_GLASS_PANE"), Map.entry("RED_CONCRETE", "STAINED_GLASS_PANE"),
            Map.entry("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE"), Map.entry("WOODEN_SWORD", "WOOD_SWORD"));
    private static final Map<String, String> SOUNDS = Map.ofEntries(
            Map.entry("BLOCK_WOOL_STEP", "STEP_WOOL"), Map.entry("BLOCK_WOOD_STEP", "STEP_WOOD"),
            Map.entry("BLOCK_STONE_STEP", "STEP_STONE"), Map.entry("BLOCK_SAND_STEP", "STEP_SAND"),
            Map.entry("BLOCK_GRAVEL_STEP", "STEP_GRAVEL"), Map.entry("BLOCK_GRASS_STEP", "STEP_GRASS"),
            Map.entry("ENTITY_PLAYER_BIG_FALL", "FALL_BIG"), Map.entry("ENTITY_PLAYER_SMALL_FALL", "FALL_SMALL"));

    public static void main(String[] args) throws Exception {
        if (args.length != 3) { System.err.println("用法: java -jar bedwars-config-migrator.jar <1.21配置> <1.8模板> <输出目录>"); System.exit(2); }
        Path modern = Path.of(args[0]), legacy = Path.of(args[1]), outDir = Path.of(args[2]);
        Files.createDirectories(outDir);
        DumperOptions options = new DumperOptions(); options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); options.setIndent(2); options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        Map<String, Object> source = map(yaml.load(Files.readString(modern)));
        Map<String, Object> target = map(yaml.load(Files.readString(legacy)));
        List<String> migrated = new ArrayList<>(), skipped = new ArrayList<>(), warnings = new ArrayList<>();
        merge(source, target, "", migrated, skipped, warnings);
        if (source.containsKey("config-version")) skipped.add("config-version (由目标版自行管理)");
        Path output = outDir.resolve(legacy.getFileName());
        Files.writeString(output, yaml.dump(target), StandardCharsets.UTF_8);
        String report = "来源: " + modern.toAbsolutePath() + "\n目标模板: " + legacy.toAbsolutePath() + "\n输出: " + output.toAbsolutePath() +
                "\n\n已迁移路径 (" + migrated.size() + "):\n- " + String.join("\n- ", migrated) +
                "\n\n未迁移路径 (" + skipped.size() + "):\n- " + String.join("\n- ", skipped) +
                "\n\n警告:\n- " + String.join("\n- ", warnings) + "\n";
        Files.writeString(outDir.resolve("migration-report.txt"), report, StandardCharsets.UTF_8);
        System.out.println(report);
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return value instanceof Map<?,?> m ? (Map<String,Object>) m : new LinkedHashMap<>(); }
    private static void merge(Map<String,Object> source, Map<String,Object> target, String prefix, List<String> migrated, List<String> skipped, List<String> warnings) {
        for (var entry : source.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue(), existing = target.get(entry.getKey());
            if (value instanceof Map<?,?> sm) {
                if (existing instanceof Map<?,?> tm) merge(map(sm), map(tm), path, migrated, skipped, warnings); else skipped.add(path + " (目标不是配置节)");
            } else if (target.containsKey(entry.getKey())) {
                Object converted = convert(value, path, warnings); target.put(entry.getKey(), converted); migrated.add(path);
            } else skipped.add(path);
        }
    }
    private static Object convert(Object value, String path, List<String> warnings) {
        if (!(value instanceof String s)) return value;
        String key = path.toLowerCase(Locale.ROOT);
        Map<String,String> table = key.contains("sound") ? SOUNDS
                : (key.contains("material") || key.endsWith(".item") ? MATERIALS : Collections.emptyMap());
        String converted = table.getOrDefault(s, s);
        if (!converted.equals(s)) warnings.add(path + ": " + s + " -> " + converted);
        return converted;
    }
}
