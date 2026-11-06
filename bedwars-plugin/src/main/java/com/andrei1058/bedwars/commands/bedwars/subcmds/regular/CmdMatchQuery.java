package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;
import java.util.UUID;

/** 1.8-compatible text query for persisted match history. */
public final class CmdMatchQuery extends SubCommand {
    public CmdMatchQuery(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(17);
        showInList(true);
        setDisplayInfo(new TextComponent("§6 ▪ §7/" + MainCommand.getInstance().getName() + " match"));
    }

    @Override public boolean execute(String[] args, CommandSender sender) {
        if (BedWars.plugin.getMatchHistory() == null) {
            send(sender, msg(sender, "match-history.disabled"));
            return true;
        }
        if (args.length > 0 && "all".equalsIgnoreCase(args[0])) return showAll(sender, page(args, 1));
        if (args.length == 0) {
            if (!(sender instanceof Player)) { send(sender, msg(sender, "match-history.usage")); return true; }
            return showTotals(sender, ((Player) sender).getUniqueId());
        }
        try {
            UUID uuid = UUID.fromString(args[0]);
            if (args.length > 1 && "list".equalsIgnoreCase(args[1])) return showList(sender, uuid, page(args, 2));
            return showTotals(sender, uuid);
        } catch (IllegalArgumentException exception) {
            try {
                long number = Long.parseLong(args[0]);
                BedWars.plugin.getMatchHistory().findMatch(number).whenComplete((match, error) -> {
                    send(sender, error != null || match.isEmpty() ? msg(sender, "match-history.not-found") : formatMatch(match.get()));
                });
                return true;
            } catch (NumberFormatException ignored) { send(sender, msg(sender, "match-history.usage")); return true; }
        }
    }

    private boolean showTotals(CommandSender sender, UUID uuid) {
        BedWars.plugin.getMatchHistory().getPlayerTotals(uuid).whenComplete((totals, error) -> {
            if (error != null) send(sender, msg(sender, "match-history.not-found") + " " + error.getMessage());
            else send(sender, "§6" + uuid + " §7场次 " + totals.matchesPlayed() + " 击杀 " + totals.kills()
                    + " 最终击杀 " + totals.finalKills() + " 死亡 " + totals.deaths() + " K/D "
                    + String.format(java.util.Locale.ROOT, "%.2f", totals.kdRatio()));
        });
        return true;
    }

    private boolean showList(CommandSender sender, UUID uuid, int page) {
        BedWars.plugin.getMatchHistory().getPlayerMatches(uuid, 10, (page - 1) * 10).whenComplete((matches, error) -> {
            if (error != null) send(sender, msg(sender, "match-history.not-found") + " " + error.getMessage());
            else if (matches.isEmpty()) send(sender, msg(sender, "match-history.empty"));
            else for (MatchInfo match : matches) send(sender, formatMatch(match));
        });
        return true;
    }

    private boolean showAll(CommandSender sender, int page) {
        BedWars.plugin.getMatchHistory().getMatches(10, (page - 1) * 10).whenComplete((matches, error) -> {
            if (error != null) send(sender, msg(sender, "match-history.not-found") + " " + error.getMessage());
            else if (matches.isEmpty()) send(sender, msg(sender, "match-history.empty"));
            else for (MatchInfo match : matches) send(sender, formatMatch(match));
        });
        return true;
    }

    private static int page(String[] args, int index) {
        if (args.length <= index) return 1;
        try { return Math.max(1, Integer.parseInt(args[index])); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private static String formatMatch(MatchInfo match) {
        return "§6对局 #" + match.matchNumber() + " §7" + match.status() + " §f" + match.arenaName();
    }

    private static void send(CommandSender sender, String message) {
        if (Bukkit.isPrimaryThread()) sender.sendMessage(message);
        else Bukkit.getScheduler().runTask(BedWars.plugin, () -> sender.sendMessage(message));
    }

    private static String msg(CommandSender sender, String key) {
        Language language = sender instanceof Player
                ? Language.getPlayerLanguage((Player) sender) : Language.getDefaultLanguage();
        return language.m(key).replace("{prefix}", "");
    }

    @Override public List<String> getTabComplete() { return java.util.Collections.emptyList(); }
    @Override public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) { return hasPermission(sender); }
}
