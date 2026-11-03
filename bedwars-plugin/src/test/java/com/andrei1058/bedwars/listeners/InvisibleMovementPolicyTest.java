package com.andrei1058.bedwars.listeners;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvisibleMovementPolicyTest {
    @Test
    public void sprintEffectsOnlyPlayForGroundedInvisibleSprintMovement() {
        assertTrue(InvisibleMovementPolicy.shouldPlaySprintEffects(true, true, true, 0.01, null, 1000));
        assertFalse(InvisibleMovementPolicy.shouldPlaySprintEffects(false, true, true, 0.01, null, 1000));
        assertFalse(InvisibleMovementPolicy.shouldPlaySprintEffects(true, false, true, 0.01, null, 1000));
        assertFalse(InvisibleMovementPolicy.shouldPlaySprintEffects(true, true, false, 0.01, null, 1000));
        assertFalse(InvisibleMovementPolicy.shouldPlaySprintEffects(true, true, true, 0.0, null, 1000));
    }

    @Test
    public void sprintEffectsAreRateLimitedPerPlayer() {
        assertFalse(InvisibleMovementPolicy.shouldPlaySprintEffects(true, true, true, 0.01, 1000L, 1200));
        assertTrue(InvisibleMovementPolicy.shouldPlaySprintEffects(true, true, true, 0.01, 1000L, 1250));
    }

    @Test
    public void fallSoundRequiresUncancelledFallDamageWhileInvisible() {
        assertTrue(InvisibleMovementPolicy.shouldPlayFallSound(true, false, 1.0));
        assertFalse(InvisibleMovementPolicy.shouldPlayFallSound(false, false, 1.0));
        assertFalse(InvisibleMovementPolicy.shouldPlayFallSound(true, true, 1.0));
        assertFalse(InvisibleMovementPolicy.shouldPlayFallSound(true, false, 0.0));
    }
}
