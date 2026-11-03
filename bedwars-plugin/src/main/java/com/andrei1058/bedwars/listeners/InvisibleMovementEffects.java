package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.Particle;
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
    private static final double SOUND_RADIUS_SQUARED = 256.0D;
    private final Map<UUID, Long> lastSprintEffectAt = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistanceSquared = dx * dx + dz * dz;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long lastEffectAt = lastSprintEffectAt.get(playerId);
        if (!InvisibleMovementPolicy.shouldPlaySprintEffects(
                player.hasPotionEffect(PotionEffectType.INVISIBILITY), player.isSprinting(), player.isOnGround(),
                horizontalDistanceSquared, lastEffectAt, now)) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(player)) return;
        lastSprintEffectAt.put(playerId, now);
        Location effectLocation = player.getLocation();
        Sound stepSound = sprintStepSound(effectLocation);
        for (Player viewer : arena.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(effectLocation) > SOUND_RADIUS_SQUARED) continue;
            Block block = effectLocation.getBlock().getRelative(BlockFace.DOWN);
            if (!block.getType().isAir()) {
                viewer.spawnParticle(Particle.BLOCK, effectLocation, 8, 0.25, 0.08, 0.25, 0.04,
                        block.getBlockData());
            }
            viewer.playSound(effectLocation, stepSound, 0.35f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !InvisibleMovementPolicy.shouldPlayFallSound(
                player.hasPotionEffect(PotionEffectType.INVISIBILITY), event.isCancelled(), event.getDamage())) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(player)) return;
        boolean bigFall = event.getDamage() >= 4.0D;
        Sound fallSound = Sound.valueOf(legacyServer()
                ? (bigFall ? "FALL_BIG" : "FALL_SMALL")
                : (bigFall ? "ENTITY_PLAYER_BIG_FALL" : "ENTITY_PLAYER_SMALL_FALL"));
        Location location = player.getLocation();
        for (Player viewer : arena.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(location) > SOUND_RADIUS_SQUARED) continue;
            viewer.playSound(location, fallSound, 0.8f, 1.0f);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSprintEffectAt.remove(event.getPlayer().getUniqueId());
    }

    static Sound sprintStepSound(Location location) {
        Material type = location.getBlock().getRelative(0, -1, 0).getType();
        String block = type.name();
        String legacy;
        String modern;
        if (block.contains("WOOL")) {
            legacy = "STEP_WOOL";
            modern = "BLOCK_WOOL_STEP";
        } else if (block.contains("WOOD") || block.contains("PLANK") || block.contains("LOG") || block.contains("STEM")) {
            legacy = "STEP_WOOD";
            modern = "BLOCK_WOOD_STEP";
        } else if (block.contains("SAND") || block.contains("GRAVEL")) {
            legacy = block.contains("GRAVEL") ? "STEP_GRAVEL" : "STEP_SAND";
            modern = block.contains("GRAVEL") ? "BLOCK_GRAVEL_STEP" : "BLOCK_SAND_STEP";
        } else if (block.contains("SNOW")) {
            legacy = "STEP_SNOW";
            modern = "BLOCK_SNOW_STEP";
        } else if (block.contains("GRASS") || block.contains("DIRT") || block.contains("FARMLAND")
                || block.contains("MYCELIUM") || block.contains("PODZOL") || block.equals("SOIL")) {
            legacy = "STEP_GRASS";
            modern = "BLOCK_GRASS_STEP";
        } else {
            legacy = "STEP_STONE";
            modern = "BLOCK_STONE_STEP";
        }
        return Sound.valueOf(legacyServer() ? legacy : modern);
    }

    private static boolean legacyServer() {
        return BedWars.nms != null && BedWars.nms.getName().contains("1_8");
    }
}
