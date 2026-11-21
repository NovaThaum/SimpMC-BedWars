/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.api.messages.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EnemyTrackerCompass implements Runnable {

    private static final String ITEM_DATA = "ENEMY_TRACKER_COMPASS";
    private static final int HOTBAR_SLOT = 8;
    private static final double IDLE_TARGET_RADIUS = 16.0;
    private static final double IDLE_SPIN_STEP = Math.PI / 5.0;
    private static final Map<UUID, Player> trackedTargets = new HashMap<>();

    private double idleAngle;

    public static Player getTrackedTarget(Player player) {
        if (player == null) return null;
        Player target = trackedTargets.get(player.getUniqueId());
        return target != null && target.isOnline() ? target : null;
    }

    public static void giveTo(Player player) {
        if (player == null) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isTrackingCompass(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }

        ItemStack displaced = inventory.getItem(HOTBAR_SLOT);
        if (displaced != null && displaced.getType() != Material.AIR) {
            int emptySlot = inventory.firstEmpty();
            if (emptySlot == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), displaced);
            } else {
                inventory.setItem(emptySlot, displaced);
            }
        }

        ItemStack compass = BedWars.nms.addCustomData(new ItemStack(Material.COMPASS), ITEM_DATA);
        inventory.setItem(HOTBAR_SLOT, compass);
        player.updateInventory();
    }

    public static boolean isTrackingCompass(ItemStack item) {
        return item != null
                && item.getType() == Material.COMPASS
                && BedWars.nms != null
                && BedWars.nms.isCustomBedWarsItem(item)
                && ITEM_DATA.equals(BedWars.nms.getCustomData(item));
    }

    @Override
    public void run() {
        idleAngle += IDLE_SPIN_STEP;
        if (idleAngle >= Math.PI * 2.0) {
            idleAngle -= Math.PI * 2.0;
        }

        updateArenas(new ArrayList<>(Arena.getArenas()));
    }

    void updateArenas(List<IArena> arenas) {
        Set<UUID> currentTrackers = new HashSet<>();
        for (IArena arena : arenas) {
            updateArena(arena, currentTrackers);
        }
        trackedTargets.keySet().removeIf(uuid -> !currentTrackers.contains(uuid));
    }

    void updateArena(IArena arena, Set<UUID> currentTrackers) {
        if (arena == null || arena.getStatus() != GameState.playing) return;

        List<Player> arenaPlayers = arena.getPlayers();
        if (arenaPlayers == null || arenaPlayers.isEmpty()) return;
        List<TrackedPlayer> activePlayers = new ArrayList<>(arenaPlayers.size());
        for (Player player : arenaPlayers) {
            if (!isActivePlayer(arena, player)) continue;
            ITeam team = arena.getTeam(player);
            if (team != null) activePlayers.add(new TrackedPlayer(player, team, player.getLocation()));
        }

        for (TrackedPlayer trackedPlayer : activePlayers) {
            Player player = trackedPlayer.player();
            if (!hasTrackingCompass(player)) continue;
            currentTrackers.add(player.getUniqueId());

            TrackedPlayer nearestEnemy = findNearestEnemy(arena, activePlayers, trackedPlayer);
            if (nearestEnemy == null) {
                trackedTargets.remove(player.getUniqueId());
                player.setCompassTarget(createIdleTarget(player));
            } else {
                trackedTargets.put(player.getUniqueId(), nearestEnemy.player());
                player.setCompassTarget(nearestEnemy.location());
            }
        }
    }

    /** 与指针共用当前敌人目标，避免距离提示仍追踪队友。 */
    public static void showTrackingActionBar(IArena arena, Player player) {
        Player target = getTrackedTarget(player);
        if (arena == null || arena.getStatus() != GameState.playing || target == null
                || !isActivePlayer(arena, player) || !isActivePlayer(arena, target)
                || isInvisible(arena, target)) return;
        ITeam team = arena.getTeam(target);
        if (team == null || team == arena.getTeam(player)) return;
        String color = team.getColor().chat().toString();
        int distance = (int) player.getLocation().distance(target.getLocation());
        BedWars.nms.playAction(player, ChineseMessages.getMsg(player, Messages.FORMATTING_ACTION_BAR_TRACKING)
                .replace("{player}", color + target.getName())
                .replace("{team}", color + team.getDisplayName())
                .replace("{distance}", color + distance).replace('&', '§'));
    }

    private TrackedPlayer findNearestEnemy(IArena arena, List<TrackedPlayer> players,
                                           TrackedPlayer trackedPlayer) {
        TrackedPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (TrackedPlayer candidate : players) {
            if (candidate.player().equals(trackedPlayer.player())) continue;
            if (isInvisible(arena, candidate.player())) continue;
            if (candidate.team() == trackedPlayer.team()) continue;

            double distance = trackedPlayer.location().distanceSquared(candidate.location());
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isActivePlayer(IArena arena, Player player) {
        return player != null
                && player.isOnline()
                && arena.isPlayer(player)
                && !player.isDead()
                && !arena.isSpectator(player)
                && !arena.isReSpawning(player)
                && arena.getWorld() != null
                && arena.getWorld().equals(player.getWorld());
    }

    private static boolean isInvisible(IArena arena, Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                || (arena.getShowTime() != null && arena.getShowTime().containsKey(player));
    }

    private boolean hasTrackingCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTrackingCompass(item)) return true;
        }
        return false;
    }

    private record TrackedPlayer(Player player, ITeam team, Location location) {
    }

    private Location createIdleTarget(Player player) {
        return player.getLocation().clone().add(
                Math.cos(idleAngle) * IDLE_TARGET_RADIUS,
                0,
                Math.sin(idleAngle) * IDLE_TARGET_RADIUS
        );
    }
}
