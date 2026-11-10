package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.matchmaking.ArenaInviteManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Invites a lobby player into the sender's waiting arena. */
public final class CmdInvite extends SubCommand {
    private final ArenaInviteManager invitations = ArenaInviteManager.getInstance();
    public CmdInvite(ParentCommand parent, String name) { super(parent, name); setPriority(13); }
    @Override public boolean execute(String[] args, CommandSender sender) {
        if (!(sender instanceof Player)) return false; Player player = (Player) sender;
        if (args.length == 0) { help(player); return true; }
        String action = args[0].toLowerCase();
        if (action.equals("list")) { list(player); return true; }
        if ((action.equals("accept") || action.equals("decline") || action.equals("reject")) && args.length > 1) {
            Player inviter = Bukkit.getPlayerExact(args[1]);
            if (inviter == null) { player.sendMessage("§c邀请者已离线。"); return true; }
            ArenaInviteManager.Invitation i = invitations.find(player.getUniqueId(), inviter.getUniqueId(), System.currentTimeMillis());
            if (i == null) { player.sendMessage("§c没有找到有效邀请。"); return true; }
            if (action.equals("accept")) {
                IArena arena = Arena.getArenaByName(i.arenaName);
                if (arena == null || Arena.getArenaByPlayer(inviter) != arena || arena.getStatus() == GameState.playing || !arena.addPlayer(player, true)) { player.sendMessage("§c加入失败，竞技场可能已满或即将开始。"); invitations.remove(player.getUniqueId(), inviter.getUniqueId()); return true; }
                invitations.clearPlayer(player.getUniqueId()); player.sendMessage("§a你已接受竞技场邀请。"); inviter.sendMessage("§a" + player.getName() + " 已加入你的竞技场。");
            } else { invitations.remove(player.getUniqueId(), inviter.getUniqueId()); player.sendMessage("§e已拒绝邀请。"); inviter.sendMessage("§e" + player.getName() + " 拒绝了你的邀请。"); }
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]); IArena arena = Arena.getArenaByPlayer(player);
        if (target == null || arena == null || (arena.getStatus() != GameState.waiting && arena.getStatus() != GameState.starting) || arena.getPlayers().size() >= arena.getMaxPlayers() || Arena.getArenaByPlayer(target) != null) { player.sendMessage("§c只能从尚未开始且未满的竞技场邀请大厅玩家。"); return true; }
        invitations.create(player.getUniqueId(), target.getUniqueId(), arena.getArenaName(), System.currentTimeMillis());
        player.sendMessage("§a已向 " + target.getName() + " 发送 30 秒竞技场邀请。"); target.sendMessage("§e" + player.getName() + " 邀请你加入竞技场，输入 §f/bw invite accept " + player.getName() + " §e接受。"); return true;
    }
    private void list(Player p) { List<ArenaInviteManager.Invitation> list = invitations.findAll(p.getUniqueId(), System.currentTimeMillis()); if (list.isEmpty()) { p.sendMessage("§7当前没有有效竞技场邀请。"); return; } p.sendMessage("§6当前竞技场邀请："); for (ArenaInviteManager.Invitation i : list) { Player inviter = Bukkit.getPlayer(i.inviter); p.sendMessage("§7- §b" + (inviter == null ? i.inviter : inviter.getName()) + " §a→ " + i.arenaName); } }
    private void help(Player p) { p.sendMessage("§6/bw invite <玩家> §7邀请大厅玩家加入当前竞技场"); p.sendMessage("§6/bw invite accept|decline <邀请者> §7处理邀请"); p.sendMessage("§6/bw invite list §7查看邀请"); }
    @Override public List<String> getTabComplete() { return new ArrayList<>(Arrays.asList("accept", "decline", "list")); }
    @Override public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) { return sender instanceof Player && hasPermission(sender); }
}
