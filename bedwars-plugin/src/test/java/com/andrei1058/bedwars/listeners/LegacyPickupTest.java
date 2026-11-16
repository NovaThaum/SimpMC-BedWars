package com.andrei1058.bedwars.listeners;
import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.*;
import com.andrei1058.bedwars.api.server.ServerType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyPickupTest {
    @Test void arenaMembershipTakesPriorityOverLobbyWorldForPickupAndDrop() {
        BedWars api = mock(BedWars.class, RETURNS_DEEP_STUBS);
        Player p = mock(Player.class); World world = mock(World.class); Item item = mock(Item.class);
        when(world.getName()).thenReturn("shared"); when(p.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(api.getServerType()).thenReturn(ServerType.MULTIARENA); when(api.getLobbyWorld()).thenReturn("shared");
        IArena arena = mock(IArena.class); when(api.getArenaUtil().getArenaByPlayer(p)).thenReturn(arena);
        when(arena.isPlayer(p)).thenReturn(true); when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.getRespawnSessions()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
        ItemStack iron = mock(ItemStack.class); when(iron.getType()).thenReturn(Material.IRON_INGOT);
        when(item.getItemStack()).thenReturn(iron);
        PlayerDropPick_1_11Minus listener = new PlayerDropPick_1_11Minus(api);
        PlayerPickupItemEvent pickup = new PlayerPickupItemEvent(p, item, 0); listener.onPickup(pickup); assertFalse(pickup.isCancelled());
        PlayerDropItemEvent drop = new PlayerDropItemEvent(p, item); listener.onDrop(drop); assertFalse(drop.isCancelled());
        when(arena.isPlayer(p)).thenReturn(false);
        pickup = new PlayerPickupItemEvent(p, item, 0); listener.onPickup(pickup); assertTrue(pickup.isCancelled());
        when(api.getArenaUtil().getArenaByPlayer(p)).thenReturn(null);
        pickup = new PlayerPickupItemEvent(p, item, 0); listener.onPickup(pickup); assertTrue(pickup.isCancelled());
        drop = new PlayerDropItemEvent(p, item); listener.onDrop(drop); assertTrue(drop.isCancelled());
    }
}
