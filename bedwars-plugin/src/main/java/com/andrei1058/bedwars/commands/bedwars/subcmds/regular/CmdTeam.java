package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.PreGameSquad;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.team.PreGameSquadManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Commands for forming fixed teams before the arena starts. */
public final class CmdTeam extends SubCommand {
    private final PreGameSquadManager squads = PreGameSquadManager.getInstance();
    public CmdTeam(ParentCommand parent, String name) { super(parent, name); setPriority(12); }

    @Override public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        if (args.length == 0) {
            com.andrei1058.bedwars.arena.team.PreGameSquadGUI.getInstance().open(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) { help(player); return true; }
        String action = args[0].toLowerCase();
        if (action.equals("leave")) {
            PreGameSquad.Result result = squads.leave(player);
            player.sendMessage(result == PreGameSquad.Result.SUCCESS ? "§a已退出固定队伍。" : "§e你当前没有固定队伍。");
            return true;
        }
        if (args.length < 2) { help(player); return true; }
        Player other = Bukkit.getPlayerExact(args[1]);
        if (other == null) { player.sendMessage("§c找不到在线玩家：" + args[1]); return true; }
        PreGameSquad.Result result;
        if (action.equals("invite")) {
            result = squads.invite(player, other);
            if (result == PreGameSquad.Result.SUCCESS) {
                player.sendMessage("§a已向 " + other.getName() + " 发送队伍邀请（30 秒有效）。");
                other.sendMessage("§e" + player.getName() + " 邀请你成为固定队友。输入 /bw team accept " + player.getName() + " 接受。");
            } else player.sendMessage(failure(result));
            return true;
        }
        if (action.equals("accept") || action.equals("decline")) {
            result = action.equals("accept") ? squads.accept(player, other) : squads.decline(player, other);
            player.sendMessage(result == PreGameSquad.Result.SUCCESS
                    ? (action.equals("accept") ? "§a已加入 " + other.getName() + " 的固定队伍。" : "§e已拒绝邀请。")
                    : failure(result));
            if (result == PreGameSquad.Result.SUCCESS && action.equals("accept")) other.sendMessage("§a" + player.getName() + " 已加入你的固定队伍。");
            return true;
        }
        help(player);
        return true;
    }

    private String failure(PreGameSquad.Result result) {
        switch (result) {
            case NOT_IN_PRE_GAME: return "§c你和目标玩家必须在同一场尚未开始的竞技场。";
            case DIFFERENT_ARENA: return "§c你和目标玩家不在同一场竞技场。";
            case CANNOT_INVITE_SELF: return "§c不能邀请自己。";
            case NOT_LEADER: return "§c只有固定队伍队长可以邀请。";
            case TARGET_ALREADY_GROUPED: return "§c该玩家已经有固定队伍。";
            case SQUAD_FULL: return "§c固定队伍已达到本地图每队人数上限。";
            case ALREADY_INVITED: return "§e邀请已发送，请等待处理。";
            case INVITE_EXPIRED: return "§c邀请已过期。";
            case NO_INVITE: return "§c没有找到有效邀请。";
            default: return "§c无法完成此操作。";
        }
    }
    private void help(Player player) {
        player.sendMessage("§6固定队伍命令：");
        player.sendMessage("§e/bw team invite <玩家> §7邀请同场玩家成为队友");
        player.sendMessage("§e/bw team accept <队长> §7接受邀请");
        player.sendMessage("§e/bw team decline <队长> §7拒绝邀请");
        player.sendMessage("§e/bw team leave §7退出固定队伍");
    }
    @Override public List<String> getTabComplete() {
        return new ArrayList<>(Arrays.asList("invite", "accept", "decline", "leave", "help"));
    }
    @Override public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        return !(sender instanceof ConsoleCommandSender) && Arena.getArenaByPlayer((Player) sender) != null && hasPermission(sender);
    }
}
