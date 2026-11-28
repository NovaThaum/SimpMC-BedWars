package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.nms;

public class FireballListener implements Listener {

    private static final double MIN_FIREBALL_EXPLOSION_SIZE = 0.1D;
    private static final double MAX_FIREBALL_EXPLOSION_SIZE = 16D;
    private static final double MAX_FIREBALL_KNOCKBACK = 8D;

    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;
    private final double fireballHorizontal;
    private final double fireballVertical;

    private final double damageSelf;
    private final double damageEnemy;
    private final Set<DamageKey> activeCustomExplosions = new HashSet<>();

    public FireballListener() {
        this.fireballExplosionSize = FireballSettings.EXPLOSION_SIZE;
        this.fireballMakeFire = false;
        this.fireballHorizontal = FireballSettings.HORIZONTAL_KNOCKBACK;
        this.fireballVertical = FireballSettings.VERTICAL_KNOCKBACK;
        this.damageSelf = FireballSettings.SELF_DAMAGE;
        this.damageEnemy = FireballSettings.ENEMY_DAMAGE;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void fireballExplode(EntityExplodeEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getEntity() instanceof Fireball fireball)) return;
        Location location = e.getLocation();

        ProjectileSource projectileSource = fireball.getShooter();
        if (!(projectileSource instanceof Player source)) return;

        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(source)) return;
        ITeam sourceTeam = arena.getTeam(source);

        World world = location.getWorld();
        if (world == null) return;
        Vector explosionPosition = location.toVector();
        Collection<org.bukkit.entity.Entity> nearbyPlayers = world.getNearbyEntities(location,
                fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);
        long hitTime = System.currentTimeMillis();
        for (org.bukkit.entity.Entity entity : nearbyPlayers) {
            if (!(entity instanceof Player player)) continue;
            if (Arena.getArenaByPlayer(player) != arena || !arena.isPlayer(player)) continue;
            Vector playerPosition = player.getLocation().toVector();
            if (!isWithinExplosionRadius(explosionPosition, playerPosition, fireballExplosionSize)) continue;

            player.setVelocity(calculateKnockback(explosionPosition, playerPosition,
                    fireballHorizontal, fireballVertical));

            boolean self = player.equals(source);
            boolean teammate = sourceTeam != null && sourceTeam.equals(arena.getTeam(player));
            if (shouldCreditShooter(self, teammate)) {
                LastHit hit = LastHit.getLastHit(player);
                if (hit == null) new LastHit(player, source, hitTime);
                else { hit.setDamager(source); hit.setTime(hitTime); }
            }

            if (self) {
                if (damageSelf > 0) {
                    nms.damageFireball(player, fireball, damageSelf, false); // damage shooter without kill credit
                }
            } else if (shouldDamageTarget(self, teammate) && damageEnemy > 0) {
                damageEnemy(player, fireball);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Fireball fireball)) return;
        if (!(e.getEntity() instanceof Player target)) return;
        // The custom area damage also names the fireball as its direct entity.
        // Only its synchronous, explicitly marked damage call is allowed through;
        // the projectile's native contact and native explosion damage stay cancelled.
        DamageKey damageKey = new DamageKey(fireball.getUniqueId(), target.getUniqueId());
        boolean explosionDamage = e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        if (!shouldCancelDirectHit(explosionDamage, activeCustomExplosions.contains(damageKey))) return;
        if (!(fireball.getShooter() instanceof Player source)) return;
        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null || Arena.getArenaByPlayer(target) != arena) return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void fireballPrime(ExplosionPrimeEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getEntity() instanceof Fireball fireball)) return;
        ProjectileSource shooter = fireball.getShooter();
        if (!(shooter instanceof Player player)) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(player)) return;

        e.setFire(fireballMakeFire);
        e.setRadius((float) fireballExplosionSize);
        // 1.8.8 原生火球在 mobGriefing=false 时不会发出 EntityExplodeEvent。
        // 只替换本次爆炸，保留火球来源和所有爆炸事件保护，不更改世界规则。
        e.setCancelled(true);
        nms.explodeFireball(fireball, e.getRadius(), e.getFire());
    }

    static Vector calculateKnockback(Vector explosion, Vector player, double horizontal, double vertical) {
        double safeHorizontal = boundedFiniteNonNegative(horizontal, 0D, MAX_FIREBALL_KNOCKBACK);
        double safeVertical = boundedFiniteNonNegative(vertical, 0D, MAX_FIREBALL_KNOCKBACK);
        Vector towardExplosion = explosion.clone().subtract(player);
        double lengthSquared = towardExplosion.lengthSquared();
        if (!Double.isFinite(lengthSquared) || lengthSquared <= 1.0E-12D) {
            return new Vector(0D, safeVertical * 1.5D, 0D);
        }

        towardExplosion.multiply(1D / Math.sqrt(lengthSquared));
        Vector knockback = horizontalKnockback(explosion, player, safeHorizontal);
        double y = towardExplosion.getY();
        if (y < 0) y += 1.5;
        y = y <= 0.5 ? safeVertical * 1.5D : y * safeVertical * 1.5D;
        return knockback.setY(y);
    }

    /**
     * Push the player away along the horizontal plane only, so the configured
     * horizontal strength is delivered in full regardless of how far above or
     * below the explosion the player stands.
     */
    private static Vector horizontalKnockback(Vector explosion, Vector player, double horizontal) {
        Vector away = new Vector(player.getX() - explosion.getX(), 0D, player.getZ() - explosion.getZ());
        double lengthSquared = away.lengthSquared();
        if (!Double.isFinite(lengthSquared) || lengthSquared <= 1.0E-12D) {
            return new Vector(0D, 0D, 0D);
        }
        return away.multiply(horizontal / Math.sqrt(lengthSquared));
    }

    static boolean isWithinExplosionRadius(Vector explosion, Vector player, double radius) {
        if (explosion == null || player == null || !Double.isFinite(radius) || radius < 0D) return false;
        double distanceSquared = explosion.distanceSquared(player);
        double radiusSquared = radius > Math.sqrt(Double.MAX_VALUE)
                ? Double.MAX_VALUE : radius * radius;
        return Double.isFinite(distanceSquared) && distanceSquared <= radiusSquared;
    }

    static double normalizeExplosionSize(double value) {
        return boundedFiniteNonNegative(value, MIN_FIREBALL_EXPLOSION_SIZE, MAX_FIREBALL_EXPLOSION_SIZE);
    }

    private static double boundedFiniteNonNegative(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.min(maximum, Math.max(minimum, value)) : minimum;
    }

    static boolean shouldCreditShooter(boolean self, boolean teammate) {
        return !self && !teammate;
    }

    static boolean shouldDamageTarget(boolean self, boolean teammate) {
        return !self && !teammate;
    }

    static boolean shouldCancelDirectHit(boolean explosionDamage, boolean activeCustomExplosion) {
        return !explosionDamage || !activeCustomExplosion;
    }

    private void damageEnemy(Player target, Fireball fireball) {
        DamageKey damageKey = new DamageKey(fireball.getUniqueId(), target.getUniqueId());
        boolean firstSettlement = activeCustomExplosions.add(damageKey);
        try {
            nms.damageFireball(target, fireball, damageEnemy, true);
        } finally {
            if (firstSettlement) activeCustomExplosions.remove(damageKey);
        }
    }

    private record DamageKey(UUID fireball, UUID target) {
    }

}
