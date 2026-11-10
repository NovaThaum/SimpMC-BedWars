package com.andrei1058.bedwars.shop.main;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Pure stack calculations for the shop's buy-to-limit action. */
final class PurchaseBatch {
    private PurchaseBatch() { }

    static int affordablePurchases(int availableCurrency, int unitPrice) {
        if (unitPrice <= 0) return 0;
        return availableCurrency / unitPrice;
    }

    static ItemStack[] createStacks(ItemStack template, int purchases) {
        if (template == null || template.getType() == org.bukkit.Material.AIR || template.getAmount() <= 0 || purchases <= 0) {
            return new ItemStack[0];
        }
        int max = Math.max(1, template.getType().getMaxStackSize());
        long total = (long) template.getAmount() * purchases;
        long count = (total + max - 1) / max;
        if (count > Integer.MAX_VALUE) throw new IllegalArgumentException("too many purchase stacks");
        List<ItemStack> result = new ArrayList<>((int) count);
        while (total > 0) {
            ItemStack stack = template.clone();
            stack.setAmount((int) Math.min(max, total));
            result.add(stack);
            total -= stack.getAmount();
        }
        return result.toArray(new ItemStack[0]);
    }

    static int[] stackAmounts(int itemAmount, int purchases, int maxStackSize) {
        if (itemAmount <= 0 || purchases <= 0 || maxStackSize <= 0) return new int[0];
        long total = (long) itemAmount * purchases;
        List<Integer> amounts = new ArrayList<>();
        while (total > 0) {
            int amount = (int) Math.min(maxStackSize, total);
            amounts.add(amount);
            total -= amount;
        }
        int[] result = new int[amounts.size()];
        for (int i = 0; i < result.length; i++) result[i] = amounts.get(i);
        return result;
    }
}
