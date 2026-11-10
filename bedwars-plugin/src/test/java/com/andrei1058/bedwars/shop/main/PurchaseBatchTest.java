package com.andrei1058.bedwars.shop.main;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PurchaseBatchTest {
    @Test
    public void buysOnlyWholeAffordableUnits() {
        assertEquals(3, PurchaseBatch.affordablePurchases(11, 3));
        assertEquals(0, PurchaseBatch.affordablePurchases(2, 3));
        assertEquals(0, PurchaseBatch.affordablePurchases(20, 0));
    }

    @Test
    public void splitsBulkPurchaseIntoLegalStacks() {
        assertArrayEquals(new int[]{64, 64, 2}, PurchaseBatch.stackAmounts(1, 130, 64));
        ItemStack[] stacks = PurchaseBatch.createStacks(new ItemStack(Material.IRON_INGOT), 130);
        assertEquals(3, stacks.length);
        assertEquals(64, stacks[0].getAmount());
        assertEquals(2, stacks[2].getAmount());
    }
}
