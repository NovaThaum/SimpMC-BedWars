package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.support.npc.NativeJoinNpcs;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 内置 NPC 管理；旧版 Citizens NPC 继续使用 /npc remove 删除。 */
public final class NativeNPC extends SubCommand {
  public NativeNPC(ParentCommand parent) {
    super(parent, "npc");
    setPermission(Permissions.PERMISSION_NPC);
    showInList(true);
    setPriority(12);
    setDisplayInfo(new net.md_5.bungee.api.chat.TextComponent("§6 ▪ §7/bw npc §8- §e管理大厅加入游戏 NPC"));
  }

  @Override
  public boolean execute(String[] args, CommandSender sender) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage("§c请在游戏内管理 NPC。");
      return true;
    }
    NativeJoinNpcs manager = NativeJoinNpcs.get();
    if (manager == null) {
      p.sendMessage("§c内置 NPC 尚未就绪，请检查控制台。");
      return true;
    }
    if (Arena.isInArena(p) || SetupSession.isInSetupSession(p.getUniqueId())) {
      p.sendMessage("§c请在大厅中管理 NPC。");
      return true;
    }
    try {
      if (args.length >= 3
          && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("add"))) {
        if (!validGroups(args[2])) {
          p.sendMessage("§c竞技场分组不存在，请先在 arenaGroups 中配置分组。");
          return true;
        }
        manager.create(
            p.getLocation(),
            args[1],
            args[2],
            args.length == 3 ? "" : String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
        p.sendMessage("§a已创建加入游戏 NPC，目标分组：§f" + args[2]);
        p.sendMessage("§7皮肤异步加载，玩家右键 NPC 即可加入该分组。");
      } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
        p.sendMessage(
            manager.removeTarget(p)
                ? "§a已删除你正在看向的内置 NPC。"
                : "§c请看向 6 格内的内置 NPC；Citizens NPC 请用 /npc remove 删除。");
      } else {
        p.sendMessage("§e/bw npc create <皮肤玩家名> <竞技场分组> [悬浮文字]");
        p.sendMessage("§7悬浮文字可省略；支持 & 颜色代码、{players} 分组人数，最多 6 行。add 为兼容别名。");
        p.sendMessage("§e/bw npc remove §7删除正在看向的内置 NPC。");
      }
    } catch (IllegalArgumentException e) {
      p.sendMessage("§c" + e.getMessage());
    } catch (java.io.IOException e) {
      p.sendMessage("§cNPC 配置保存失败，操作未生效，请检查控制台。");
      com.andrei1058.bedwars.BedWars.plugin.getLogger().warning("NPC 配置保存失败：" + e);
    }
    return true;
  }

  static boolean validGroups(String groups) {
    return Arrays.stream(groups.split("\\+", -1))
        .allMatch(g -> !g.isBlank() && MainCommand.isArenaGroup(g));
  }

  @Override
  public List<String> getTabComplete() {
    return List.of("create", "add", "remove");
  }

  @Override
  public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
    return s instanceof Player p
        && !Arena.isInArena(p)
        && !SetupSession.isInSetupSession(p.getUniqueId())
        && hasPermission(s);
  }
}
