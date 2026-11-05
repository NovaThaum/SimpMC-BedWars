package com.andrei1058.bedwars.listeners;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InvisibilityPotionListenerTest {
    @Test
    public void usesAmbientFlagForSparseInvisibilityParticles() {
        PotionEffect original = new PotionEffect(PotionEffectType.INVISIBILITY, 600, 1, false);
        PotionEffect ambient = InvisibilityPotionListener.ambientInvisibility(original);

        assertEquals(PotionEffectType.INVISIBILITY, ambient.getType());
        assertEquals(600, ambient.getDuration());
        assertEquals(1, ambient.getAmplifier());
        assertTrue(ambient.isAmbient());
    }
}
