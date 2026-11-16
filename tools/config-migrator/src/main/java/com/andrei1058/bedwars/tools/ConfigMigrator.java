package com.andrei1058.bedwars.tools;

import com.andrei1058.bedwars.compat.LegacyMaterials;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Merges a modern BedWars YAML file into a legacy 1.8 template without overwriting the source. */
public final class ConfigMigrator {
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
        Map<Path,String> migratedArenaGroups = new LinkedHashMap<>();
        try (var files = Files.walk(modernDir)) {
            for (Path modern : files.filter(Files::isRegularFile).filter(ConfigMigrator::isYaml).toList()) {
                Path relative = modernDir.relativize(modern);
                if (relative.getNameCount() > 0 && "Languages".equalsIgnoreCase(relative.getName(0).toString())
                        && (relative.getNameCount() != 2 || !"messages_zh_cn.yml".equals(relative.getFileName().toString()))) {
                    report.add(relative + ": 已跳过，仅支持简体中文 messages_zh_cn.yml");
                    continue;
                }
                if (relative.getNameCount() > 0 && "Cache".equalsIgnoreCase(relative.getName(0).toString())) {
                    report.add(relative + ": 主动跳过，Cache 由 1.8.8 插件重新获取地图缓存");
                    continue;
                }
                if (relative.getNameCount() == 1 && relative.getFileName().toString().equals("migration-report.txt")) continue;
                Path legacy = legacyDir.resolve(relative);
                List<String> migrated = new ArrayList<>(), skipped = new ArrayList<>(), warnings = new ArrayList<>(), notes = new ArrayList<>();
                Map<String,Object> source = map(yaml.load(Files.readString(modern)));
                boolean arenaFile = relative.getNameCount() > 1 && "Arenas".equalsIgnoreCase(relative.getName(0).toString());
                String arenaGroup = arenaFile ? readArenaGroup(source) : null;
                if (arenaGroup != null) {
                    source.remove("groups"); source.remove("arena-group"); source.put("group", arenaGroup);
                    warnings.add("竞技场分组转换为 group: " + arenaGroup);
                }
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
                    if (arenaGroup != null) transferableSource.remove("group");
                    boolean upgradesConfig = relative.getNameCount() == 1 && "upgrades2.yml".equalsIgnoreCase(relative.getFileName().toString());
                    if (transferableSource.remove("config-version") != null && !upgradesConfig)
                        notes.add("config-version 已忽略，由目标插件管理");
                    boolean mainConfig = relative.getNameCount() == 1 && "config.yml".equalsIgnoreCase(relative.getFileName().toString());
                    if (mainConfig) {
                        transferableSource.remove("language");
                        transferableSource.remove("disabled-languages");
                        target.remove("language");
                        target.remove("disabled-languages");
                        transferableSource.remove("lobbyLoc");
                        transferableSource.remove("arenaGroups");
                    }
                    Map<String,Object> supportedSwordTiers = new LinkedHashMap<>();
                    if (relative.getNameCount() == 1 && "upgrades2.yml".equalsIgnoreCase(relative.getFileName().toString())) {
                        Map<String,Object> swords = map(transferableSource.get("upgrade-swords"));
                        for (int tier = 2; tier <= 4; tier++) {
                            String key = "tier-" + tier;
                            if (swords.containsKey(key)) supportedSwordTiers.put(key, swords.remove(key));
                        }
                    }
                    boolean languageFile = relative.getNameCount() > 0 && "Languages".equalsIgnoreCase(relative.getName(0).toString());
                    normalizeLegacyItems(transferableSource, "", warnings);
                    normalizeLegacyItems(supportedSwordTiers, "upgrade-swords", warnings);
                    mergeDerivedLegacyItemData(transferableSource, target, "", migrated);
                    mergeDerivedLegacyItemData(supportedSwordTiers, target.get("upgrade-swords") instanceof Map<?,?>
                            ? map(target.get("upgrade-swords")) : new LinkedHashMap<>(), "upgrade-swords", migrated);
                    merge(transferableSource, target, "", migrated, skipped, warnings, notes, languageFile);
                    migrateLegacyConfigKeys(source, target, migrated, warnings, relative);
                    if (arenaGroup != null) {
                        target.remove("groups"); target.remove("arena-group"); target.put("group", arenaGroup);
                        migrated.add("group");
                    }
                    mergeSupportedSwordTiers(supportedSwordTiers, target, migrated, skipped, warnings, notes);
                    if (upgradesConfig) {
                        // 升级价格迁移必须知道源配置版本；目标旧版使用独立 price-schema 标记。
                        target.remove("config-version");
                        target.put("price-schema", 0);
                        if (source.get("config-version") instanceof Integer version) {
                            target.put("config-version", version);
                            notes.add("保留升级价格来源 config-version，启动后由目标插件迁移价格");
                        }
                    }
                } else { report.add(relative + ": 跳过，1.8 目录没有对应模板"); continue; }
                normalizeLegacyItems(target, "", warnings);
                if (arenaFile) normalizeArenaCoordinates(target, "");
                Path output = legacyDir.resolve(relative).normalize();
                if (!output.startsWith(legacyDir)) throw new IllegalArgumentException("配置路径越界: " + relative);
                Files.createDirectories(output.getParent());
                Files.writeString(output, yaml.dump(target), StandardCharsets.UTF_8);
                if (arenaGroup != null) migratedArenaGroups.put(output, arenaGroup);
                report.add(relative + ": 已迁移 " + migrated.size() + " 项，已回退目标版默认值/忽略 " + notes.size() + " 项，未处理 " + skipped.size() + " 项，映射 " + warnings.size() + " 项");
                report.addAll(warnings.stream().map(w -> "  映射: " + w).toList());
                report.addAll(notes.stream().map(n -> "  已回退/说明: " + n).toList());
                report.addAll(skipped.stream().map(s -> "  未处理（需人工确认）: " + s).toList());
            }
        }
        Path main = legacyDir.resolve("config.yml");
        if (!migratedArenaGroups.isEmpty() && Files.isRegularFile(main)) {
            Map<String,Object> values = map(yaml.load(Files.readString(main)));
            List<String> groups = mergedGroups(values.get("arenaGroups"), migratedArenaGroups.values());
            values.put("arenaGroups", groups);
            Files.writeString(main, yaml.dump(values), StandardCharsets.UTF_8);
            for (var arena : migratedArenaGroups.entrySet()) {
                String canonical = groups.stream().filter(g -> g.equalsIgnoreCase(arena.getValue())).findFirst().orElse(arena.getValue());
                if (!canonical.equals(arena.getValue())) {
                    Map<String,Object> map = map(yaml.load(Files.readString(arena.getKey())));
                    map.put("group", canonical); Files.writeString(arena.getKey(), yaml.dump(map), StandardCharsets.UTF_8);
                }
            }
            report.add("config.yml: 已登记迁移地图引用的竞技场组 " + groups);
        }
        String text = "来源: " + modernDir + "\n目标插件目录: " + legacyDir + "\n输出: 直接更新目标目录中的对应配置文件\n\n" + String.join("\n", report) + "\n";
        Files.writeString(reportFile, text, StandardCharsets.UTF_8);
        System.out.println(text);
    }

    private static boolean isYaml(Path path) { String name = path.getFileName().toString().toLowerCase(Locale.ROOT); return name.endsWith(".yml") || name.endsWith(".yaml"); }

    /** 与 1.21.11 的 ArenaGroupPolicy 一致，历史 groups 的首个有效值优先。 */
    private static String readArenaGroup(Map<String,Object> arena) {
        Object old = arena.get("groups");
        if (old instanceof List<?> list) {
            for (Object value : list) if (value instanceof String group && !group.isBlank()) return group.trim();
        } else if (old instanceof String group) return group.isBlank() ? "Default" : group.trim();
        Object current = arena.get("group");
        if (current instanceof String group && !group.isBlank()) return group.trim();
        Object alias = arena.get("arena-group");
        return alias instanceof String group && !group.isBlank() ? group.trim() : null;
    }

    private static List<String> mergedGroups(Object first, Object second) {
        List<String> result = new ArrayList<>();
        for (Object source : new Object[]{first, second}) if (source instanceof Collection<?> values) {
            for (Object value : values) if (value instanceof String group && !group.isBlank()
                    && result.stream().noneMatch(known -> known.equalsIgnoreCase(group.trim()))) result.add(group.trim());
        }
        return result;
    }

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

    private static void mergeSupportedSwordTiers(Map<String,Object> tiers, Map<String,Object> target,
                                                  List<String> migrated, List<String> skipped,
                                                  List<String> warnings, List<String> notes) {
        if (tiers.isEmpty()) return;
        Map<String,Object> targetSwords = map(target.get("upgrade-swords"));
        if (targetSwords.isEmpty()) {
            notes.add("upgrade-swords (目标配置缺少兼容的升级节，保留目标版默认值)");
            return;
        }
        merge(tiers, targetSwords, "upgrade-swords", migrated, skipped, warnings, notes, true);
        target.put("upgrade-swords", targetSwords);
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
                } else notes.add(path + " (新版配置节无 1.8.8 等价实现，使用目标版默认值)");
            } else if (target.containsKey(entry.getKey())) {
                if (existing instanceof Map<?,?>) {
                    if (addMissingScalars) notes.add(path + " (新版标量无法覆盖目标配置节，使用目标版默认值)");
                    else skipped.add(path + " (源和目标类型不兼容)");
                } else {
                    if (addMissingScalars) {
                        Object converted = convertPreservingNpcFacing(value, existing, path, warnings);
                        target.put(entry.getKey(), converted);
                        migrated.add(path + " (使用 1.21.11 值)");
                    } else {
                        Object converted = convertPreservingNpcFacing(value, existing, path, warnings); target.put(entry.getKey(), converted); migrated.add(path);
                    }
                }
            } else if (addMissingScalars) {
                target.put(entry.getKey(), convert(value, path, warnings));
                migrated.add(path + " (新增语言键)");
                    } else notes.add(path + " (目标版无对应配置项，使用目标版默认值)");
        }
    }
    private static Object convertPreservingNpcFacing(Object value, Object existing, String path, List<String> warnings) {
        if (path.matches("(?i)Team\\.[^.]+\\.(Shop|Upgrade)") && value instanceof String source && existing instanceof String target) {
            String[] src = source.replace("[", "").replace("]", "").split(",", -1);
            String[] dst = target.replace("[", "").replace("]", "").split(",", -1);
            if (src.length == 3 && dst.length == 5) return source.replace("[", "").replace("]", "") + "," + dst[3].trim() + "," + dst[4].trim();
        }
        return convert(value, path, warnings);
    }

    private static Object convert(Object value, String path, List<String> warnings) {
        if (path.startsWith("start-items-per-group.") && value instanceof List<?> list)
            return list.stream().map(item -> {
                if (!(item instanceof String text)) return item;
                String[] parts = text.split(",", 2);
                return LegacyMaterials.resolve(parts[0]).material() + (parts.length == 2 ? "," + parts[1] : "");
            }).toList();
        if (!(value instanceof String s)) return value;
        String location = legacyLocation(s);
        if (location != null) return location;
        if (keyEndsWithColor(path)) {
            String color = s.equalsIgnoreCase("cyan") ? "AQUA" : s.toUpperCase(Locale.ROOT);
            if (!color.equals(s)) warnings.add(path + ": " + s + " -> " + color);
            return color;
        }
        String key = path.toLowerCase(Locale.ROOT);
        String converted = key.contains("sound") ? SOUNDS.getOrDefault(s, s)
                : (key.endsWith("material") || key.endsWith(".item") ? LegacyMaterials.resolve(s).material() : s);
        if (!converted.equals(s)) warnings.add(path + ": " + s + " -> " + converted);
        return converted;
    }

    private static void migrateLegacyConfigKeys(Map<String,Object> source, Map<String,Object> target,
                                                List<String> migrated, List<String> warnings, Path relative) {
        if (relative.getNameCount() != 1 || !"config.yml".equalsIgnoreCase(relative.getFileName().toString())) return;
        Object lobby = source.get("lobbyLoc");
        if (lobby instanceof String value) {
            String converted = convert(value, "lobbyLoc", warnings).toString();
            target.put("lobbyLoc", converted);
            migrated.add("lobbyLoc");
        }
        Object groups = source.get("arenaGroups");
        if (groups instanceof List<?> list) {
            target.put("arenaGroups", mergedGroups(target.get("arenaGroups"), list));
            migrated.add("arenaGroups");
        }
    }

    private static void normalizeLegacyItems(Map<String,Object> values, String prefix, List<String> warnings) {
        for (Map.Entry<String,Object> entry : new ArrayList<>(values.entrySet())) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?,?> nested) {
                normalizeLegacyItems(map(nested), path, warnings);
                continue;
            }
            if (!entry.getKey().endsWith("material") || !(entry.getValue() instanceof String material)) continue;
            LegacyMaterials.Item item = LegacyMaterials.resolve(material);
            String legacy = item.material();
            if (legacy.equals(material) && item.data() == null) continue;
            entry.setValue(legacy);
            warnings.add(path + ": " + material + " -> " + legacy);
            Map<String,Object> parent = values;
            Integer data = item.data();
            if (data != null) {
                parent.put(entry.getKey().substring(0, entry.getKey().length() - "material".length()) + "data", data);
                warnings.add(path + ".data: 自动映射为 " + data);
            }
        }
    }

    private static void mergeDerivedLegacyItemData(Map<String,Object> source, Map<String,Object> target,
                                                   String prefix, List<String> migrated) {
        for (Map.Entry<String,Object> entry : source.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (!(entry.getValue() instanceof Map<?,?> sourceMap)) continue;
            Map<String,Object> sourceSection = map(sourceMap);
            Object targetValue = target.get(entry.getKey());
            if (!(targetValue instanceof Map<?,?> targetMap)) continue;
            Map<String,Object> targetSection = map(targetMap);
            Object material = sourceSection.get("material");
            if (material instanceof String name && LegacyMaterials.resolve(name).material()
                    .equalsIgnoreCase(String.valueOf(targetSection.get("material")))
                    && sourceSection.get("data") instanceof Number data && !targetSection.containsKey("data")) {
                targetSection.put("data", data);
                migrated.add(path + ".data");
            }
            mergeDerivedLegacyItemData(sourceSection, targetSection, path, migrated);
        }
    }

    private static String legacyLocation(String value) {
        String clean = value.trim().replace("[", "").replace("]", "");
        String[] parts = clean.split(",");
        if (parts.length != 3) return null;
        try {
            Double.parseDouble(parts[0].trim());
            Double.parseDouble(parts[1].trim());
            Double.parseDouble(parts[2].trim());
            return clean;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void normalizeArenaCoordinates(Map<String,Object> values, String prefix) {
        for (var entry : values.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?,?> nested) normalizeArenaCoordinates(map(nested), path);
            else if (path.matches("(?i)(spectator-loc|waiting\\.(Loc|Pos[^.]+)|generator\\.(Diamond|Emerald)|Team\\.[^.]+\\.(Spawn|Bed|Shop|Upgrade|Iron|Gold|Diamond|Emerald|kill-drops-loc))")) {
                if (value instanceof String text) entry.setValue(arenaCoordinates(text, path));
                else if (value instanceof List<?> list) entry.setValue(list.stream()
                        .map(v -> v instanceof String text ? arenaCoordinates(text, path) : v).toList());
            }
        }
    }

    private static String arenaCoordinates(String text, String path) {
        String clean = text.trim().replace("[", "").replace("]", "");
        String[] parts = clean.split(",", -1);
        if (parts.length != 3 && parts.length != 5) return text;
        try {
            for (int i = 0; i < 3; i++)
                if (!Double.isFinite(Double.parseDouble(parts[i].trim()))) return text;
            String xyz = parts[0].trim() + "," + parts[1].trim() + "," + parts[2].trim();
            if (path.matches("(?i)Team\\.[^.]+\\.(Shop|Upgrade)")) {
                if (parts.length == 3) return xyz + ",0.0,0.0";
                if (!Float.isFinite(Float.parseFloat(parts[3].trim())) || !Float.isFinite(Float.parseFloat(parts[4].trim()))) return text;
                return xyz + "," + parts[3].trim() + "," + parts[4].trim();
            }
            return xyz;
        } catch (NumberFormatException ignored) { return text; }
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
