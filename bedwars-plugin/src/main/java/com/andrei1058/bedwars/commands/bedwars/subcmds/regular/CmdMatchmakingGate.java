package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.MatchStartGate;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.language.MatchmakingMessages;
import org.bukkit.command.CommandSender;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 暂停或恢复本服新对局，允许控制台和获授权的管理员使用。 */
public final class CmdMatchmakingGate extends SubCommand {
    public enum Action { STOP, RESUME }
    private final Action action;
    public CmdMatchmakingGate(ParentCommand parent, String name, Action action) {
        super(parent, name);
        this.action = action;
        setPermission("bw.command." + name.toLowerCase(Locale.ROOT));
        setPriority(12);
        showInList(true);
        String description = action == Action.STOP ? "暂停新对局开局，已开始的比赛继续" : "恢复新对局开局";
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7/" + parent.getName() + " " + name + " §8 - §e" + description,
                "/" + parent.getName() + " " + name, "§f" + description));
    }
    @Override public boolean execute(String[] args, CommandSender sender) {
        if (!hasPermission(sender)) {
            sender.sendMessage(MatchmakingMessages.text(sender, MatchmakingMessages.Key.NO_PERMISSION));
            return true;
        }
        if (args.length != 0) return false;
        boolean changed = action == Action.STOP ? MatchStartGate.stop() : MatchStartGate.resume();
        if (changed) {
            for (IArena arena : Arena.getArenas()) {
                if (action == Action.STOP) {
                    if (arena.getStatus() == GameState.starting) arena.changeStatus(GameState.waiting);
                } else if (arena instanceof Arena) {
                    ((Arena) arena).reevaluateStartEligibility();
                }
            }
        }
        MatchmakingMessages.Key key = action == Action.STOP
                ? (changed ? MatchmakingMessages.Key.MATCHES_BLOCKED : MatchmakingMessages.Key.MATCHES_ALREADY_BLOCKED)
                : (changed ? MatchmakingMessages.Key.MATCHES_RESUMED : MatchmakingMessages.Key.MATCHES_ALREADY_RESUMED);
        sender.sendMessage(MatchmakingMessages.text(sender, key));
        return true;
    }
    @Override public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        return hasPermission(sender);
    }
    @Override public List<String> getTabComplete() { return Collections.emptyList(); }
}
