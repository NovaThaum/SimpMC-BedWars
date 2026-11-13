package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.stats.MatchHistory;
import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.api.stats.MatchPlayerResult;
import com.andrei1058.bedwars.api.stats.PlayerMatchTotals;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.language.MatchQueryMessages;
import com.andrei1058.bedwars.language.MatchQueryMessages.Key;
import com.andrei1058.bedwars.language.MatchQueryMessages.Snapshot;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Persisted match queries with the BW1058 command semantics. */
public final class CmdMatchQuery extends SubCommand {
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));
    private static final Set<CommandSender> PENDING = ConcurrentHashMap.newKeySet();

    public CmdMatchQuery(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(17);
        showInList(true);
        String usage = "/" + parent.getName() + " " + name;
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7" + usage, usage, description(null, name)));
    }

    private static String description(CommandSender sender, String command) {
        return MatchQueryMessages.description(sender, command);
    }

    public net.md_5.bungee.api.chat.TextComponent getDisplayInfo(CommandSender sender) {
        String usage = "/" + getParent().getName() + " " + getSubCommandName();
        return MainCommand.createTC("§6 ▪ §7" + usage, usage,
                description(sender, getSubCommandName()));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        MatchHistory history = BedWars.plugin.getMatchHistory();
        Snapshot messages = MatchQueryMessages.forSender(sender);
        if (history == null || !history.isEnabled()) {
            send(sender, messages.text(Key.DISABLED));
            return true;
        }
        if (!PENDING.add(sender)) {
            send(sender, messages.text(Key.PENDING));
            return true;
        }

        try {
            CompletableFuture<List<String>> response;
            switch (getSubCommandName().toLowerCase(Locale.ROOT)) {
                case "match": response = match(args, sender, history, messages); break;
                case "history": response = history(args, sender, history, messages); break;
                default: response = totals(args, sender, history, messages); break;
            }
            response.whenComplete((lines, error) -> {
                PENDING.remove(sender);
                if (!BedWars.plugin.isEnabled()) return;
                try {
                    Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                        if (sender instanceof Player && !((Player) sender).isOnline()) return;
                        if (error != null) {
                            Throwable cause = error;
                            while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
                                cause = cause.getCause();
                            }
                            if (cause instanceof IllegalArgumentException) {
                                send(sender, cause.getMessage());
                            } else {
                                BedWars.plugin.getLogger().log(Level.WARNING, "查询对局战绩失败", error);
                                send(sender, messages.text(Key.FAILED));
                            }
                        } else {
                            for (String line : lines) send(sender, line);
                        }
                    });
                } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
                    // The server may stop between the enabled check and task submission.
                }
            });
        } catch (IllegalArgumentException exception) {
            PENDING.remove(sender);
            send(sender, exception.getMessage());
        } catch (RuntimeException exception) {
            PENDING.remove(sender);
            throw exception;
        }
        return true;
    }

    private CompletableFuture<List<String>> match(String[] args, CommandSender sender,
                                                   MatchHistory history, Snapshot messages) {
        if (args.length > 1) throw new IllegalArgumentException(messages.text(Key.MATCH_USAGE));
        CompletableFuture<Optional<MatchInfo>> query;
        if (args.length == 0) {
            if (!(sender instanceof Player)) throw new IllegalArgumentException(messages.text(Key.CONSOLE_MATCH_ID));
            Optional<MatchInfo> current = history.getCurrentMatch(Arena.getArenaByPlayer((Player) sender));
            if (!current.isPresent()) {
                return CompletableFuture.completedFuture(Collections.singletonList(messages.text(Key.NO_CURRENT_MATCH)));
            }
            query = CompletableFuture.completedFuture(current);
        } else {
            query = findMatch(history, args[0], messages);
        }
        return query.thenCompose(found -> {
            if (!found.isPresent()) {
                return CompletableFuture.completedFuture(Collections.singletonList(messages.text(Key.MATCH_NOT_FOUND)));
            }
            MatchInfo info = found.get();
            List<String> lines = matchHeader(info, messages);
            if (info.matchNumber() == 0) {
                lines.add(messages.text(Key.NUMBER_PENDING));
                return CompletableFuture.completedFuture(Collections.unmodifiableList(lines));
            }
            return history.getMatchPlayers(info.matchUuid()).thenApply(players -> {
                if ("RUNNING".equals(info.status())) lines.add(messages.text(Key.RUNNING_SNAPSHOT));
                for (MatchPlayerResult player : players) {
                    String name = player.playerName() == null ? player.playerUuid().toString() : player.playerName();
                    lines.add(messages.text(Key.PLAYER_ROW, "{player}", name,
                            "{outcome}", outcome(player.outcome(), messages),
                            "{counters}", counters(player.kills(), player.finalKills(), player.deaths(),
                                    player.bedsDestroyed(), player.kdRatio(), messages)));
                }
                if (players.isEmpty()) lines.add(messages.text(Key.NO_PLAYERS));
                return lines;
            });
        });
    }

    private CompletableFuture<Optional<MatchInfo>> findMatch(MatchHistory history, String identifier,
                                                              Snapshot messages) {
        if (identifier.matches("[0-9]+")) {
            try {
                long number = Long.parseLong(identifier);
                if (number <= 0) throw new NumberFormatException();
                return history.findMatch(number);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(messages.text(Key.INVALID_IDENTIFIER));
            }
        }
        try {
            UUID uuid = parseUuid(identifier);
            return history.findMatch(uuid);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(messages.text(Key.INVALID_IDENTIFIER));
        }
    }

    private CompletableFuture<List<String>> history(String[] args, CommandSender sender,
                                                    MatchHistory history, Snapshot messages) {
        if (args.length > 2) throw new IllegalArgumentException(messages.text(Key.HISTORY_USAGE));
        String target = args.length == 0 ? null : args[0];
        int offset = pageOffset(args.length > 1 ? args[1] : "1", messages);
        int page = offset / PAGE_SIZE + 1;
        if (target != null && "all".equalsIgnoreCase(target)) {
            return history.getMatches(PAGE_SIZE, offset).thenApply(matches ->
                    historyLines(matches, messages.text(Key.HISTORY_ALL), "all", page, true, messages));
        }
        return playerId(target, sender, history, messages).thenCompose(uuid ->
                history.getPlayerMatches(uuid, PAGE_SIZE, offset).thenApply(matches ->
                        historyLines(matches, target == null ? playerName(sender, uuid) : target,
                                target == null ? uuid.toString() : target, page, false, messages)));
    }

    private CompletableFuture<List<String>> totals(String[] args, CommandSender sender,
                                                    MatchHistory history, Snapshot messages) {
        if (args.length > 1) throw new IllegalArgumentException(messages.text(Key.RECORD_USAGE));
        String target = args.length == 0 ? null : args[0];
        return playerId(target, sender, history, messages).thenCompose(uuid -> history.getPlayerTotals(uuid)
                .thenApply(total -> totalsLines(total, target == null ? playerName(sender, uuid) : target, messages)));
    }

    static List<String> historyLines(List<MatchInfo> matches, String displayTarget,
                                    String commandTarget, int page, boolean oldestAtTop, Snapshot messages) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.text(Key.HISTORY_HEADER, "{target}", displayTarget, "{page}", String.valueOf(page)));
        List<MatchInfo> displayMatches = new ArrayList<>(matches);
        if (oldestAtTop) Collections.reverse(displayMatches);
        for (MatchInfo info : displayMatches) {
            lines.add(messages.text(Key.HISTORY_ROW, "{number}", String.valueOf(info.matchNumber()),
                    "{arena}", info.arenaName(), "{status}", status(info.status(), messages),
                    "{time}", TIME.format(info.startedAt())));
        }
        if (matches.isEmpty()) lines.add(messages.text(Key.HISTORY_EMPTY));
        if (!commandTarget.isEmpty()) {
            lines.add(messages.text(Key.HISTORY_NEXT, "{target}", commandTarget, "{page}", String.valueOf(page + 1)));
        }
        return lines;
    }

    private static List<String> totalsLines(PlayerMatchTotals total, String target, Snapshot messages) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.text(Key.TOTALS_HEADER, "{player}", target));
        lines.add(messages.text(Key.TOTALS_MATCHES, "{count}", String.valueOf(total.matchesPlayed())));
        lines.add(counters(total.kills(), total.finalKills(), total.deaths(), total.bedsDestroyed(),
                total.kdRatio(), messages));
        lines.add(messages.text(Key.KD_EXPLANATION));
        return lines;
    }

    private CompletableFuture<UUID> playerId(String value, CommandSender sender,
                                              MatchHistory history, Snapshot messages) {
        if (value == null) {
            if (sender instanceof Player) return CompletableFuture.completedFuture(((Player) sender).getUniqueId());
            throw new IllegalArgumentException(messages.text(Key.CONSOLE_HISTORY_TARGET));
        }
        try {
            return CompletableFuture.completedFuture(parseUuid(value));
        } catch (IllegalArgumentException ignored) {
            if (value.contains("-")) throw new IllegalArgumentException(messages.text(Key.INVALID_PLAYER_UUID));
        }
        Player online = Bukkit.getPlayerExact(value);
        if (online != null) return CompletableFuture.completedFuture(online.getUniqueId());
        return history.findPlayerUuid(value).thenApply(found -> {
            if (!found.isPresent()) throw new IllegalArgumentException(messages.text(Key.PLAYER_NOT_FOUND));
            return found.get();
        });
    }

    private static UUID parseUuid(String value) {
        UUID uuid = UUID.fromString(value);
        if (!uuid.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        return uuid;
    }

    static int pageOffset(String text) {
        return pageOffset(text, MatchQueryMessages.defaults(true));
    }

    private static int pageOffset(String text, Snapshot messages) {
        try {
            int page = Integer.parseInt(text);
            if (page < 1) throw new NumberFormatException();
            return Math.multiplyExact(page - 1, PAGE_SIZE);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new IllegalArgumentException(messages.text(Key.INVALID_PAGE));
        }
    }

    private static String playerName(CommandSender sender, UUID uuid) {
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(uuid)) {
            return ((Player) sender).getName();
        }
        return uuid.toString();
    }

    private static List<String> matchHeader(MatchInfo info, Snapshot messages) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.text(Key.MATCH_HEADER, "{number}", info.matchNumber() == 0
                ? messages.text(Key.MATCH_NUMBER_PENDING) : String.valueOf(info.matchNumber()),
                "{status}", status(info.status(), messages)));
        lines.add(messages.text(Key.MATCH_UUID, "{uuid}", info.matchUuid().toString()));
        lines.add(messages.text(Key.MATCH_MAP_GROUP, "{map}", info.arenaName(), "{group}", info.arenaGroup()));
        lines.add(messages.text(Key.MATCH_TIMES, "{start}", TIME.format(info.startedAt()),
                "{end}", info.endedAt() == null ? messages.text(Key.MATCH_NOT_ENDED) : TIME.format(info.endedAt())));
        return lines;
    }

    private static String counters(long kills, long finalKills, long deaths, long beds, double kd, Snapshot messages) {
        return messages.text(Key.COUNTERS, "{kills}", String.valueOf(kills),
                "{finalKills}", String.valueOf(finalKills), "{beds}", String.valueOf(beds),
                "{deaths}", String.valueOf(deaths), "{kd}", String.format(Locale.ROOT, "%.4f", kd));
    }

    private static String status(String status, Snapshot messages) {
        if ("FINISHED".equals(status)) return messages.text(Key.STATUS_FINISHED);
        if ("ABORTED".equals(status)) return messages.text(Key.STATUS_ABORTED);
        return messages.text(Key.STATUS_RUNNING);
    }

    private static String outcome(String value, Snapshot messages) {
        if ("WIN".equals(value)) return messages.text(Key.OUTCOME_WIN);
        if ("LOSS".equals(value)) return messages.text(Key.OUTCOME_LOSS);
        if ("ABANDONED".equals(value)) return messages.text(Key.OUTCOME_ABANDONED);
        if ("DISCONNECTED".equals(value)) return messages.text(Key.OUTCOME_DISCONNECTED);
        return messages.text(Key.OUTCOME_UNSETTLED);
    }

    private static void send(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    public List<String> getTabComplete() {
        if ("history".equalsIgnoreCase(getSubCommandName())) return Collections.singletonList("all");
        return Collections.emptyList();
    }

    @Override
    public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        return hasPermission(sender);
    }
}
