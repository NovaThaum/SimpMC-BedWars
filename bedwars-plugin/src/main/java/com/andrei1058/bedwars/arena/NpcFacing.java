package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import org.bukkit.Location;

public final class NpcFacing {
    private NpcFacing() {
    }

    public static float normalize(float yaw) {
        return ConfigManager.snapYawToCardinal(yaw);
    }

    public static float toward(double fromX, double fromZ, double targetX, double targetZ) {
        double deltaX = targetX - fromX;
        double deltaZ = targetZ - fromZ;
        if (Math.abs(deltaX) < 1.0E-9 && Math.abs(deltaZ) < 1.0E-9) return 0.0F;
        return normalize((float) Math.toDegrees(Math.atan2(-deltaX, deltaZ)));
    }

    public static Location apply(Location npc, Number configuredYaw, Location fallbackTarget) {
        if (npc == null) return null;
        float yaw = configuredYaw == null
                ? fallbackTarget == null ? 0.0F
                : toward(npc.getX(), npc.getZ(), fallbackTarget.getX(), fallbackTarget.getZ())
                : normalize(configuredYaw.floatValue());
        npc.setYaw(yaw);
        npc.setPitch(0.0F);
        return npc;
    }
}
