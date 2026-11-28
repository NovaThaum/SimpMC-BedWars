package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** 结算中的活动玩家使用现有命令物品路径返回大厅。 */
public final class GameEndItems {
    private GameEndItems() {
    }

    public static void give(Player player) {
        if (player == null || !player.isOnline()) return;
        player.closeInventory();
        player.getInventory().clear();
        ItemStack leave = Misc.createItem(Material.RED_BED, (byte) 0, false,
                Language.getMsg(player, Messages.ARENA_SPECTATOR_LEAVE_ITEM_NAME),
                Language.getList(player, Messages.ARENA_SPECTATOR_LEAVE_ITEM_LORE),
                player, "RUNCOMMAND", BedWars.mainCmd + " leave");
        player.getInventory().setItem(8, leave);
        player.updateInventory();
    }
}
