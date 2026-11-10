package com.andrei1058.bedwars.listeners;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameRules;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
/** Applies BedWars world rules when managed worlds load. */
public final class BedWarsWorldEnvironment implements Listener {
    @EventHandler public void onLoad(WorldLoadEvent event) { apply(event.getWorld()); }
    @EventHandler public void onWeather(WeatherChangeEvent event) { if (managed(event.getWorld())) event.setCancelled(true); }
    public static void apply(World world) { if (!managed(world)) return; if (isLobby(world)) GameRules.enforceLobbyEnvironment(world); else GameRules.enforceArenaEnvironment(world); }
    private static boolean managed(World world) { return world != null && (Arena.getArenaByIdentifier(world.getName()) != null || world.getName().equalsIgnoreCase(BedWars.getLobbyWorld()) || BedWars.getServerType() == com.andrei1058.bedwars.api.server.ServerType.BUNGEE); }
    private static boolean isLobby(World world) { return world.getName().equalsIgnoreCase(BedWars.getLobbyWorld()) || Arena.getArenaByIdentifier(world.getName()) == null; }
}
