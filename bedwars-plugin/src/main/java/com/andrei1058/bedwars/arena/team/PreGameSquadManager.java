package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.PreGameSquad;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/** Arena-local invitations which survive only until the next assignment. */
public final class PreGameSquadManager implements Listener, PreGameSquad {
    private static final long TTL = 30_000L;
    private static final PreGameSquadManager INSTANCE = new PreGameSquadManager();
    private final Map<UUID, Squad> members = new HashMap<>();
    private final Map<Key, Invite> invites = new HashMap<>();
    private PreGameSquadManager() { }
    public static PreGameSquadManager getInstance() { return INSTANCE; }

    public Result invite(Player inviter, Player target) {
        purge();
        IArena arena = preGame(inviter);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        if (inviter.equals(target)) return Result.CANNOT_INVITE_SELF;
        if (preGame(target) != arena) return Result.DIFFERENT_ARENA;
        Squad squad = members.get(inviter.getUniqueId());
        if (squad != null && !squad.leader.equals(inviter.getUniqueId())) return Result.NOT_LEADER;
        if (members.containsKey(target.getUniqueId())) return Result.TARGET_ALREADY_GROUPED;
        if ((squad == null ? 1 : squad.ids.size()) >= arena.getMaxInTeam()) return Result.SQUAD_FULL;
        Key key = new Key(inviter.getUniqueId(), target.getUniqueId());
        if (invites.containsKey(key)) return Result.ALREADY_INVITED;
        invites.put(key, new Invite(arena, System.currentTimeMillis() + TTL));
        return Result.SUCCESS;
    }

    public Result accept(Player target, Player inviter) {
        IArena arena = preGame(target);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        if (preGame(inviter) != arena) return Result.DIFFERENT_ARENA;
        Invite invite = invites.remove(new Key(inviter.getUniqueId(), target.getUniqueId()));
        if (invite == null) return Result.NO_INVITE;
        if (invite.expires < System.currentTimeMillis()) return Result.INVITE_EXPIRED;
        if (members.containsKey(target.getUniqueId())) return Result.TARGET_ALREADY_GROUPED;
        Squad squad = members.get(inviter.getUniqueId());
        if (squad != null && !squad.leader.equals(inviter.getUniqueId())) return Result.NOT_LEADER;
        if (squad == null) { squad = new Squad(arena, inviter.getUniqueId()); members.put(inviter.getUniqueId(), squad); }
        if (squad.ids.size() >= arena.getMaxInTeam()) return Result.SQUAD_FULL;
        squad.ids.add(target.getUniqueId()); members.put(target.getUniqueId(), squad);
        removeInvites(target.getUniqueId());
        return Result.SUCCESS;
    }

    public Result decline(Player target, Player inviter) {
        if (preGame(target) == null) return Result.NOT_IN_PRE_GAME;
        Invite invite = invites.remove(new Key(inviter.getUniqueId(), target.getUniqueId()));
        if (invite == null) return Result.NO_INVITE;
        return invite.expires < System.currentTimeMillis() ? Result.INVITE_EXPIRED : Result.SUCCESS;
    }

    public Result leave(Player player) {
        Squad squad = members.get(player.getUniqueId());
        if (squad == null) return Result.NOT_IN_SQUAD;
        removePlayer(player.getUniqueId());
        return Result.SUCCESS;
    }

    public boolean isLeader(Player player) { Squad s = members.get(player.getUniqueId()); return s == null || s.leader.equals(player.getUniqueId()); }
    public List<Player> getMembers(Player player) { Squad s = members.get(player.getUniqueId()); if (s == null) return Collections.singletonList(player); List<Player> out = new ArrayList<>(); for (UUID id : s.ids) { Player p = Bukkit.getPlayer(id); if (p != null && s.arena.isPlayer(p)) out.add(p); } return Collections.unmodifiableList(out); }
    public List<Player> getAvailableTargets(Player player) { IArena a = preGame(player); if (a == null || !isLeader(player)) return Collections.emptyList(); Squad s = members.get(player.getUniqueId()); if (s != null && s.ids.size() >= a.getMaxInTeam()) return Collections.emptyList(); List<Player> out = new ArrayList<>(); for (Player p : a.getPlayers()) if (!p.equals(player) && !members.containsKey(p.getUniqueId())) out.add(p); return out; }

    public List<List<Player>> getAssignmentGroups(IArena arena) {
        LinkedHashMap<UUID, Player> remaining = new LinkedHashMap<>(); for (Player p : arena.getPlayers()) remaining.put(p.getUniqueId(), p);
        List<List<Player>> groups = new ArrayList<>(); Set<Squad> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player p : arena.getPlayers()) { Squad s = members.get(p.getUniqueId()); if (s == null || s.arena != arena || !seen.add(s)) continue; List<Player> group = new ArrayList<>(getMembers(p)); group.removeIf(x -> !remaining.containsKey(x.getUniqueId())); if (group.size() > 1) { groups.add(group); for (Player x : group) remaining.remove(x.getUniqueId()); } }
        for (Player p : new ArrayList<>(remaining.values())) { if (BedWars.getParty().hasParty(p)) { List<Player> party = BedWars.getParty().getMembers(p); if (party != null) { LinkedHashSet<Player> group = new LinkedHashSet<>(); for (Player x : party) if (x != null && arena.isPlayer(x) && remaining.containsKey(x.getUniqueId())) group.add(x); if (group.size() > 1) { groups.add(new ArrayList<>(group)); for (Player x : group) remaining.remove(x.getUniqueId()); } } } }
        for (Player p : remaining.values()) groups.add(new ArrayList<>(Collections.singletonList(p))); return groups;
    }
    public void clearArena(IArena arena) { for (Squad s : new HashSet<>(members.values())) if (s.arena == arena) removeSquad(s); invites.entrySet().removeIf(e -> e.getValue().arena == arena); }
    @EventHandler public void onLeave(PlayerLeaveArenaEvent e) { removePlayer(e.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { removePlayer(e.getPlayer().getUniqueId()); }
    @EventHandler public void onState(GameStateChangeEvent e) { if (e.getNewState() == GameState.playing || e.getNewState() == GameState.restarting) clearArena(e.getArena()); }
    @EventHandler public void onDisable(ArenaDisableEvent e) { for (Squad s : new HashSet<>(members.values())) if (s.arena.getWorldName().equalsIgnoreCase(e.getWorldName())) removeSquad(s); }
    private IArena preGame(Player p) { IArena a = Arena.getArenaByPlayer(p); if (a == null || !a.isPlayer(p)) return null; GameState s = a.getStatus(); return s == GameState.waiting || s == GameState.starting ? a : null; }
    private void removePlayer(UUID id) { removeInvites(id); Squad s = members.remove(id); if (s == null) return; s.ids.remove(id); if (s.ids.size() < 2) removeSquad(s); else if (s.leader.equals(id)) s.leader = s.ids.iterator().next(); }
    private void removeSquad(Squad s) { for (UUID id : s.ids) members.remove(id); s.ids.clear(); }
    private void removeInvites(UUID id) { invites.keySet().removeIf(k -> k.inviter.equals(id) || k.target.equals(id)); }
    private void purge() { long now = System.currentTimeMillis(); invites.entrySet().removeIf(e -> e.getValue().expires < now); }
    private static final class Squad { final IArena arena; UUID leader; final LinkedHashSet<UUID> ids = new LinkedHashSet<>(); Squad(IArena a, UUID leader) { this.arena=a; this.leader=leader; ids.add(leader); } }
    private record Key(UUID inviter, UUID target) { }
    private record Invite(IArena arena, long expires) { }
}
