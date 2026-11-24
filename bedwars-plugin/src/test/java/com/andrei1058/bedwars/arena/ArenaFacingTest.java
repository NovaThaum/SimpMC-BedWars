package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArenaFacingTest {
    @Test void cardinalRoundingMatchesModernVersionAtBoundariesAndWrappedAngles() {
        double[] inputs = {44.99, 45, -45, -45.01, 135, -135, 181, 540, -540, 719, Double.NaN};
        float[] expected = {0, 90, 0, -90, 180, -90, -180, 180, -180, 0, 0};
        for (int i = 0; i < inputs.length; i++)
            assertEquals(expected[i], ConfigManager.snapYawToCardinal(inputs[i]), "yaw=" + inputs[i]);
        assertEquals(Float.floatToIntBits(0.0F), Float.floatToIntBits(ConfigManager.snapYawToCardinal(-1)));
    }

    @Test void explicitNpcFacingWinsOverSpawnDirectionAndResetsPitch() {
        Location npc = new Location(null, 0.5, 64, 0.5, 180, 60);
        Location spawn = new Location(null, 10.5, 64, 0.5);
        assertSame(npc, NpcFacing.apply(npc, 46, spawn));
        assertEquals(90, npc.getYaw());
        assertEquals(0, npc.getPitch());
        assertEquals(0.5, npc.getX());
        assertEquals(-90, NpcFacing.apply(npc, null, spawn).getYaw());
    }

    @Test void missingNpcFacingLooksTowardSpawnInAllFourDirections() {
        assertEquals(-90, NpcFacing.toward(0, 0, 10, 0));
        assertEquals(90, NpcFacing.toward(0, 0, -10, 0));
        assertEquals(0, NpcFacing.toward(0, 0, 0, 10));
        assertEquals(-180, NpcFacing.toward(0, 0, 0, -10));
        assertEquals(0, NpcFacing.toward(1, 1, 1, 1));
        assertNull(NpcFacing.apply(null, null, null));
    }

    @Test void playerFacingSurvivesSerializationAndKeepsLocationUnchanged() {
        Location location = new Location(null, -1.5, 64, 2.5, 100, -30);
        assertEquals("90.0,0.0", PlayerFacing.serialize(location));
        PlayerFacing.apply(location, "[-135, 40]");
        assertEquals(-90, location.getYaw());
        assertEquals(0, location.getPitch());
        assertEquals(-1.5, location.getX());
        assertDoesNotThrow(() -> PlayerFacing.apply(location, ","));
    }
}
