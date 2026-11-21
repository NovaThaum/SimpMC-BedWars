package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.VersionSupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameEndItemInteractionTest {
    @Test void rightClickDispatchesLeaveEvenWhenArenaProtectionHasCancelledInteraction() {
        VersionSupport previous = BedWars.nms;
        BedWars.nms = mock(VersionSupport.class);
        Player player = mock(Player.class);
        ItemStack item = new ItemStack(Material.BED);
        when(BedWars.nms.getItemInHand(player)).thenReturn(item);
        when(BedWars.nms.isCustomBedWarsItem(item)).thenReturn(true);
        when(BedWars.nms.getCustomData(item)).thenReturn("RUNCOMMAND_bw leave");
        try (var bukkit = mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            doAnswer(call -> { call.<Runnable>getArgument(1).run(); return null; })
                    .when(scheduler).runTask(any(), any(Runnable.class));
            for (Action action : new Action[]{Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK}) {
                PlayerInteractEvent event = mock(PlayerInteractEvent.class);
                when(event.getPlayer()).thenReturn(player);
                when(event.getAction()).thenReturn(action);
                when(event.isCancelled()).thenReturn(true);
                new Interact().onItemCommand(event);
                verify(event).setCancelled(true);
            }
            bukkit.verify(() -> Bukkit.dispatchCommand(player, "bw leave"), times(2));
        } finally { BedWars.nms = previous; }
    }
}
