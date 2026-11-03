package com.andrei1058.bedwars.listeners;

final class InvisibleMovementPolicy {
    static final long SPRINT_EFFECT_INTERVAL_MILLIS = 250L;

    private InvisibleMovementPolicy() {
    }

    static boolean shouldPlaySprintEffects(boolean invisible, boolean sprinting, boolean grounded,
                                           double horizontalDistanceSquared, Long lastEffectAt, long now) {
        if (!invisible || !sprinting || !grounded || horizontalDistanceSquared < 0.0001D) return false;
        return lastEffectAt == null || now - lastEffectAt >= SPRINT_EFFECT_INTERVAL_MILLIS;
    }

    static boolean shouldPlayFallSound(boolean invisible, boolean cancelled, double damage) {
        return invisible && !cancelled && damage > 0.0D;
    }
}
