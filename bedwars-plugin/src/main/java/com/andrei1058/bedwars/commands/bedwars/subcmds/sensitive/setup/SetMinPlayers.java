package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SetMinPlayers extends SubCommand {
    public SetMinPlayers(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        SetupSession session = SetupSession.getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c▪ §7你当前不在竞技场设置会话中。");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage("§c▪ §7用法：/" + getParent().getName() + " setMinPlayers <整数>");
            return true;
        }

        int minimum;
        try {
            minimum = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            minimum = 0;
        }
        int capacity = arenaCapacity(session);
        if (minimum < 2 || (capacity > 0 && minimum > capacity)) {
            String range = capacity > 0 ? "2 到 " + capacity : "大于或等于 2";
            player.sendMessage("§c▪ §7全场最低开局人数必须是" + range + "的整数。");
            return true;
        }

        session.getConfig().set("minPlayers", minimum);
        player.sendMessage("§6▪ §7已将全场最低开局人数设置为 §e" + minimum
                + "§7；正常开局仍要求能够分成至少两个未超员的队伍。");
        return true;
    }

    static int arenaCapacity(SetupSession session) {
        ConfigurationSection teams = session.getConfig().getYml().getConfigurationSection("Team");
        if (teams == null) return 0;
        int maxInTeam = Math.max(1, session.getConfig().getYml().getInt("maxInTeam", 1));
        return teams.getKeys(false).size() * maxInTeam;
    }

    @Override public List<String> getTabComplete() {
        List<String> values = new ArrayList<>();
        for (int value = 2; value <= 16; value++) values.add(String.valueOf(value));
        return values;
    }

    @Override public boolean canSee(CommandSender sender, BedWars api) {
        return sender instanceof Player && SetupSession.isInSetupSession(((Player) sender).getUniqueId())
                && hasPermission(sender);
    }
}
