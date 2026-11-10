package com.andrei1058.bedwars.listeners;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
/** Stops ordinary players changing lobby inventories or using vanilla items. */
public final class LobbyProtection implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent event) { if (event.getWhoClicked() instanceof Player && protectedPlayer((Player) event.getWhoClicked())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drag(InventoryDragEvent event) { if (event.getWhoClicked() instanceof Player && protectedPlayer((Player) event.getWhoClicked())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void craft(PrepareItemCraftEvent event) { if (event.getView().getPlayer() instanceof Player && protectedPlayer((Player) event.getView().getPlayer())) event.getInventory().setResult(new ItemStack(Material.AIR)); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent event) { if (protectedPlayer(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void consume(PlayerItemConsumeEvent event) { if (protectedPlayer(event.getPlayer())) event.setCancelled(true); }
    private static boolean protectedPlayer(Player player) { return !player.isOp() && !BreakPlace.isBuildSession(player) && player.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld()) && Arena.getArenaByPlayer(player) == null; }
}
