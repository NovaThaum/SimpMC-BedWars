package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import org.bukkit.Location;

public final class PlayerFacing {
    private PlayerFacing() {
    }

    public static String serialize(Location location) {
        return ConfigManager.snapYawToCardinal(location.getYaw()) + ",0.0";
    }

    public static String serialize(float yaw, float pitch) {
        return ConfigManager.snapYawToCardinal(yaw) + ",0.0";
    }

    public static Location apply(Location location, String facing) {
        if (location == null) return null;
        location.setYaw(ConfigManager.snapYawToCardinal(location.getYaw()));
        location.setPitch(0.0F);
        if (facing == null || facing.isBlank()) return location;
        try {
            location.setYaw(ConfigManager.snapYawToCardinal(Float.parseFloat(
                    facing.replace("[", "").replace("]", "").split(",", -1)[0].trim())));
        } catch (IllegalArgumentException ignored) {
        }
        return location;
    }
}
