package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class ListTeams extends SubCommand {
    public ListTeams(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return false;
        SetupSession session = SetupSession.getSession(((Player) sender).getUniqueId());
        if (session == null) return false;
        session.displayAvailableTeams();
        return true;
    }

    @Override public List<String> getTabComplete() { return Collections.emptyList(); }

    @Override public boolean canSee(CommandSender sender, BedWars api) {
        return sender instanceof Player && SetupSession.isInSetupSession(((Player) sender).getUniqueId())
                && hasPermission(sender);
    }
}
