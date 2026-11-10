package com.andrei1058.bedwars.listeners;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TeamChestQuickDepositTest {
    @Test
    public void calculatesFullAndPartialTransfers() {
        assertEquals(16, TeamChestQuickDeposit.transferredAmount(16, Collections.<Integer, ItemStack>emptyMap()));
        Map<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, new ItemStack(Material.IRON_INGOT, 5));
        assertEquals(11, TeamChestQuickDeposit.transferredAmount(16, leftovers));
    }

    @Test
    public void rejectsInvalidOrOversizedLeftovers() {
        assertEquals(0, TeamChestQuickDeposit.transferredAmount(16, 16));
        assertEquals(0, TeamChestQuickDeposit.transferredAmount(-1, 0));
        assertEquals(0, TeamChestQuickDeposit.transferredAmount(16, 20));
    }
}
