package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.language.MatchmakingMessages;
import com.andrei1058.bedwars.language.MatchQueryMessages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lists available commands and their argument shapes without requiring hover text. */
public final class CmdHelp extends SubCommand {
    public CmdHelp(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(0);
        showInList(true);
        setDisplayInfo(com.andrei1058.bedwars.commands.bedwars.MainCommand.createTC(
                "§6 ▪ §7/" + parent.getName() + " help", "/" + parent.getName() + " help",
                "§f列出可用命令、参数和简要说明。"));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        sender.sendMessage(MatchmakingMessages.text(sender, MatchmakingMessages.Key.HELP_HEADER));
        List<SubCommand> commands = new ArrayList<>(getParent().getSubCommands());
        commands.sort(Comparator.comparingInt(SubCommand::getPriority)
                .thenComparing(SubCommand::getSubCommandName, String.CASE_INSENSITIVE_ORDER));
        for (SubCommand command : commands) {
            if (command == this || !command.hasPermission(sender)) continue;
            if (command.isArenaSetupCommand()
                    && (!(sender instanceof Player)
                    || !SetupSession.isInSetupSession(((Player) sender).getUniqueId()))) continue;
            String name = command.getSubCommandName();
            String description = command instanceof CmdMatchQuery
                    ? MatchQueryMessages.description(sender, name) : description(name);
            sender.sendMessage("§e/" + getParent().getName() + " " + name + arguments(name)
                    + " §7— " + description);
        }
        sender.sendMessage(MatchmakingMessages.text(sender, MatchmakingMessages.Key.HELP_FOOTER));
        return true;
    }

    private static String arguments(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "match": return " [对局编号|UUID]";
            case "history": return " [玩家名|UUID|all] [页码]";
            case "record": return " [玩家名|UUID]";
            case "join": return " <竞技场|分组|random>";
            case "team": return " [invite|accept|decline|leave|squad|help]";
            case "setminplayers": case "setmaxinteam": return " <人数>";
            case "setuparena": return " <世界名>";
            case "setspawn": return " <队伍名>";
            case "createteam": return " <队伍名> <颜色>";
            case "clonearena": return " <源世界> <新竞技场名>";
            case "tp": return " <玩家>";
            case "start": case "forceStart": return " [debug]";
            default: return "";
        }
    }

    private static String description(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "help": return "列出当前有权限使用的命令。";
            case "cmds": return "查看玩家命令或设置会话中的地图设置进度。";
            case "match": return "查询当前或指定对局的逐局详情。";
            case "history": return "分页查看玩家历史；使用 all 查看全部对局。";
            case "record": return "查询已完成对局的累计战绩。";
            case "lang": return "列出语言或切换语言；对局中只允许列出。";
            case "setminplayers": return "设置全场最低开局人数。";
            case "setmaxinteam": return "设置每队最大人数。";
            case "listteams": return "列出地图已配置的队伍。";
            case "start": case "forcestart": return "启动当前竞技场倒计时。";
            case "stopgames": return "暂停本服新对局开局。";
            case "resumegames": return "恢复本服新对局开局。";
            case "join": return "加入指定或随机竞技场。";
            case "leave": return "退出当前对局。";
            case "team": return "管理当前等待队伍。";
            case "setuparena": return "开始创建或编辑竞技场。";
            case "save": return "校验并保存竞技场设置。";
            case "arenalist": return "列出已加载竞技场。";
            case "reload": return "重载插件配置。";
            default: return "管理起床战争竞技场或查看玩家信息。";
        }
    }

    @Override public List<String> getTabComplete() { return new ArrayList<>(); }
    @Override public boolean canSee(CommandSender sender, BedWars api) { return hasPermission(sender); }
}
