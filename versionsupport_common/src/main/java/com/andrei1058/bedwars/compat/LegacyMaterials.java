package com.andrei1058.bedwars.compat;

import java.util.Locale;
import java.util.Map;

/** 1.8.8 配置材质映射。此文件与独立迁移工具中的副本保持一致。 */
public final class LegacyMaterials {
    private LegacyMaterials() { }
    public record Item(String material, Integer data) { }
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("SNOWBALL", "SNOW_BALL"), Map.entry("FIRE_CHARGE", "FIREBALL"),
        Map.entry("END_STONE", "ENDER_STONE"), Map.entry("OAK_PLANKS", "WOOD"),
        Map.entry("OAK_WOOD", "WOOD"), Map.entry("OAK_LOG", "LOG"),
        Map.entry("WOODEN_SWORD", "WOOD_SWORD"), Map.entry("WOODEN_PICKAXE", "WOOD_PICKAXE"),
        Map.entry("WOODEN_AXE", "WOOD_AXE"), Map.entry("WOODEN_SHOVEL", "WOOD_SPADE"),
        Map.entry("GOLDEN_SWORD", "GOLD_SWORD"), Map.entry("GOLDEN_PICKAXE", "GOLD_PICKAXE"),
        Map.entry("GOLDEN_AXE", "GOLD_AXE"), Map.entry("GOLDEN_SHOVEL", "GOLD_SPADE"),
        Map.entry("IRON_SHOVEL", "IRON_SPADE"), Map.entry("DIAMOND_SHOVEL", "DIAMOND_SPADE"),
        Map.entry("STONE_SHOVEL", "STONE_SPADE"), Map.entry("BREWING_STAND", "BREWING_STAND_ITEM"),
        Map.entry("EXPERIENCE_BOTTLE", "EXP_BOTTLE"), Map.entry("GUNPOWDER", "SULPHUR"),
        Map.entry("CRAFTING_TABLE", "WORKBENCH"), Map.entry("COBWEB", "WEB"));
    private static final String[] COLORS = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
        "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
    public static Item resolve(String name) {
        if (name == null) return new Item(null, null);
        String key = name.trim().toUpperCase(Locale.ROOT);
        if (key.equals("BED")) return new Item("BED", 0);
        if (key.equals("PLAYER_HEAD")) return new Item("SKULL_ITEM", 3);
        if (key.equals("SKELETON_SKULL")) return new Item("SKULL_ITEM", 0);
        if (key.equals("IRON_GOLEM_SPAWN_EGG")) return new Item("MONSTER_EGG", 99);
        if (key.equals("HORSE_SPAWN_EGG")) return new Item("MONSTER_EGG", 100);
        if (key.equals("SILVERFISH_SPAWN_EGG")) return new Item("MONSTER_EGG", 60);
        if (key.equals("ZOMBIE_SPAWN_EGG")) return new Item("MONSTER_EGG", 54);
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i] + "_";
            if (!key.startsWith(color)) continue;
            String suffix = key.substring(color.length());
            return switch (suffix) {
                case "BED" -> new Item("BED", 0);
                case "WOOL" -> new Item("WOOL", i);
                case "CARPET" -> new Item("CARPET", i);
                case "TERRACOTTA" -> new Item("STAINED_CLAY", i);
                case "STAINED_GLASS" -> new Item("STAINED_GLASS", i);
                case "STAINED_GLASS_PANE", "CONCRETE" -> new Item("STAINED_GLASS_PANE", i);
                default -> new Item(ALIASES.getOrDefault(key, key), null);
            };
        }
        String alias = ALIASES.getOrDefault(key, key);
        return new Item(alias, key.equals("OAK_WOOD") || key.equals("OAK_PLANKS") || key.equals("OAK_LOG") ? 0 : null);
    }
}
