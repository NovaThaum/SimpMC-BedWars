/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.player.PlayerInvisibilityPotionEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.InvisibilityManager;
import com.andrei1058.bedwars.sidebar.SidebarService;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * This is used to hide and show player name tag above head when he drinks an invisibility
 * potion or when the potion is gone. It is required because it is related to scoreboards.
 */
public class InvisibilityPotionListener implements Listener {

    private static final Color INVISIBILITY_PARTICLE_COLOR = Color.fromRGB(127, 127, 127);
    private final Set<UUID> pendingEquipmentRefresh = new HashSet<>();
    private final Map<UUID, BukkitTask> invisibilityParticleTasks = new HashMap<>();

    @EventHandler
    public void onPotion(@NotNull PlayerInvisibilityPotionEvent e) {
        if (e.getTeam() == null) return;
        SidebarService.getInstance().handleInvisibility(
                e.getTeam(), e.getPlayer(), e.getType() == PlayerInvisibilityPotionEvent.Type.ADDED
        );
    }

    @EventHandler
    public void onDrink(PlayerItemConsumeEvent e) {
        if (Arena.getArenaByPlayer(e.getPlayer()) == null) return;
        if (e.getItem().getType() != Material.POTION) return;
        // remove potion bottle
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                        nms.minusAmount(e.getPlayer(), new ItemStack(Material.GLASS_BOTTLE), 1),
                5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PotionEffect effect = event.getNewEffect();
        boolean invisibility = effect != null && PotionEffectType.INVISIBILITY.equals(effect.getType());
        IArena arena = Arena.getArenaByPlayer(player);
        if (isDrunkInvisibility(event.getCause(), event.getAction(), event.isOverride(), invisibility)
                && arena != null) {
            InvisibilityManager.activate(arena, player, durationSeconds(effect.getDuration()));
        }
        if (!shouldUseLowFrequencyParticles(PotionEffectType.INVISIBILITY.equals(event.getModifiedType()), arena != null)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || Arena.getArenaByPlayer(player) != arena) return;
            PotionEffect active = player.getPotionEffect(PotionEffectType.INVISIBILITY);
            if (active == null) {
                stopInvisibilityParticles(player.getUniqueId());
                return;
            }
            // Vanilla still applies the effect, but its own particle flag is suppressed so
            // that only the low-frequency particle below is visible for invisibility.
            if (active.hasParticles()) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.addPotionEffect(withoutParticles(active), true);
            }
            startInvisibilityParticles(player, arena);
        });
    }

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.getShowTime().containsKey(player)) return;
        if (!pendingEquipmentRefresh.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            pendingEquipmentRefresh.remove(player.getUniqueId());
            if (player.isOnline() && Arena.getArenaByPlayer(player) == arena) {
                InvisibilityManager.synchronizePlayerEquipment(arena, player);
            }
        });
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        stopInvisibilityParticles(event.getPlayer().getUniqueId());
    }

    private void startInvisibilityParticles(Player invisible, IArena arena) {
        UUID playerId = invisible.getUniqueId();
        if (invisibilityParticleTasks.containsKey(playerId)) return;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!invisible.isOnline() || Arena.getArenaByPlayer(invisible) != arena
                    || !invisible.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                stopInvisibilityParticles(playerId);
                return;
            }
            Location location = invisible.getLocation().add(0, 1, 0);
            for (Player viewer : arena.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(location) > 256.0D) continue;
                viewer.spawnParticle(Particle.ENTITY_EFFECT, location, 1,
                        0.18, 0.3, 0.18, 0, INVISIBILITY_PARTICLE_COLOR);
            }
        }, 1L, 20L);
        invisibilityParticleTasks.put(playerId, task);
    }

    private void stopInvisibilityParticles(UUID playerId) {
        BukkitTask task = invisibilityParticleTasks.remove(playerId);
        if (task != null) task.cancel();
    }

    static PotionEffect withoutParticles(PotionEffect effect) {
        return new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(),
                effect.isAmbient(), false, effect.hasIcon());
    }

    static boolean shouldUseLowFrequencyParticles(boolean invisibilityEffect, boolean inArena) {
        return invisibilityEffect && inArena;
    }

    static boolean isDrunkInvisibility(EntityPotionEffectEvent.Cause cause,
                                        EntityPotionEffectEvent.Action action,
                                        boolean override,
                                        boolean invisibility) {
        return cause == EntityPotionEffectEvent.Cause.POTION_DRINK
                && (action == EntityPotionEffectEvent.Action.ADDED
                || (action == EntityPotionEffectEvent.Action.CHANGED && override))
                && invisibility;
    }

    static int durationSeconds(int durationTicks) {
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                (durationTicks + 19L) / 20L));
    }
}
