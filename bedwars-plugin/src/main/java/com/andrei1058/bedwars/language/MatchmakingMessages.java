package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/** 开局控制提示通过现有玩家语言文件提供覆盖入口。 */
public final class MatchmakingMessages {
    private MatchmakingMessages() { }
    public enum Key {
        MATCHES_BLOCKED("cmd-matchmaking-blocked", "&c管理员已暂停新对局。"),
        MATCHES_ALREADY_BLOCKED("cmd-matchmaking-already-blocked", "&e新对局已经处于暂停状态。"),
        MATCHES_RESUMED("cmd-matchmaking-resumed", "&a已恢复新对局。"),
        MATCHES_ALREADY_RESUMED("cmd-matchmaking-already-resumed", "&e新对局已经处于开启状态。"),
        NO_PERMISSION("cmd-matchmaking-no-permission", "&c你没有管理新对局开局的权限。"),
        LIST_HEADER("cmd-list-header", "&6ONLINE &8» &fThere are {online} players online, with a maximum capacity of {max} players.", "&6在线玩家 &8» &f当前有 {online} 位玩家在线，可容纳最大在线人数为 {max} 位玩家。"),
        LIST_LOBBY("cmd-list-lobby", "&aLOBBY &8» &f{players}", "&a大厅 &8» &f{players}"),
        LIST_ARENA("cmd-list-arena", "&bARENA &8[&f{arena}&8] &8» &f{players}", "&b竞技场 &8[&f{arena}&8] &8» &f{players}"),
        LIST_EMPTY("cmd-list-empty", "&7None", "&7无人"),
        HELP_HEADER("cmd-help-header", "&6SimpMC-BedWars 命令帮助 §7<> 必填；[] 可选。"),
        HELP_FOOTER("cmd-help-footer", "&7使用 /bw help 查看全部命令；竞技场设置命令需先进入 /bw setupArena 设置会话。"),
        LANG_PLAYER_ONLY("cmd-lang-player-only", "&c只有玩家可以使用语言切换命令。"),
        LANG_CHANGE_CANCELLED("cmd-lang-change-cancelled", "&c语言切换被其他插件取消。");
        private final String path;
        private final String fallback;
        private final String english;
        private final String chinese;
        Key(String path, String fallback) { this(path, fallback, fallback); }
        Key(String path, String english, String chinese) {
            this.path = path;
            this.fallback = chinese;
            this.english = english;
            this.chinese = chinese;
        }
    }
    public static void install(Language language) {
        String iso = language.getIso();
        boolean chinese = iso != null && iso.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        for (Key key : Key.values()) language.getYml().addDefault(key.path, chinese ? key.chinese : key.english);
        language.save();
    }
    public static String text(CommandSender sender, Key key) {
        Language language = sender instanceof Player
                ? Language.getPlayerLanguage((Player) sender) : Language.getDefaultLanguage();
        String template = language == null ? key.fallback : language.getYml().getString(key.path, key.fallback);
        return ChatColor.translateAlternateColorCodes('&', template);
    }

    public static Snapshot forSender(CommandSender sender) {
        Language language = sender instanceof Player
                ? Language.getPlayerLanguage((Player) sender) : Language.getDefaultLanguage();
        EnumMap<Key, String> values = new EnumMap<>(Key.class);
        for (Key key : Key.values()) {
            values.put(key, language == null ? key.fallback : language.m(key.path));
        }
        return new Snapshot(values);
    }

    public static final class Snapshot {
        private final Map<Key, String> values;
        private Snapshot(Map<Key, String> values) { this.values = new EnumMap<>(values); }
        public String text(Key key, String... replacements) {
            String text = values.get(key);
            for (int index = 0; index < replacements.length; index += 2) {
                text = text.replace(replacements[index], replacements[index + 1]);
            }
            return text;
        }
    }

    public static Snapshot defaults(boolean chinese) {
        EnumMap<Key, String> values = new EnumMap<>(Key.class);
        for (Key key : Key.values()) values.put(key, chinese ? key.chinese : key.english);
        return new Snapshot(values);
    }
}
