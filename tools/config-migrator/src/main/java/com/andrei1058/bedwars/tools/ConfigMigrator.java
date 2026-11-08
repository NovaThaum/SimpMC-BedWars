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
        if (modernDir.equals(legacyDir) || modernDir.startsWith(legacyDir))
            throw new IllegalArgumentException("1.21 源目录不能等于或位于 1.8 插件目录内");
        Path reportFile = legacyDir.resolve("migration-report.txt");
        DumperOptions options = new DumperOptions(); options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); options.setIndent(2); options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        List<String> report = new ArrayList<>();
        try (var files = Files.walk(modernDir)) {
            for (Path modern : files.filter(Files::isRegularFile).filter(ConfigMigrator::isYaml).toList()) {
                Path relative = modernDir.relativize(modern);
                if (relative.getNameCount() > 0 && "Cache".equalsIgnoreCase(relative.getName(0).toString())) {
                    report.add(relative + ": 主动跳过，Cache 由 1.8.8 插件重新获取地图缓存");
                    continue;
                }
                if (relative.getNameCount() == 1 && relative.getFileName().toString().equals("migration-report.txt")) continue;
                Path legacy = legacyDir.resolve(relative);
                List<String> migrated = new ArrayList<>(), skipped = new ArrayList<>(), warnings = new ArrayList<>(), notes = new ArrayList<>();
                Map<String,Object> source = map(yaml.load(Files.readString(modern)));
                Map<String,Object> target;
                boolean newArena = relative.getNameCount() > 1 && "Arenas".equalsIgnoreCase(relative.getName(0).toString()) && !Files.isRegularFile(legacy);
                if (newArena) {
                    target = new LinkedHashMap<>(source);
                    normalizeArena(target, notes, warnings);
                    normalizeValues(target, "", warnings);
                    migrated.addAll(leafPaths(target, ""));
                } else if (Files.isRegularFile(legacy)) {
                    target = map(yaml.load(Files.readString(legacy)));
                    Map<String,Object> transferableSource = new LinkedHashMap<>(source);
                    if (transferableSource.remove("config-version") != null) notes.add("config-version 已忽略，由目标插件管理");
                    boolean languageFile = relative.getNameCount() > 0 && "Languages".equalsIgnoreCase(relative.getName(0).toString());
                    merge(transferableSource, target, "", migrated, skipped, warnings, notes, languageFile);
                } else { report.add(relative + ": 跳过，1.8 目录没有对应模板"); continue; }
                Path output = legacyDir.resolve(relative).normalize();
                if (!output.startsWith(legacyDir)) throw new IllegalArgumentException("配置路径越界: " + relative);
                Files.createDirectories(output.getParent());
                Files.writeString(output, yaml.dump(target), StandardCharsets.UTF_8);
                report.add(relative + ": 已迁移 " + migrated.size() + " 项，目标不支持/已忽略 " + notes.size() + " 项，未迁移 " + skipped.size() + " 项，映射 " + warnings.size() + " 项");
                report.addAll(warnings.stream().map(w -> "  映射: " + w).toList());
                report.addAll(notes.stream().map(n -> "  已忽略: " + n).toList());
                report.addAll(skipped.stream().map(s -> "  旧版不支持/结构冲突: " + s).toList());
            }
        }
        String text = "来源: " + modernDir + "\n目标插件目录: " + legacyDir + "\n输出: 直接更新目标目录中的对应配置文件\n\n" + String.join("\n", report) + "\n";
        Files.writeString(reportFile, text, StandardCharsets.UTF_8);
        System.out.println(text);
    }

    private static boolean isYaml(Path path) { String name = path.getFileName().toString().toLowerCase(Locale.ROOT); return name.endsWith(".yml") || name.endsWith(".yaml"); }

    private static void normalizeArena(Map<String,Object> arena, List<String> notes, List<String> warnings) {
        arena.remove("config-version");
        notes.add("config-version 已忽略，由 1.8 竞技场加载器管理");
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
    }

    private static List<String> leafPaths(Map<String,Object> values, String prefix) {
        List<String> paths = new ArrayList<>();
        for (var entry : values.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?,?> nested) paths.addAll(leafPaths(map(nested), path));
            else paths.add(path);
        }
        return paths;
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return value instanceof Map<?,?> m ? (Map<String,Object>) m : new LinkedHashMap<>(); }
    private static void merge(Map<String,Object> source, Map<String,Object> target, String prefix, List<String> migrated, List<String> skipped, List<String> warnings, List<String> notes, boolean addMissingScalars) {
        for (var entry : source.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue(), existing = target.get(entry.getKey());
            if (value instanceof Map<?,?> sm) {
                if (existing instanceof Map<?,?> tm) merge(map(sm), map(tm), path, migrated, skipped, warnings, notes, addMissingScalars);
                else if (!target.containsKey(entry.getKey()) && addMissingScalars) {
                    Map<String,Object> added = new LinkedHashMap<>();
                    merge(map(sm), added, path, migrated, skipped, warnings, notes, true);
                    target.put(entry.getKey(), added);
                } else skipped.add(path + " (源为配置节，目标为标量，保留目标值)");
            } else if (target.containsKey(entry.getKey())) {
                if (existing instanceof Map<?,?>) {
                    if (addMissingScalars) skipped.add(path + " (源为标量，目标为配置节，保留目标节)");
                    else skipped.add(path + " (源和目标类型不兼容)");
                } else {
                    if (addMissingScalars) {
                        Object converted = convert(value, path, warnings);
                        target.put(entry.getKey(), converted);
                        migrated.add(path + " (使用 1.21.11 值)");
                    } else {
                        Object converted = convert(value, path, warnings); target.put(entry.getKey(), converted); migrated.add(path);
                    }
                }
            } else if (addMissingScalars) {
                target.put(entry.getKey(), convert(value, path, warnings));
                migrated.add(path + " (新增语言键)");
            } else skipped.add(path);
        }
    }
    private static Object convert(Object value, String path, List<String> warnings) {
        if (!(value instanceof String s)) return value;
        String location = legacyLocation(s);
        if (location != null) return location;
        if (keyEndsWithColor(path)) {
            String color = s.equalsIgnoreCase("cyan") ? "AQUA" : s.toUpperCase(Locale.ROOT);
            if (!color.equals(s)) warnings.add(path + ": " + s + " -> " + color);
            return color;
        }
        String key = path.toLowerCase(Locale.ROOT);
        Map<String,String> table = key.contains("sound") ? SOUNDS
                : (key.contains("material") || key.endsWith(".item") ? MATERIALS : Collections.emptyMap());
        String converted = table.getOrDefault(s, s);
        if (!converted.equals(s)) warnings.add(path + ": " + s + " -> " + converted);
        return converted;
    }

    private static String legacyLocation(String value) {
        String clean = value.trim().replace("[", "").replace("]", "");
        String[] parts = clean.split(",");
        if (parts.length != 3) return null;
        try {
            Double.parseDouble(parts[0].trim());
            Double.parseDouble(parts[1].trim());
            Double.parseDouble(parts[2].trim());
            return clean + ",0,0";
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean keyEndsWithColor(String path) {
        String key = path.toLowerCase(Locale.ROOT);
        return key.endsWith(".color") || key.endsWith("color") || key.endsWith("team-color");
    }

    private static void normalizeValues(Map<String,Object> values, String prefix, List<String> warnings) {
        for (Map.Entry<String,Object> entry : values.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?,?> nested) normalizeValues(map(nested), path, warnings);
            else if (entry.getValue() instanceof String string) entry.setValue(convert(string, path, warnings));
        }
    }
}
