package com.andrei1058.bedwars.arena.matchmaking;
import java.util.*;
/** Short-lived arena invitations from a player to a lobby player. */
public final class ArenaInviteManager {
    public static final long INVITE_LIFETIME_MILLIS = 30_000L;
    private static final ArenaInviteManager INSTANCE = new ArenaInviteManager();
    private final Map<UUID, Map<UUID, Invitation>> byTarget = new HashMap<>();
    private ArenaInviteManager() { }
    public static ArenaInviteManager getInstance() { return INSTANCE; }
    public void create(UUID inviter, UUID target, String arena, long now) { byTarget.computeIfAbsent(target, x -> new HashMap<>()).put(inviter, new Invitation(inviter, target, arena, now + INVITE_LIFETIME_MILLIS)); }
    public Invitation find(UUID target, UUID inviter, long now) { Map<UUID, Invitation> map = byTarget.get(target); if (map == null) return null; Invitation i = map.get(inviter); if (i == null || i.expiresAtMillis <= now) { remove(target, inviter); return null; } return i; }
    public List<Invitation> findAll(UUID target, long now) { Map<UUID, Invitation> map = byTarget.get(target); if (map == null) return Collections.emptyList(); List<Invitation> out = new ArrayList<>(); for (Invitation i : new ArrayList<>(map.values())) if (i.expiresAtMillis > now) out.add(i); else remove(target, i.inviter); return out; }
    public void remove(UUID target, UUID inviter) { Map<UUID, Invitation> map = byTarget.get(target); if (map == null) return; map.remove(inviter); if (map.isEmpty()) byTarget.remove(target); }
    public void clearPlayer(UUID player) { byTarget.remove(player); for (UUID target : new ArrayList<>(byTarget.keySet())) remove(target, player); }
    public static final class Invitation { public final UUID inviter, target; public final String arenaName; public final long expiresAtMillis; Invitation(UUID i, UUID t, String a, long e) { inviter=i; target=t; arenaName=a; expiresAtMillis=e; } }
}
