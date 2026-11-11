package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class RecallMessages {
    private RecallMessages() { }
    public enum Key {
        COUNTDOWN("recall-scroll.countdown", "&b回城倒计时：&f{time}&b 秒，移动或受伤会取消。"),
        ALREADY_CHANNELING("recall-scroll.already-channeling", "&e回城卷轴正在引导中。"),
        UNAVAILABLE("recall-scroll.unavailable", "&c当前无法使用回城卷轴。"),
        CANCELLED("recall-scroll.cancelled", "&c回城引导已取消。"),
        COMPLETED("recall-scroll.completed", "&a已返回队伍出生点。"),
        FAILED("recall-scroll.failed", "&c回城失败，卷轴未消耗。");
        final String path; final String fallback;
        Key(String path, String fallback) { this.path = path; this.fallback = fallback; }
    }
    public static void install(Language language) {
        for (Key key : Key.values()) language.getYml().addDefault(key.path, key.fallback);
        language.save();
    }
    public static String text(Player player, Key key) {
        Language language = Language.getPlayerLanguage(player);
        return ChatColor.translateAlternateColorCodes('&', language == null ? key.fallback
                : language.getYml().getString(key.path, key.fallback));
    }
}
