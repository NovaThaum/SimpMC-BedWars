package com.andrei1058.bedwars.listeners;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BreakPlaceFireballTest {

    @Test
    public void fireballsProtectMapStructureBedsAndDefensiveMaterials() {
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, Material.WOOL, false, false));
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, Material.WOOL, true, true));
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, Material.ENDER_STONE, true, false));
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, Material.HARD_CLAY, true, false));
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, Material.STAINED_CLAY, true, false));
    }

    @Test
    public void leavesOtherExplosionsAndBreakableBlocksAlone() {
        assertFalse(BreakPlace.shouldProtectExplosionBlock(true, Material.WOOL, true, false));
        assertFalse(BreakPlace.shouldProtectExplosionBlock(false, Material.ENDER_STONE, false, false));
        assertFalse(BreakPlace.shouldProtectExplosionBlock(true, null, true, false));
    }
}
