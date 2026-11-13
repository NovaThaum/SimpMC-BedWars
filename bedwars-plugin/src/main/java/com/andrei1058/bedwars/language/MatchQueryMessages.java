package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Built-in defaults for persisted match-query commands. */
public final class MatchQueryMessages {
    private MatchQueryMessages() { }

    public enum Key {
        DISABLED("cmd-match-query-disabled", "&cMatch statistics are unavailable. Ask an administrator to check the database.", "&c对局统计未启用，请联系管理员检查统计设置与数据库连接。"),
        PENDING("cmd-match-query-pending", "&eYour previous match query is still running. Please wait.", "&e上一条战绩查询仍在处理中，请稍候。"),
        FAILED("cmd-match-query-failed", "&cCould not read match statistics. Please try again later; details are in the server log.", "&c暂时无法读取战绩，请稍后重试；详细原因已写入服务器日志。"),
        MATCH_USAGE("cmd-match-query-match-usage", "&eUsage: /bw match [match number or UUID]", "&e用法：/bw match [对局编号或 UUID]"),
        MATCH_DESCRIPTION("cmd-match-query-match-description", "&fView your current match or query match results by number or UUID.", "&f查看当前对局，或按编号、UUID 查询逐局战绩。"),
        HISTORY_USAGE("cmd-match-query-history-usage", "&eUsage: /bw history [player name or UUID] [page]; /bw history all [page]", "&e用法：/bw history [玩家名或 UUID] [页码]；/bw history all [页码]"),
        HISTORY_DESCRIPTION("cmd-match-query-history-description", "&fBrowse player match history by page, or use all for every match.", "&f分页查看玩家对局历史；可使用 all 查看全部记录。"),
        RECORD_USAGE("cmd-match-query-record-usage", "&eUsage: /bw record [player name or UUID]", "&e用法：/bw record [玩家名或 UUID]（支持离线玩家）"),
        RECORD_DESCRIPTION("cmd-match-query-record-description", "&fView a player's totals across finished matches.", "&f查看玩家所有已完成对局的累计战绩。"),
        NO_CURRENT_MATCH("cmd-match-query-no-current", "&eYou are not in a started match.", "&e你当前没有已开始的对局。"),
        MATCH_NOT_FOUND("cmd-match-query-not-found", "&eThis match was not found.", "&e没有找到这场对局。"),
        NUMBER_PENDING("cmd-match-query-number-pending", "&eThe match just started. Its number and first statistics are being saved; try again shortly.", "&e对局刚开始，编号和首次战绩正在保存，请稍后查询。"),
        RUNNING_SNAPSHOT("cmd-match-query-running-snapshot", "&7These are the last saved statistics. Final results are saved after the match.", "&7以下为最近一次保存的战绩，结束后会保存最终结果。"),
        NO_PLAYERS("cmd-match-query-no-players", "&7No player results have been saved yet.", "&7暂无已保存的参赛玩家战绩。"),
        INVALID_IDENTIFIER("cmd-match-query-invalid-id", "&eEnter a positive match number or a full UUID.", "&e请输入有效的正整数对局编号或完整 UUID。"),
        CONSOLE_MATCH_ID("cmd-match-query-console-id", "&eThe console must specify a match number or UUID.", "&e控制台请指定对局编号或 UUID。"),
        TOTALS_HEADER("cmd-match-query-totals-header", "&6Player totals &7{player}", "&6玩家累计战绩 &7{player}"),
        TOTALS_MATCHES("cmd-match-query-totals-matches", "&7Finished matches: &f{count}", "&7已完成对局：&f{count}"),
        KD_EXPLANATION("cmd-match-query-kd-explanation", "&7K/D = regular kills / all deaths; with zero deaths it equals regular kills.", "&7K/D = 普通击杀 ÷ 全部死亡；死亡为 0 时等于普通击杀。"),
        HISTORY_ALL("cmd-match-query-history-all", "All matches", "全部记录"),
        HISTORY_HEADER("cmd-match-query-history-header", "&6Match history &7{target} · Page {page}", "&6对局历史 &7{target} · 第 {page} 页"),
        HISTORY_ROW("cmd-match-query-history-row", "&e#{number} &f{arena} &7{status} · {time}", "&e#{number} &f{arena} &7{status} · {time}"),
        HISTORY_EMPTY("cmd-match-query-history-empty", "&7There are no matches on this page.", "&7这一页没有对局记录。"),
        HISTORY_NEXT("cmd-match-query-history-next", "&7Use /bw match <number> for details; /bw history {target} {page} for older matches.", "&7使用 /bw match <编号> 查看单局战绩；/bw history {target} {page} 查看更早记录。"),
        INVALID_PAGE("cmd-match-query-invalid-page", "&eThe page must be a valid positive integer.", "&e页码必须是有效的正整数。"),
        CONSOLE_HISTORY_TARGET("cmd-match-query-console-target", "&eThe console must specify a player name or UUID, or use /bw history all.", "&e控制台请指定玩家名或 UUID，或使用 /bw history all。"),
        INVALID_PLAYER_UUID("cmd-match-query-invalid-player-uuid", "&eEnter the player's full UUID.", "&e请输入完整的玩家 UUID。"),
        PLAYER_NOT_FOUND("cmd-match-query-player-not-found", "&eThis player name was not found in match history. Check the name or use a full UUID.", "&e历史中未找到该玩家名，请检查名字或使用完整玩家 UUID。"),
        MATCH_HEADER("cmd-match-query-header", "&6Match #{number} &7{status}", "&6对局 #{number} &7{status}"),
        MATCH_NUMBER_PENDING("cmd-match-query-number-label-pending", "pending", "待分配"),
        MATCH_UUID("cmd-match-query-uuid", "&7UUID: &f{uuid}", "&7UUID：&f{uuid}"),
        MATCH_MAP_GROUP("cmd-match-query-map-group", "&7Map: &f{map} &7Mode: &f{group}", "&7地图：&f{map} &7模式：&f{group}"),
        MATCH_TIMES("cmd-match-query-times", "&7Start: &f{start} &7End: &f{end} &7(Beijing time)", "&7开始：&f{start} &7结束：&f{end} &7（北京时间）"),
        MATCH_NOT_ENDED("cmd-match-query-not-ended", "not ended", "尚未结束"),
        COUNTERS("cmd-match-query-counters", "&7Kills &f{kills} &7Final kills &f{finalKills} &7Beds &f{beds} &7Deaths &f{deaths} &7K/D &f{kd}", "&7击杀 &f{kills} &7最终击杀 &f{finalKills} &7破坏床 &f{beds} &7死亡 &f{deaths} &7K/D &f{kd}"),
        PLAYER_ROW("cmd-match-query-player-row", "&f{player} &7[{outcome}] {counters}", "&f{player} &7[{outcome}] {counters}"),
        STATUS_FINISHED("cmd-match-query-status-finished", "finished", "已完成"),
        STATUS_ABORTED("cmd-match-query-status-aborted", "aborted", "已中止"),
        STATUS_RUNNING("cmd-match-query-status-running", "running", "进行中"),
        OUTCOME_WIN("cmd-match-query-outcome-win", "win", "胜利"),
        OUTCOME_LOSS("cmd-match-query-outcome-loss", "loss", "失败"),
        OUTCOME_ABANDONED("cmd-match-query-outcome-abandoned", "left early", "中途离开"),
        OUTCOME_DISCONNECTED("cmd-match-query-outcome-disconnected", "disconnected", "掉线"),
        OUTCOME_UNSETTLED("cmd-match-query-outcome-unsettled", "unsettled", "未结算"),
        LANG_PLAYER_ONLY("cmd-lang-player-only", "&cOnly players can change their language.", "&c只有玩家可以使用语言切换命令。"),
        LANG_CHANGE_CANCELLED("cmd-lang-change-cancelled", "&cThe language change was cancelled by another plugin.", "&c语言切换被其他插件取消。");

