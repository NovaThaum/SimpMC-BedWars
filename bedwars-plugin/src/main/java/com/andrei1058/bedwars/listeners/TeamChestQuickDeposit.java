package com.andrei1058.bedwars.listeners;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Calculates how much of a held stack was accepted by a team chest. */
final class TeamChestQuickDeposit {
    private TeamChestQuickDeposit() { }

    static int transferredAmount(int offeredAmount, Map<Integer, ItemStack> leftovers) {
        long remaining = 0;
        if (leftovers != null) {
            for (ItemStack item : leftovers.values()) {
                if (item != null && item.getAmount() > 0) remaining += item.getAmount();
            }
        }
        return transferredAmount(offeredAmount, remaining);
    }

    static int transferredAmount(int offeredAmount, long leftoverAmount) {
        if (offeredAmount <= 0) return 0;
        return (int) Math.max(0L, Math.min((long) offeredAmount, offeredAmount - leftoverAmount));
    }
}
