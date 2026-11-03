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

package com.andrei1058.bedwars.arena.despawnables;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.entity.Despawnable;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

public class TargetListener implements Listener {

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e){
        if (!(e.getTarget() instanceof Player)) return;
        IArena arena = Arena.getArenaByIdentifier(e.getEntity().getWorld().getName());
        Player p = (Player) e.getTarget();
        if (arena == null) return;
        Despawnable despawnable = BedWars.nms.getDespawnablesList().get(e.getEntity().getUniqueId());
        if (despawnable != null) {
            ITeam ownerTeam = despawnable.getTeam();
            IArena ownerArena = ownerTeam == null ? null : ownerTeam.getArena();
            Player target = findTarget(ownerArena, ownerTeam, e.getEntity().getLocation());
            if (e.getTarget() != target) e.setTarget(target);
            return;
        }
        if (!arena.isPlayer(p)) {
            e.setCancelled(true);
            return;
        }
        if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            e.setCancelled(true);
            return;
        }
        if (arena.getStatus() != GameState.playing){
            e.setCancelled(true);
            return;
        }
        if (BedWars.nms.isDespawnable(e.getEntity())){
            if (arena.getTeam(p) == BedWars.nms.getDespawnablesList().get(e.getEntity().getUniqueId()).getTeam()){
                e.setCancelled(true);
            }
        }
    }

    private Player findTarget(IArena arena, ITeam ownerTeam, Location source) {
        if (arena == null || ownerTeam == null || arena.getStatus() != GameState.playing) return null;
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player candidate : arena.getPlayers()) {
            if (candidate.isDead() || !arena.isPlayer(candidate) || arena.isSpectator(candidate)
                    || arena.isReSpawning(candidate) || candidate.hasPotionEffect(PotionEffectType.INVISIBILITY)
                    || candidate.getWorld() != source.getWorld() || arena.getTeam(candidate) == ownerTeam) continue;
            double distance = candidate.getLocation().distanceSquared(source);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