        private final String path;
        private final String english;
        private final String chinese;

        Key(String path, String english, String chinese) {
            this.path = path;
            this.english = english;
            this.chinese = chinese;
        }
    }

    public static void install(Language language) {
        String iso = language.getIso();
        boolean chinese = iso != null && iso.toLowerCase(Locale.ROOT).startsWith("zh");
        for (Key key : Key.values()) {
            language.getYml().addDefault(key.path, chinese ? key.chinese : key.english);
        }
        language.getYml().options().copyDefaults(true);
        language.save();
    }

    public static Snapshot forSender(CommandSender sender) {
        Language language = sender instanceof Player
                ? Language.getPlayerLanguage((Player) sender) : Language.getDefaultLanguage();
        EnumMap<Key, String> templates = new EnumMap<>(Key.class);
        for (Key key : Key.values()) templates.put(key, language.m(key.path));
        return new Snapshot(templates);
    }

    public static String description(CommandSender sender, String command) {
        Key key;
        switch (command.toLowerCase(Locale.ROOT)) {
            case "match": key = Key.MATCH_DESCRIPTION; break;
            case "history": key = Key.HISTORY_DESCRIPTION; break;
            default: key = Key.RECORD_DESCRIPTION;
        }
        Language language = sender instanceof Player
                ? Language.getPlayerLanguage((Player) sender) : Language.getDefaultLanguage();
        return language == null ? key.chinese : language.m(key.path);
    }

    public static Snapshot defaults(boolean chinese) {
        EnumMap<Key, String> templates = new EnumMap<>(Key.class);
        for (Key key : Key.values()) templates.put(key, chinese ? key.chinese : key.english);
        return new Snapshot(templates);
    }

    public static final class Snapshot {
        private final Map<Key, String> templates;

        private Snapshot(Map<Key, String> templates) {
            this.templates = new EnumMap<>(templates);
        }

        public String text(Key key, String... replacements) {
            String line = templates.get(key);
            for (int index = 0; index < replacements.length; index += 2) {
                line = line.replace(replacements[index], replacements[index + 1]);
            }
            return line;
        }
    }
}
