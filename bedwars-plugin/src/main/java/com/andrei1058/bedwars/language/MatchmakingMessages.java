package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 开局控制提示通过现有玩家语言文件提供覆盖入口。 */
public final class MatchmakingMessages {
    private MatchmakingMessages() { }
    public enum Key {
        MATCHES_BLOCKED("cmd-matchmaking-blocked", "&c管理员已暂停新对局。"),
        MATCHES_ALREADY_BLOCKED("cmd-matchmaking-already-blocked", "&e新对局已经处于暂停状态。"),
        MATCHES_RESUMED("cmd-matchmaking-resumed", "&a已恢复新对局。"),
        MATCHES_ALREADY_RESUMED("cmd-matchmaking-already-resumed", "&e新对局已经处于开启状态。"),
        NO_PERMISSION("cmd-matchmaking-no-permission", "&c你没有管理新对局开局的权限。");
        private final String path;
        private final String fallback;
        Key(String path, String fallback) { this.path = path; this.fallback = fallback; }
    }
    public static void install(Language language) {
        for (Key key : Key.values()) language.getYml().addDefault(key.path, key.fallback);
        language.save();
    }
    public static String text(CommandSender sender, Key key) {
        Language language = sender instanceof Player
                ? Language.getPlayerLanguage((Player) sender) : Language.getDefaultLanguage();
        String template = language == null ? key.fallback : language.getYml().getString(key.path, key.fallback);
        return ChatColor.translateAlternateColorCodes('&', template);
    }
}
