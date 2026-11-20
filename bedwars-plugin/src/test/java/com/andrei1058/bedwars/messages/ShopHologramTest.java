package com.andrei1058.bedwars.messages;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.shop.ShopHolo;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopHologramTest {
    @Test void oneChineseHologramPerShopAndCleanupRemovesItsEntities() {
        IArena arena = mock(IArena.class);
        ArmorStand first = mock(ArmorStand.class), second = mock(ArmorStand.class), duplicate = mock(ArmorStand.class);
        Location location = new Location(null, 1, 64, 2);
        int before = ShopHolo.getShopHolo().size();
        try {
            new ShopHolo(first, second, location, arena);
            new ShopHolo(duplicate, null, location.clone(), arena);
            assertEquals(before + 1, ShopHolo.getShopHolo().size());
            verify(duplicate).remove();
            verify(first, never()).remove();
            ShopHolo.clearForArena(arena);
            assertEquals(before, ShopHolo.getShopHolo().size());
            verify(first).remove(); verify(second).remove();
        } finally { ShopHolo.clearForArena(arena); }
    }
}
