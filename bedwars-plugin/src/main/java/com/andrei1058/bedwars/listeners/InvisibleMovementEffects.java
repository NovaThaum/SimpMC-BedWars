package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InvisibleMovementEffects implements Listener {
    private final Map<UUID, Long> lastSprintEffectAt = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = lastSprintEffectAt.get(player.getUniqueId());
        if (!InvisibleMovementPolicy.shouldPlaySprintEffects(player.hasPotionEffect(PotionEffectType.INVISIBILITY),
                player.isSprinting(), player.isOnGround(), dx * dx + dz * dz, last, now)) return;
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(player)) return;
        lastSprintEffectAt.put(player.getUniqueId(), now);
        Location location = player.getLocation();
        Sound sound = sprintStepSound(location);
        for (Player viewer : arena.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(location) <= 256.0D) {
                Material ground = location.getBlock().getRelative(0, -1, 0).getType();
                if (ground != Material.AIR) {
                    viewer.spigot().playEffect(location, Effect.TILE_DUST, ground.getId(),
                            location.getBlock().getRelative(0, -1, 0).getData(),
                            0.25f, 0.08f, 0.25f, 0.04f, 8, 16);
                }
                viewer.playSound(location, sound, 0.35f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        Player player = (Player) event.getEntity();
        if (!InvisibleMovementPolicy.shouldPlayFallSound(player.hasPotionEffect(PotionEffectType.INVISIBILITY),
                event.isCancelled(), event.getDamage())) return;
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(player)) return;
        Sound sound = event.getDamage() >= 4.0D ? Sound.FALL_BIG : Sound.FALL_SMALL;
        Location location = player.getLocation();
        for (Player viewer : arena.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(location) <= 256.0D) {
                viewer.playSound(location, sound, 0.8f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSprintEffectAt.remove(event.getPlayer().getUniqueId());
    }

    private static Sound sprintStepSound(Location location) {
        Material type = location.getBlock().getRelative(0, -1, 0).getType();
        String block = type.name();
        if (block.contains("WOOL")) return Sound.STEP_WOOL;
        if (block.contains("WOOD") || block.contains("PLANK") || block.contains("LOG")) return Sound.STEP_WOOD;
        if (block.contains("GRAVEL")) return Sound.STEP_GRAVEL;
        if (block.contains("SAND")) return Sound.STEP_SAND;
        if (block.contains("SNOW")) return Sound.STEP_SNOW;
        if (block.contains("GRASS") || block.contains("DIRT") || block.contains("SOIL")) return Sound.STEP_GRASS;
        return Sound.STEP_STONE;
    }
}
