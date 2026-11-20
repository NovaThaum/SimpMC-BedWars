package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FireballSettlementTest {
    @Test void fixedBalanceDamagesAndCreditsOnlyEnemiesWhilePushingAllActivePlayers() {
        com.andrei1058.bedwars.configuration.MainConfig previousConfig = BedWars.config;
        BedWars.config = mock(com.andrei1058.bedwars.configuration.MainConfig.class);
        VersionSupport previous = BedWars.nms; BedWars.nms = mock(VersionSupport.class);
        Player shooter = player(), teammate = player(), enemy = player(), spectator = player(), outsider = player();
        World world = mock(World.class); Location explosion = new Location(world, 0, 64, 0);
        for (Player p : List.of(shooter, teammate, enemy, spectator, outsider))
            when(p.getLocation()).thenReturn(new Location(world, 1, 64, 0));
        PlayerFireball fireball = mock(PlayerFireball.class); when(fireball.getShooter()).thenReturn(shooter);
        when(fireball.getUniqueId()).thenReturn(UUID.randomUUID());
        when(world.getNearbyEntities(any(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(shooter, teammate, enemy, spectator, outsider));
        IArena arena = mock(IArena.class); ITeam team = mock(ITeam.class);
        when(arena.getTeam(shooter)).thenReturn(team); when(arena.getTeam(teammate)).thenReturn(team);
        for (Player p : List.of(shooter, teammate, enemy)) when(arena.isPlayer(p)).thenReturn(true);
        FireballListener listener = new FireballListener();
        try (var arenas = mockStatic(Arena.class)) {
            for (Player p : List.of(shooter, teammate, enemy, spectator)) arenas.when(() -> Arena.getArenaByPlayer(p)).thenReturn(arena);
            doAnswer(call -> {
                var custom = new EntityDamageByEntityEvent(fireball, enemy, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, 3.5D);
                listener.fireballDirectHit(custom); assertFalse(custom.isCancelled()); return null;
            }).when(BedWars.nms).damageFireball(enemy, fireball, 3.5D, true);
            listener.fireballExplode(new EntityExplodeEvent(fireball, explosion, new ArrayList<>(), 0F));
            verify(BedWars.nms).damageFireball(shooter, fireball, 2D, false);
            verify(BedWars.nms).damageFireball(enemy, fireball, 3.5D, true);
            verify(BedWars.nms, never()).damageFireball(eq(teammate), any(), anyDouble(), anyBoolean());
            for (Player p : List.of(shooter, teammate, enemy)) verify(p).setVelocity(any());
            verify(spectator, never()).setVelocity(any()); verify(outsider, never()).setVelocity(any());
            assertNull(LastHit.getLastHit(shooter)); assertNull(LastHit.getLastHit(teammate));
            assertSame(shooter, LastHit.getLastHit(enemy).getDamager());
            for (var cause : List.of(EntityDamageEvent.DamageCause.PROJECTILE, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
                var nativeDamage = new EntityDamageByEntityEvent(fireball, enemy, cause, 20D);
                listener.fireballDirectHit(nativeDamage); assertTrue(nativeDamage.isCancelled());
            }
            var prime = new ExplosionPrimeEvent(fireball, 9F, true); listener.fireballPrime(prime);
            assertEquals(3.25F, prime.getRadius()); assertFalse(prime.getFire());
        } finally {
            LastHit hit = LastHit.getLastHit(enemy); if (hit != null) hit.remove(); BedWars.nms = previous;
            BedWars.config = previousConfig;
        }
    }

    private Player player() {
        Player player = mock(Player.class); when(player.getUniqueId()).thenReturn(UUID.randomUUID()); return player;
    }

    // Bukkit 1.8 also declares the deprecated LivingEntity getter. This covariant
    // override gives Mockito concrete bridge methods for both JVM descriptors.
    interface PlayerFireball extends Fireball {
        @Override Player getShooter();
    }
}
