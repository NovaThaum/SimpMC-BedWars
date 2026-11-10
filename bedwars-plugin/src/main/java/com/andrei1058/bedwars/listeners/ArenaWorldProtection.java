package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

/** 阻止未登记玩家通过外部传送进入对局并操作世界。 */
public final class ArenaWorldProtection implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null || isEditing(event.getPlayer(), to.getWorld().getName())) return;
        IArena destination = Arena.getArenaByIdentifier(to.getWorld().getName());
        if (destination != null && Arena.getArenaByPlayer(event.getPlayer()) != destination) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (isUnregistered(event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (isUnregistered(event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
        onEntityInteract(event);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player && isUnregistered((Player) event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (isUnregistered(event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (isUnregistered(event.getPlayer())) event.setCancelled(true);
    }
    private static boolean isUnregistered(Player player) {
        String world = player.getWorld().getName();
        if (isEditing(player, world)) return false;
        IArena arena = Arena.getArenaByIdentifier(world);
        return arena != null && Arena.getArenaByPlayer(player) != arena;
    }
    private static boolean isEditing(Player player, String world) {
        SetupSession session = SetupSession.getSession(player.getUniqueId());
        return session != null && world.equalsIgnoreCase(session.getWorldName());
    }
}
