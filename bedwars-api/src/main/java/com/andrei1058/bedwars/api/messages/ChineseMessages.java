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

package com.andrei1058.bedwars.api.messages;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChineseMessages extends ConfigManager {

    private String prefix = "";
    private static String prefixStatic = "";
    private static ChineseMessages instance;
    private String serverIp;

    public ChineseMessages(Plugin plugin) {
        super(plugin, "messages_zh_cn", plugin.getDataFolder().getPath() + "/Languages");
        migrateLegacyTowerShopItem(getYml());

        // replace old placeholders
        List<String> oldMsg = getYml().getStringList(Messages.GAME_END_TOP_PLAYER_CHAT);
        if (!oldMsg.isEmpty()) {
            String[] oldTop1 = new String[]{"{firstName}", "{secondName}", "{thirdName}"};
            String[] oldTop2 = new String[]{"{firstKills}", "{secondKills}", "{thirdKills}"};

            List<String> newMsg = new ArrayList<>();
            for (String oldString : oldMsg) {
                for (String oldPlaceholder : oldTop1) {
                    oldString = oldString.replace(oldPlaceholder, "{topPlayerName}");
                }
                for (String oldPlaceholder : oldTop2) {
                    oldString = oldString.replace(oldPlaceholder, "{topValue}");
                }
                newMsg.add(oldString);
            }

            getYml().set(Messages.GAME_END_TOP_PLAYER_CHAT, newMsg);
        }

        if (null != getYml().get("scoreboard")) {
            for (String group : getYml().getConfigurationSection("scoreboard").getKeys(false)) {
                if (group.equalsIgnoreCase("lobby")) {
                    relocate("scoreboard." + group, "sidebar." + group);
                } else {
                    Map<String, String[]> stages = new HashMap<>();
                    stages.put("waiting", new String[]{Messages.SCOREBOARD_DEFAULT_WAITING, Messages.SCOREBOARD_DEFAULT_WAITING_SPEC});
                    stages.put("starting", new String[]{Messages.SCOREBOARD_DEFAULT_STARTING, Messages.SCOREBOARD_DEFAULT_STARTING_SPEC});

                    for (Map.Entry<String, String[]> stage : stages.entrySet()) {
                        if (exists("scoreboard." + group + "." + stage.getKey() + ".player")) {
                            relocate("scoreboard." + group + "." + stage.getKey() + ".player", stage.getValue()[0].replace("Default", group));
                        } else {
                            relocate("scoreboard." + group + "." + stage.getKey(), stage.getValue()[0].replace("Default", group));
                        }
                        if (exists("scoreboard." + group + "." + stage.getKey() + ".spectator")) {
                            relocate("scoreboard." + group + "." + stage.getKey() + ".spectator", stage.getValue()[1].replace("Default", group));
                        }
                    }
                    if (exists("scoreboard." + group + ".playing.alive")) {
                        relocate("scoreboard." + group + ".playing.alive", Messages.SCOREBOARD_DEFAULT_PLAYING.replace("Default", group));
                        relocate("scoreboard." + group + ".playing.spectator", Messages.SCOREBOARD_DEFAULT_PLAYING.replace("Default", group));
                    } else {
                        relocate("scoreboard." + group + ".playing", Messages.SCOREBOARD_DEFAULT_PLAYING.replace("Default", group));
                    }
                }
            }
            getYml().set("scoreboard", null);
        }

        instance = this;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setPrefixStatic(String prefix) {
        prefixStatic = prefix;
    }

    public static List<String> getScoreboard(Player player, String path, String alternative) {
        ChineseMessages language = get();
        if (language.exists(path)) {
            return language.l(path);
        } else {
            if (path.split("\\.").length == 3) {
                String[] sp = path.split("\\.");
                String path2 = sp[1];
                path2 = String.valueOf(path2.charAt(0)).toUpperCase() + path2.substring(1).toLowerCase();
                path2 = sp[0] + "." + path2 + "." + sp[2];
                if (language.exists(path2)) {
                    return language.l(path);
                } else if (language.exists(sp[0] + "." + sp[1].toUpperCase() + "." + sp[2])) {
                    return language.l(sp[0] + "." + sp[1].toUpperCase() + "." + sp[2]);
                }
            }
        }
        return language.l(alternative);
    }

    public static String getMsg(Player player, String path) {
        if (player == null) {
            return get().m(path);
        }
        return get()
                .m(path).replace("{prefix}", (prefixStatic == null ? "" : prefixStatic));
    }

    public boolean exists(String path) {
        return getYml().get(path) != null;
    }

    public static List<String> getList(@NotNull Player player, String path) {
        return get().l(path);
    }

    @SuppressWarnings("unused")
    public void relocate(String from, String to) {
        Object fromData = getYml().get(from);
        if (null != fromData) {
            this.getYml().set(to, fromData);
            this.getYml().set(from, null);
        }
    }

    public static void saveIfNotExists(String path, Object data) {
        ChineseMessages l = get();
        if (l.getYml().get(path) == null) {
            l.set(path, data);
        }

    }

    public String m(String path) {
        String message = getYml().getString(path);
        if (message == null) {
            System.err.println("缺少中文消息：" + path);
            message = "缺少中文消息";
        }
        if (null == serverIp) {
            BedWars api = Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider();
            if (null != api.getConfigs().getMainConfig()) {
                serverIp = api.getConfigs().getMainConfig().
                        getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message
                .replace("{prefix}", (prefix == null ? "" : prefix))
                .replace("{serverIp}", serverIp == null ? "" : serverIp)
        );
    }

    public List<String> l(String path) {
        List<String> result = new ArrayList<>();
        List<String> lines = getYml().getStringList(path);
        if (lines == null) {
            System.err.println("缺少中文消息列表：" + path);
            lines = Collections.emptyList();
        }
        for (String line : lines) {
            result.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return result;
    }

    public static void setupCustomStatsMessages() {
        BedWars api = Bukkit.getServer().getServicesManager().getRegistration(BedWars.class).getProvider();
        ChineseMessages l = get();
        if (l == null) return;
        if (l.getYml() == null) return;
        /* save messages for stats gui items if custom items added */
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_STATS_PATH) == null)
            return;
        for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_STATS_PATH).getKeys(false)) {
            if (ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE.contains(item)) continue;
            if (l.getYml().getDefaults() == null || !l.getYml().getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-name"))
                l.getYml().addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-name", "未设置名称");
            if (l.getYml().getDefaults() == null || !l.getYml().getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-lore"))
                l.getYml().addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-lore", Collections.singletonList("未设置说明"));
        }
        l.save();

    }

    @SuppressWarnings("WeakerAccess")
    public void addDefaultStatsMsg(@NotNull YamlConfiguration yml, String path, String name, String... lore) {
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-name"))
            yml.addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-name", name);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-lore"))
            yml.addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-lore", lore);
    }

    public static void addDefaultMessagesCommandItems(ChineseMessages language) {
        if (language == null) return;
        YamlConfiguration yml = language.getYml();
        if (yml == null) return;
        BedWars api = Bukkit.getServer().getServicesManager().getRegistration(BedWars.class).getProvider();
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH) != null) {
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH).getKeys(false)) {
                if (item.isEmpty()) continue;
                String p1 = Messages.GENERAL_CONFIGURATION_LOBBY_ITEMS_NAME.replace("%path%", item);
                String p2 = Messages.GENERAL_CONFIGURATION_LOBBY_ITEMS_LORE.replace("%path%", item);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p1, "&c未设置名称：&f" + p1);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p2, Arrays.asList("&c未设置说明：", " &f" + p2));
            }
        }
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH) != null) {
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH).getKeys(false)) {
                if (item.isEmpty()) continue;
                String p1 = Messages.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_NAME.replace("%path%", item);
                String p2 = Messages.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_LORE.replace("%path%", item);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p1, "&c未设置名称：&f" + p1);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p2, Arrays.asList("&c未设置说明：", " &f" + p2));
            }
        }
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH) != null) {
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH).getKeys(false)) {
                if (item.isEmpty()) continue;
                String p1 = Messages.GENERAL_CONFIGURATION_WAITING_ITEMS_NAME.replace("%path%", item);
                String p2 = Messages.GENERAL_CONFIGURATION_WAITING_ITEMS_LORE.replace("%path%", item);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p1, "&c未设置名称：&f" + p1);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p2, Arrays.asList("&c未设置说明：", " &f" + p2));
            }
        }
        yml.options().copyDefaults(true);
        language.save();
    }

    @SuppressWarnings("DuplicateExpressions")
    public void setupUnSetCategories() {
        BedWars api = Bukkit.getServer().getServicesManager().getRegistration(BedWars.class).getProvider();
        for (String s : api.getConfigs().getShopConfig().getYml().getConfigurationSection("").getKeys(false)) {
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SETTINGS_PATH)) continue;
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SPECIALS_PATH)) continue;
            if (s.equals(ConfigPath.SHOP_QUICK_DEFAULTS_PATH)) continue;
            if (!exists(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", s))) {
                set(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", s), "&8未设置名称");
            }
            if (!exists(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", s))) {
                set(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", s), "&8未设置名称");
            }
            if (!exists(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", s))) {
                set(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", s), Collections.singletonList("&8未设置说明"));
            }
            if (api.getConfigs().getShopConfig().getYml().get(s + ConfigPath.SHOP_CATEGORY_CONTENT_PATH) == null)
                continue;
            for (String c : api.getConfigs().getShopConfig().getYml().getConfigurationSection(s + ConfigPath.SHOP_CATEGORY_CONTENT_PATH).getKeys(false)) {
                if (!exists(Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", s).replace("%content%", c))) {
                    set(Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", s).replace("%content%", c), "&8未设置名称");
                }
                if (!exists(Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", s).replace("%content%", c))) {
                    set(Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", s).replace("%content%", c), Collections.singletonList("&8未设置说明"));
                }
            }
        }
    }

    public static void addCategoryMessages(@NotNull YamlConfiguration yml, String categoryName, String invName, String itemName, List<String> itemLore) {
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", categoryName)))
            yml.addDefault(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", categoryName), invName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", categoryName)))
            yml.addDefault(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", categoryName), itemName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", categoryName)))
            yml.addDefault(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", categoryName), itemLore);
    }

    public static void addContentMessages(@NotNull YamlConfiguration yml, String contentName, String categoryName, String itemName, List<String> itemLore) {
        final String path1 = Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", categoryName).replace("%content%", contentName),
                path2 = Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", categoryName).replace("%content%", contentName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(path1)) yml.addDefault(path1, itemName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(path2)) yml.addDefault(path2, itemLore);
    }

    private static boolean isGeneratedShopPlaceholder(Object value, String placeholder) {
        if (value == null) return true;
        if (value instanceof String text)
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text)).trim().equalsIgnoreCase(placeholder);
        return value instanceof List<?> values && values.size() == 1
                && isGeneratedShopPlaceholder(values.get(0), placeholder);
    }

    public static void migrateLegacyTowerShopItem(@NotNull YamlConfiguration yml) {
        String oldName = Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", "Compact Pop-up Tower");
        String oldLore = Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", "Compact Pop-up Tower");
        String newName = Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", "tower");
        String newLore = Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", "tower");
        if (isGeneratedShopPlaceholder(yml.get(newName), "name not set"))
            yml.set(newName, isGeneratedShopPlaceholder(yml.get(oldName), "name not set") ? null : yml.get(oldName));
        if (isGeneratedShopPlaceholder(yml.get(newLore), "未设置说明"))
            yml.set(newLore, isGeneratedShopPlaceholder(yml.get(oldLore), "未设置说明") ? null : yml.get(oldLore));
        yml.set(oldName, null);
        yml.set(oldLore, null);
    }

    public static String @NotNull [] getCountDownTitle(@NotNull ChineseMessages playerLang, int second) {
        String[] result = new String[2];
        result[0] = ChatColor.translateAlternateColorCodes('&', playerLang.getYml().get(Messages.ARENA_STATUS_START_COUNTDOWN_TITLE + "-" + second, playerLang.getString(Messages.ARENA_STATUS_START_COUNTDOWN_TITLE)).toString().replace("{second}", String.valueOf(second)));
        if (result[0].isEmpty()) {
            result[0] = " ";
        }
        result[1] = ChatColor.translateAlternateColorCodes('&', playerLang.getYml().get(Messages.ARENA_STATUS_START_COUNTDOWN_SUB_TITLE + "-" + second, playerLang.getString(Messages.ARENA_STATUS_START_COUNTDOWN_SUB_TITLE)).toString().replace("{second}", String.valueOf(second)));
        if (result[1].isEmpty()) {
            result[1] = " ";
        }
        return result;
    }

    public static ChineseMessages get() {
        return instance;
    }
}
