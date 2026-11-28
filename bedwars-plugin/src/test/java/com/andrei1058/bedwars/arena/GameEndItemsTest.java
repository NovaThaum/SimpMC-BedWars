package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.api.messages.Messages;
import com.andrei1058.bedwars.arena.tasks.GameRestartingTask;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.configuration.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class GameEndItemsTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;
    @Test void givesUsableLeaveCommandInLastHotbarSlotWithoutChangingPlayerState() {
        String previousCommand = BedWars.mainCmd;
        BedWars.mainCmd = "bw";
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack item = new ItemStack(Material.BED);
        List<String> lore = List.of("右键返回大厅");
        try (var messages = mockStatic(ChineseMessages.class); var items = mockStatic(Misc.class)) {
            messages.when(() -> ChineseMessages.getMsg(player, Messages.ARENA_SPECTATOR_LEAVE_ITEM_NAME)).thenReturn("回到大厅");
            messages.when(() -> ChineseMessages.getList(player, Messages.ARENA_SPECTATOR_LEAVE_ITEM_LORE)).thenReturn(lore);
            items.when(() -> Misc.createItem(Material.BED, (byte) 0, false, "回到大厅", lore, player,
                    "RUNCOMMAND", "bw leave")).thenReturn(item);
            GameEndItems.give(player);
            var order = inOrder(player, inventory);
            order.verify(player).closeInventory();
            order.verify(inventory).clear();
            order.verify(inventory).setItem(8, item);
            verify(player).updateInventory();
            verify(player, never()).setGameMode(any());
        } finally { BedWars.mainCmd = previousCommand; }
    }

    @Test void settlementGivesEveryRemainingParticipantTheExitItem() {
        MainConfig previous = BedWars.config;
        BedWars previousPlugin = BedWars.plugin;
        BedWars.plugin = mock(BedWars.class);
        when(BedWars.plugin.getDataFolder()).thenReturn(directory.toFile());
        when(BedWars.plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        BedWars.config = mock(MainConfig.class);
        try (var bukkit = mockStatic(Bukkit.class); var sounds = mockStatic(Sounds.class);
             var items = mockStatic(GameEndItems.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
            Arena arena = mock(Arena.class, RETURNS_DEEP_STUBS);
            Player winner = mock(Player.class), teammate = mock(Player.class), spectator = mock(Player.class);
            when(winner.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
            when(arena.getPlayers()).thenReturn(List.of(winner, teammate));
            when(arena.getSpectators()).thenReturn(List.of(spectator));
            when(arena.getWorld().getEntities()).thenReturn(List.of());
            new GameRestartingTask(arena);
            new com.andrei1058.bedwars.listeners.GameEndListener().cleanInventoriesAndDroppedItems(
                    new com.andrei1058.bedwars.api.events.gameplay.GameEndEvent(arena, List.of(), List.of(), null, List.of(winner.getUniqueId())));
            verify(winner, never()).getInventory();
            items.verify(() -> GameEndItems.give(winner));
            items.verify(() -> GameEndItems.give(teammate));
            items.verify(() -> GameEndItems.give(spectator), never());
        } finally { BedWars.config = previous; BedWars.plugin = previousPlugin; }
    }
}
