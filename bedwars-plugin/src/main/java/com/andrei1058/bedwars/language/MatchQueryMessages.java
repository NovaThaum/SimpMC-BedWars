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
        DISABLED("cmd-match-query-disabled", "&c对局统计未启用，请联系管理员检查统计设置与数据库连接。"),
        PENDING("cmd-match-query-pending", "&e上一条战绩查询仍在处理中，请稍候。"),
        FAILED("cmd-match-query-failed", "&c暂时无法读取战绩，请稍后重试；详细原因已写入服务器日志。"),
        MATCH_USAGE("cmd-match-query-match-usage", "&e用法：/bw match [对局编号或 UUID]"),
        MATCH_DESCRIPTION("cmd-match-query-match-description", "&f查看当前对局，或按编号、UUID 查询逐局战绩。"),
        HISTORY_USAGE("cmd-match-query-history-usage", "&e用法：/bw history [玩家名或 UUID] [页码]；/bw history all [页码]"),
        HISTORY_DESCRIPTION("cmd-match-query-history-description", "&f分页查看玩家对局历史；可使用 all 查看全部记录。"),
        RECORD_USAGE("cmd-match-query-record-usage", "&e用法：/bw record [玩家名或 UUID]（支持离线玩家）"),
        RECORD_DESCRIPTION("cmd-match-query-record-description", "&f查看玩家所有已完成对局的累计战绩。"),
        NO_CURRENT_MATCH("cmd-match-query-no-current", "&e你当前没有已开始的对局。"),
        MATCH_NOT_FOUND("cmd-match-query-not-found", "&e没有找到这场对局。"),
        NUMBER_PENDING("cmd-match-query-number-pending", "&e对局刚开始，编号和首次战绩正在保存，请稍后查询。"),
        RUNNING_SNAPSHOT("cmd-match-query-running-snapshot", "&7以下为最近一次保存的战绩，结束后会保存最终结果。"),
        NO_PLAYERS("cmd-match-query-no-players", "&7暂无已保存的参赛玩家战绩。"),
        INVALID_IDENTIFIER("cmd-match-query-invalid-id", "&e请输入有效的正整数对局编号或完整 UUID。"),
        CONSOLE_MATCH_ID("cmd-match-query-console-id", "&e控制台请指定对局编号或 UUID。"),
        TOTALS_HEADER("cmd-match-query-totals-header", "&6玩家累计战绩 &7{player}"),
        TOTALS_MATCHES("cmd-match-query-totals-matches", "&7已完成对局：&f{count}"),
        KD_EXPLANATION("cmd-match-query-kd-explanation", "&7K/D = 普通击杀 ÷ 全部死亡；死亡为 0 时等于普通击杀。"),
        HISTORY_ALL("cmd-match-query-history-all", "全部记录"),
        HISTORY_HEADER("cmd-match-query-history-header", "&6对局历史 &7{target} · 第 {page} 页"),
        HISTORY_ROW("cmd-match-query-history-row", "&e#{number} &f{arena} &7{status} · {time}"),
        HISTORY_EMPTY("cmd-match-query-history-empty", "&7这一页没有对局记录。"),
        HISTORY_NEXT("cmd-match-query-history-next", "&7使用 /bw match <编号> 查看单局战绩；/bw history {target} {page} 查看更早记录。"),
        INVALID_PAGE("cmd-match-query-invalid-page", "&e页码必须是有效的正整数。"),
        CONSOLE_HISTORY_TARGET("cmd-match-query-console-target", "&e控制台请指定玩家名或 UUID，或使用 /bw history all。"),
        INVALID_PLAYER_UUID("cmd-match-query-invalid-player-uuid", "&e请输入完整的玩家 UUID。"),
        PLAYER_NOT_FOUND("cmd-match-query-player-not-found", "&e历史中未找到该玩家名，请检查名字或使用完整玩家 UUID。"),
        MATCH_HEADER("cmd-match-query-header", "&6对局 #{number} &7{status}"),
        MATCH_NUMBER_PENDING("cmd-match-query-number-label-pending", "待分配"),
        MATCH_UUID("cmd-match-query-uuid", "&7UUID：&f{uuid}"),
        MATCH_MAP_GROUP("cmd-match-query-map-group", "&7地图：&f{map} &7模式：&f{group}"),
        MATCH_TIMES("cmd-match-query-times", "&7开始：&f{start} &7结束：&f{end} &7（北京时间）"),
        MATCH_NOT_ENDED("cmd-match-query-not-ended", "尚未结束"),
        COUNTERS("cmd-match-query-counters", "&7击杀 &f{kills} &7最终击杀 &f{finalKills} &7破坏床 &f{beds} &7死亡 &f{deaths} &7K/D &f{kd}"),
        PLAYER_ROW("cmd-match-query-player-row", "&f{player} &7[{outcome}] {counters}"),
        STATUS_FINISHED("cmd-match-query-status-finished", "已完成"),
        STATUS_ABORTED("cmd-match-query-status-aborted", "已中止"),
        STATUS_RUNNING("cmd-match-query-status-running", "进行中"),
        OUTCOME_WIN("cmd-match-query-outcome-win", "胜利"),
        OUTCOME_LOSS("cmd-match-query-outcome-loss", "失败"),
        OUTCOME_ABANDONED("cmd-match-query-outcome-abandoned", "中途离开"),
        OUTCOME_DISCONNECTED("cmd-match-query-outcome-disconnected", "掉线"),
        OUTCOME_UNSETTLED("cmd-match-query-outcome-unsettled", "未结算"),
        LANG_PLAYER_ONLY("cmd-lang-player-only", "&c只有玩家可以使用语言切换命令。"),
        LANG_CHANGE_CANCELLED("cmd-lang-change-cancelled", "&c语言切换被其他插件取消。");

        private final String path;
        private final String chinese;

        Key(String path, String chinese) {
            this.path = path;
            this.chinese = chinese;
        }
    }

    public static void install(Language language) {
        for (Key key : Key.values()) {
            language.getYml().addDefault(key.path, key.chinese);
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
        for (Key key : Key.values()) templates.put(key, key.chinese);
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
