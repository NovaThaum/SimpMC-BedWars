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

package com.andrei1058.bedwars.api.configuration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigManager {

    private YamlConfiguration yml;
    private File config;
    private String name;
    private boolean firstTime = false;

    /**
     * Create a new configuration file.
     *
     * @param plugin config owner.
     * @param name   config name. Do not include .yml in it.
     */
    public ConfigManager(Plugin plugin, String name, String dir) {
        File d = new File(dir);

        if (!d.exists()) {
            if (!d.mkdirs()) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + d.getPath());
                return;
            }
        }

        config = new File(dir, name + ".yml");
        if (!config.exists()) {
            firstTime = true;
            plugin.getLogger().log(Level.INFO, "Creating " + config.getPath());
            try {
                if (!config.createNewFile()) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create " + config.getPath());
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        yml = YamlConfiguration.loadConfiguration(config);
        yml.options().copyDefaults(true);
        this.name = name;
    }

    /**
     * Reload configuration.
     */
    public void reload() {
        yml = YamlConfiguration.loadConfiguration(config);
    }

    /**
     * Convert a location to an arena location syntax
     */
    public String stringLocationArenaFormat(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    /**
     * Convert a location to a string for general use
     * Use {@link #stringLocationArenaFormat(Location)} for arena locations
     */
    public String stringLocationConfigFormat(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + (double) loc.getYaw() + "," + (double) loc.getPitch() + "," + loc.getWorld().getName();
    }

    /**
     * Save a general location to the config.
     * Use {@link #saveArenaLoc(String, Location)} for arena locations
     */
    public void saveConfigLoc(String path, Location loc) {
        String data = loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + (double) loc.getYaw() + "," + (double) loc.getPitch() + "," + loc.getWorld().getName();
        yml.set(path, data);
        save();
    }

    /**
     * Save a location for arena use
     */
    public void saveArenaLoc(String path, Location loc) {
        String data = stringLocationArenaFormat(loc);
        if (isArenaNpcLocationPath(path)) data += "," + loc.getYaw() + "," + loc.getPitch();
        yml.set(path, data);
        save();
    }

    /**
     * Get a general location
     * Use {@link #getArenaLoc(String)} for locations stored using {@link #saveArenaLoc(String, Location)}
     */
    public Location getConfigLoc(String path) {
        String d = yml.getString(path);
        if (d == null) return null;
        String[] data = d.replace("[", "").replace("]", "").split(",");
        return new Location(Bukkit.getWorld(data[5]), Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]), Float.parseFloat(data[3]), Float.parseFloat(data[4]));
    }

    /**
     * Get a location for arena use
     * Use {@link #getConfigLoc(String)} (String)} for locations stored using {@link #saveConfigLoc(String, Location)} (String, Location)}
     */
    public Location getArenaLoc(String path) {
        String d = yml.getString(path);
        if (d == null) return null;
        if (isArenaNpcLocationPath(path)) {
            String[] data = normalizeArenaNpcLocationString(d).split(",");
            return new Location(Bukkit.getWorld(name), Double.parseDouble(data[0]), Double.parseDouble(data[1]),
                    Double.parseDouble(data[2]), Float.parseFloat(data[3]), Float.parseFloat(data[4]));
        }
        return convertStringToArenaLocation(d);
    }

    /** 竞技场位置仅使用 x,y,z；兼容旧版五段格式，忽略旧朝向。 */
    public Location convertStringToArenaLocation(String string) {
        String[] data = normalizeArenaLocationString(string).split(",");
        return new Location(Bukkit.getWorld(name), Double.parseDouble(data[0]),
                Double.parseDouble(data[1]), Double.parseDouble(data[2]));
    }

    /** 保留原始坐标精度，不改变方块中心或高度。 */
    public static String normalizeArenaLocationString(String value) {
        if (value == null) throw new IllegalArgumentException("竞技场坐标不能为空");
        String[] data = value.trim().replace("[", "").replace("]", "").split(",", -1);
        if (data.length != 3 && data.length != 5)
            throw new IllegalArgumentException("竞技场坐标需要 x,y,z 或旧版 x,y,z,yaw,pitch: " + value);
        for (int i = 0; i < 3; i++) {
            data[i] = data[i].trim();
            if (!Double.isFinite(Double.parseDouble(data[i])))
                throw new IllegalArgumentException("竞技场坐标必须为有限数值: " + value);
        }
        return data[0] + "," + data[1] + "," + data[2];
    }

    /** 商店与升级 NPC 的朝向由竞技场配置控制，三段旧值兼容为零朝向。 */
    public static boolean isArenaNpcLocationPath(String path) {
        return path.matches("(?i)Team\\.[^.]+\\.(Shop|Upgrade)");
    }

    public static String normalizeArenaNpcLocationString(String value) {
        String xyz = normalizeArenaLocationString(value);
        String[] data = value.trim().replace("[", "").replace("]", "").split(",", -1);
        if (data.length == 3) return xyz + ",0.0,0.0";
        for (int i = 3; i < 5; i++) {
            data[i] = data[i].trim();
            if (!Float.isFinite(Float.parseFloat(data[i])))
                throw new IllegalArgumentException("NPC 朝向必须为有限数值: " + value);
        }
        return xyz + "," + data[3] + "," + data[4];
    }

    /** 仅匹配插件管理的位置键，避免改动管理员自定义文本。 */
    public static boolean isArenaLocationPath(String path) {
        return path.matches("(?i)(spectator-loc|waiting\\.(Loc|Pos[^.]+)|generator\\.(Diamond|Emerald)|Team\\.[^.]+\\.(Spawn|Bed|Shop|Upgrade|Iron|Gold|Diamond|Emerald|kill-drops-loc))");
    }

    /**
     * Get list of arena locations at given path
     */
    public List<Location> getArenaLocations(String path) {
        List<Location> l = new ArrayList<>();
        for (String s : yml.getStringList(path)) {
            Location loc = convertStringToArenaLocation(s);
            if (loc != null) {
                l.add(loc);
            }
        }
        return l;
    }

    /**
     * Set data to config
     */
    public void set(String path, Object value) {
        yml.set(path, value);
        save();
    }

    /**
     * Get yml instance
     */
    public YamlConfiguration getYml() {
        return yml;
    }

    /**
     * Save config changes to file
     */
    public void save() {
        try {
            yml.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get list of strings at given path
     *
     * @return a list of string with colors translated
     */
    public List<String> getList(String path) {
        return yml.getStringList(path).stream().map(s -> s.replace("&", "§")).collect(Collectors.toList());
    }

    /**
     * Get boolean at given path
     */
    public boolean getBoolean(String path) {
        return yml.getBoolean(path);
    }

    /**
     * Get Integer at given path
     */
    public int getInt(String path) {
        return yml.getInt(path);
    }

    public double getDouble(String path) {
        return yml.getDouble(path);
    }


    /**
     * Get string at given path
     */
    public String getString(String path) {
        return yml.getString(path);
    }

    /**
     * Check if the config file was created for the first time
     * Can be used to add default values
     */
    public boolean isFirstTime() {
        return firstTime;
    }

    /**
     * Compare two arena locations
     * Return true if same location
     */
    public boolean compareArenaLoc(Location l1, Location l2) {
        return l1.getBlockX() == l2.getBlockX() && l1.getBlockZ() == l2.getBlockZ() && l1.getBlockY() == l2.getBlockY();
    }

    /**
     * Get config name
     */
    public String getName() {
        return name;
    }

    /**
     * Change internal name.
     */
    public void setName(String name) {
        this.name = name;
    }
}
