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
        if (args.length != 2) { System.err.println("用法: java -jar bedwars-config-migrator.jar <1.21插件数据目录> <1.8插件数据目录>"); System.exit(2); }
        Path modernDir = Path.of(args[0]).toAbsolutePath().normalize();
        Path legacyDir = Path.of(args[1]).toAbsolutePath().normalize();
        if (!Files.isDirectory(modernDir) || !Files.isDirectory(legacyDir)) throw new IllegalArgumentException("两个参数都必须是已存在的插件配置目录");
        if (modernDir.equals(legacyDir) || modernDir.startsWith(legacyDir) || legacyDir.startsWith(modernDir))
            throw new IllegalArgumentException("1.21 与 1.8 插件目录必须分开，避免源文件被当作目标模板");
        Path outDir = legacyDir.resolve("migration-output");
        Files.createDirectories(outDir);
        DumperOptions options = new DumperOptions(); options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); options.setIndent(2); options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        List<String> report = new ArrayList<>();
        try (var files = Files.walk(modernDir)) {
            for (Path modern : files.filter(Files::isRegularFile).filter(ConfigMigrator::isYaml).toList()) {
                Path relative = modernDir.relativize(modern);
                if (relative.startsWith("migration-output")) continue;
                Path legacy = legacyDir.resolve(relative);
                List<String> migrated = new ArrayList<>(), skipped = new ArrayList<>(), warnings = new ArrayList<>();
                Map<String,Object> source = map(yaml.load(Files.readString(modern)));
                Map<String,Object> target;
                boolean newArena = relative.getNameCount() > 1 && "Arenas".equalsIgnoreCase(relative.getName(0).toString()) && !Files.isRegularFile(legacy);
                if (newArena) {
                    target = new LinkedHashMap<>(source);
                    normalizeArena(target, skipped, warnings);
                    migrated.add("<新竞技场整文件>");
                } else if (Files.isRegularFile(legacy)) {
                    target = map(yaml.load(Files.readString(legacy)));
                    merge(source, target, "", migrated, skipped, warnings);
                } else { report.add(relative + ": 跳过，1.8 目录没有对应模板"); continue; }
                if (!newArena && source.containsKey("config-version")) skipped.add("config-version (由目标版自行管理)");
                Path output = outDir.resolve(relative);
                Files.createDirectories(output.getParent());
                Files.writeString(output, yaml.dump(target), StandardCharsets.UTF_8);
                report.add(relative + ": 已迁移 " + migrated.size() + " 项，未迁移 " + skipped.size() + " 项，映射 " + warnings.size() + " 项");
                report.addAll(warnings.stream().map(w -> "  映射: " + w).toList());
                report.addAll(skipped.stream().map(s -> "  未迁移: " + s).toList());
            }
        }
        String text = "来源: " + modernDir + "\n目标模板: " + legacyDir + "\n输出: " + outDir + "\n\n" + String.join("\n", report) + "\n";
        Files.writeString(outDir.resolve("migration-report.txt"), text, StandardCharsets.UTF_8);
        System.out.println(text);
    }

    private static boolean isYaml(Path path) { String name = path.getFileName().toString().toLowerCase(Locale.ROOT); return name.endsWith(".yml") || name.endsWith(".yaml"); }

    private static void normalizeArena(Map<String,Object> arena, List<String> skipped, List<String> warnings) {
        arena.remove("config-version");
        arena.remove("minInTeam");
        Object group = arena.get("arena-group");
        if (!arena.containsKey("group") && group != null) { arena.put("group", group); arena.remove("arena-group"); warnings.add("竞技场: arena-group -> group"); }
        Object rules = arena.get("game-rules");
        if (rules instanceof List<?> list) {
            List<Object> converted = new ArrayList<>(list);
            boolean hasDaylight = converted.stream().anyMatch(v -> String.valueOf(v).startsWith("doDaylightCycle:"));
            if (!hasDaylight) converted.add("doDaylightCycle:false");
            arena.put("game-rules", converted);
        }
        skipped.add("config-version (由 1.8 竞技场加载器管理)");
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
