/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.configuration.GameMainOverridable;
import com.andrei1058.bedwars.arena.NpcFacing;
import com.andrei1058.bedwars.arena.PlayerFacing;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ArenaConfig extends ConfigManager {

    @SuppressWarnings({"SpellCheckingInspection"})
    private List<String> cachedGameOverridables = new ArrayList<>();

    public ArenaConfig(Plugin plugin, String name, String dir) {
        super(plugin, name, dir);

        YamlConfiguration yml = getYml();
        migrateArenaLocations(yml);
        boolean hadMinPlayers = yml.get("minPlayers") != null;
        Integer legacyMinInTeam = yml.get("minInTeam") == null ? null : yml.getInt("minInTeam");
        yml.options().header(plugin.getName() + " arena configuration file.\n" +
                "Documentation here: https://gitlab.com/andrei1058/BedWars1058/wikis/configuration/Arena-Configuration");
        yml.addDefault("group", "Default");
        yml.addDefault(ConfigPath.ARENA_DISPLAY_NAME, "");
        yml.addDefault("minPlayers", 2);
        yml.addDefault("maxInTeam", 1);
        yml.addDefault("allowSpectate", true);
        yml.addDefault(ConfigPath.ARENA_SPAWN_PROTECTION, 5);
        yml.addDefault(ConfigPath.ARENA_SHOP_PROTECTION, 1);
        yml.addDefault(ConfigPath.ARENA_UPGRADES_PROTECTION, 1);
        yml.addDefault(ConfigPath.ARENA_GENERATOR_PROTECTION, 1);
        yml.addDefault(ConfigPath.ARENA_ISLAND_RADIUS, 17);
        yml.addDefault("worldBorder", 300);
        yml.addDefault(ConfigPath.ARENA_Y_LEVEL_KILL, -1);
        //yml.addDefault("disableGeneratorsOnOrphanIslands", false);
        yml.addDefault(ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y, 180);
        yml.addDefault(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS, false);
        yml.addDefault(ConfigPath.ARENA_DISABLE_NPCS_FOR_EMPTY_TEAMS, true);
        yml.addDefault(ConfigPath.ARENA_NORMAL_DEATH_DROPS, false);
        yml.addDefault(ConfigPath.ARENA_USE_BED_HOLO, true);
        yml.addDefault(ConfigPath.ARENA_ALLOW_MAP_BREAK, false);
        ArrayList<String> rules = new ArrayList<>();
        rules.add("doDaylightCycle:false");
        rules.add("announceAdvancements:false");
        rules.add("doInsomnia:false");
        rules.add("doImmediateRespawn:true");
        rules.add("doWeatherCycle:false");
        rules.add("doFireTick:false");
        yml.addDefault(ConfigPath.ARENA_GAME_RULES, rules);
        yml.options().copyDefaults(true);
        save();

        //convert old configuration
        if (yml.get("spawnProtection") != null) {
            set(ConfigPath.ARENA_SPAWN_PROTECTION, yml.getInt("spawnProtection"));
            set("spawnProtection", null);
        }
        if (yml.get("shopProtection") != null) {
            set(ConfigPath.ARENA_SHOP_PROTECTION, yml.getInt("shopProtection"));
            set("shopProtection", null);
        }
        if (yml.get("upgradesProtection") != null) {
            set(ConfigPath.ARENA_UPGRADES_PROTECTION, yml.getInt("upgradesProtection"));
            set("upgradesProtection", null);
        }
        if (yml.get("islandRadius") != null) {
            set(ConfigPath.ARENA_ISLAND_RADIUS, yml.getInt("islandRadius"));
        }
        if (yml.get("voidKill") != null) {
            set("voidKill", null);
        }
        set(ConfigPath.GENERAL_CONFIGURATION_ENABLE_GEN_SPLIT, null);

        migrateMinimumPlayers(yml, hadMinPlayers, legacyMinInTeam);
        save();

        cachedGameOverridables = getGameOverridables();
    }

    static void migrateArenaLocations(YamlConfiguration yml) {
        migratePlayerFacing(yml, "waiting.Loc", ConfigPath.ARENA_WAITING_FACING);
        migratePlayerFacing(yml, ConfigPath.ARENA_SPEC_LOC, ConfigPath.ARENA_SPEC_FACING);
        org.bukkit.configuration.ConfigurationSection teams = yml.getConfigurationSection("Team");
        if (teams != null) {
            for (String team : teams.getKeys(false)) {
                String root = "Team." + team + ".";
                migratePlayerFacing(yml, root + "Spawn", root + ConfigPath.ARENA_TEAM_SPAWN_FACING);
                migrateNpcFacing(yml, root + "Shop", root + ConfigPath.ARENA_TEAM_SHOP_FACING,
                        root + "Spawn");
                migrateNpcFacing(yml, root + "Upgrade", root + ConfigPath.ARENA_TEAM_UPGRADE_FACING,
                        root + "Spawn");
            }
        }
        for (String path : new ArrayList<>(yml.getKeys(true))) {
            if (!isArenaLocationPath(path)) continue;
            Object value = yml.get(path);
            if (value instanceof String text) yml.set(path, normalizeArenaLocationString(text));
            else if (value instanceof List<?> values) {
                List<String> normalized = new ArrayList<>();
                for (Object entry : values) {
                    if (!(entry instanceof String text))
                        throw new IllegalArgumentException("竞技场坐标列表格式错误: " + path);
                    normalized.add(normalizeArenaLocationString(text));
                }
                yml.set(path, normalized);
            }
        }
    }

    private static void migratePlayerFacing(YamlConfiguration yml, String locationPath, String facingPath) {
        migrateFacing(yml, locationPath, facingPath, false, null);
    }

    private static void migrateNpcFacing(YamlConfiguration yml, String locationPath, String facingPath,
                                         String fallbackPath) {
        migrateFacing(yml, locationPath, facingPath, true, fallbackPath);
    }

    private static void migrateFacing(YamlConfiguration yml, String locationPath, String facingPath,
                                      boolean npc, String fallbackPath) {
        Object rawLocation = yml.get(locationPath);
        if (!(rawLocation instanceof String location)) return;
        String[] parts = location.replace("[", "").replace("]", "").split(",", -1);
        if (parts.length < 3) return;

        if (yml.isSet(facingPath)) {
            float yaw = normalizeFacing(yml.get(facingPath).toString());
            yml.set(facingPath, npc ? yaw : PlayerFacing.serialize(yaw, 0.0F));
            return;
        }
        if (parts.length >= 4) {
            yml.set(facingPath, npc ? NpcFacing.normalize(Float.parseFloat(parts[3].trim()))
                    : PlayerFacing.serialize(Float.parseFloat(parts[3].trim()),
                    parts.length >= 5 ? Float.parseFloat(parts[4].trim()) : 0.0F));
            return;
        }
        if (!npc) {
            yml.set(facingPath, PlayerFacing.serialize(0.0F, 0.0F));
            return;
        }
        if (fallbackPath == null || !yml.isString(fallbackPath)) return;
        String[] target = yml.getString(fallbackPath).replace("[", "").replace("]", "").split(",");
        if (target.length < 3) return;
        yml.set(facingPath, NpcFacing.toward(Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[2].trim()), Double.parseDouble(target[0].trim()),
                Double.parseDouble(target[2].trim())));
    }

    private static float normalizeFacing(String value) {
        return NpcFacing.normalize(Float.parseFloat(value.replace("[", "").replace("]", "")
                .split(",")[0].trim()));
    }

    static void migrateMinimumPlayers(YamlConfiguration yml, boolean hadMinPlayers, Integer legacyMinInTeam) {
        if (!hadMinPlayers && legacyMinInTeam != null) {
            yml.set("minPlayers", Math.max(2, legacyMinInTeam));
        }
        yml.set("minPlayers", Math.max(2, yml.getInt("minPlayers", 2)));
        yml.set("minInTeam", null);
    }

    @SuppressWarnings({"SpellCheckingInspection"})
    private @NotNull List<String> getGameOverridables() {
        List<String> paths = new ArrayList<>();
        for (Field field : ConfigPath.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(GameMainOverridable.class)) {
                try {
                    Object value = field.get(field);
                    if (value instanceof String) {
                        paths.add((String) value);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        return paths;
    }

    public boolean isGameOverridable(String path) {
        return cachedGameOverridables.contains(path);
    }

    public Object getGameOverridableValue(String path) {
        if (!isGameOverridable(path)) {
            throw new RuntimeException("Given path is not game-overridable: "+path);
        }

        Object value = getYml().get(path, null);
        if (null == value){
            return BedWars.config.getYml().get(path);
        }
        return value;
    }

    public Boolean getGameOverridableBoolean(String path) {
        Object value = getGameOverridableValue(path);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public String getGameOverridableString(String path) {
        Object value = getGameOverridableValue(path);
        return value instanceof String ? (String) value : "invalid";
    }
}
