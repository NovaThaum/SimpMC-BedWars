package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/** Language-file defaults for the pre-game squad interface and commands. */
public final class PreGameSquadMessages {
    private static final String ROOT = "pre-game-squad.";
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("not-pregame", "&c只能在竞技场开局前打开组队界面。");
        DEFAULTS.put("solo", "&e当前地图为单人队模式，无需选择队友。");
        DEFAULTS.put("title", "&8开局组队");
        DEFAULTS.put("member", "&a{name}{leader}");
        DEFAULTS.put("leader", " &6(队长)");
        DEFAULTS.put("member-lore", "&7当前固定队伍成员");
        DEFAULTS.put("info", "&b开局组队 &7({count}/{max})");
        DEFAULTS.put("lore-invite", "&7点击下方玩家头像发送邀请。");
        DEFAULTS.put("lore-pending", "&7绿色头像为收到的邀请。");
        DEFAULTS.put("lore-command", "&e/bw team invite <玩家>");
        DEFAULTS.put("accept", "&a接受 {name} 的邀请");
        DEFAULTS.put("accept-left", "&e左键接受");
        DEFAULTS.put("accept-right", "&c右键拒绝");
        DEFAULTS.put("invite", "&e邀请 {name}");
        DEFAULTS.put("invite-lore", "&a点击发送组队邀请");
        DEFAULTS.put("leave", "&c退出当前队伍");
        DEFAULTS.put("leave-lore", "&7退出后会作为单人参与自动分配");
        DEFAULTS.put("close", "&c关闭");
        DEFAULTS.put("left", "&a已退出固定队伍。");
        DEFAULTS.put("not-grouped", "&e你当前没有固定队伍。");
        DEFAULTS.put("target-missing", "&c找不到在线玩家：{name}");
        DEFAULTS.put("invite-sent", "&a已向 {name} 发送队伍邀请（30 秒有效）。");
        DEFAULTS.put("invited", "&e{name} 邀请你成为固定队友。输入 /bw team accept {inviter} 接受。");
        DEFAULTS.put("accepted", "&a已加入 {name} 的固定队伍。");
        DEFAULTS.put("accept-notice", "&a{name} 已加入你的固定队伍。");
        DEFAULTS.put("declined", "&e已拒绝邀请。");
        DEFAULTS.put("fail-not-pregame", "&c你和目标玩家必须在同一场尚未开始的竞技场。");
        DEFAULTS.put("fail-different-arena", "&c你和目标玩家不在同一场竞技场。");
        DEFAULTS.put("fail-self", "&c不能邀请自己。");
        DEFAULTS.put("fail-not-leader", "&c只有固定队伍队长可以邀请。");
        DEFAULTS.put("fail-target-grouped", "&c该玩家已经有固定队伍。");
        DEFAULTS.put("fail-full", "&c固定队伍已达到本地图每队人数上限。");
        DEFAULTS.put("fail-already-invited", "&e邀请已发送，请等待处理。");
        DEFAULTS.put("fail-expired", "&c邀请已过期。");
        DEFAULTS.put("fail-no-invite", "&c没有找到有效邀请。");
        DEFAULTS.put("fail-generic", "&c无法完成此操作。");
    }

    private PreGameSquadMessages() { }

    public static void installLanguageFallbacks() {
        for (Language language : Language.getLanguages()) {
            boolean changed = false;
            for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
                String path = ROOT + entry.getKey();
                if (!language.exists(path)) {
                    language.set(path, entry.getValue());
                    changed = true;
                }
            }
            if (changed) language.save();
        }
    }

    public static String get(Player player, String key) {
        return Language.getMsg(player, ROOT + key);
    }
}
