package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameRules;
import com.andrei1058.bedwars.arena.SetupSession;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;

/** 只初始化受管理世界，保留共存世界原有环境。 */
public final class BedWarsWorldEnvironment implements Listener {
    @EventHandler public void onLoad(WorldLoadEvent event) { apply(event.getWorld()); }
    @EventHandler public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState() && isManagedWorld(event.getWorld())) event.setCancelled(true);
    }
    @EventHandler public void onThunder(ThunderChangeEvent event) {
        if (event.toThunderState() && isManagedWorld(event.getWorld())) event.setCancelled(true);
    }
    public static void apply(World world) {
        if (world == null || isSetupWorld(world)) return;
        if (isArenaWorld(world)) GameRules.enforceArenaEnvironment(world);
        else if (isLobbyWorld(world)) GameRules.enforceLobbyEnvironment(world);
    }
    static boolean isManagedWorld(World world) {
        return world != null && !isSetupWorld(world) && (isArenaWorld(world) || isLobbyWorld(world));
    }
    static boolean isManagedWorldName(String worldName, String lobbyName, boolean arenaWorld) {
        return arenaWorld || isLobbyWorldName(worldName, lobbyName);
    }
    static boolean isLobbyWorld(World world) {
        if (world == null || isSetupWorld(world) || isArenaWorld(world)) return false;
        String lobby = BedWars.getLobbyWorld();
        if (isLobbyWorldName(world.getName(), lobby)) return true;
        // 未设置大厅时，仅 MULTIARENA 的离场逻辑使用首世界作为备用大厅。
        return (lobby == null || lobby.isEmpty()) && BedWars.getServerType() == ServerType.MULTIARENA
                && !Bukkit.getWorlds().isEmpty() && world.equals(Bukkit.getWorlds().get(0));
    }
    static boolean isLobbyWorldName(String worldName, String lobbyName) {
        return worldName != null && lobbyName != null && !lobbyName.isEmpty()
                && worldName.equalsIgnoreCase(lobbyName);
    }
    private static boolean isArenaWorld(World world) {
        if (Arena.getArenaByIdentifier(world.getName()) != null) return true;
        for (IArena arena : Arena.getEnableQueue()) {
            if (world.getName().equalsIgnoreCase(arena.getWorldName())) return true;
        }
        return false;
    }
    private static boolean isSetupWorld(World world) {
        for (SetupSession session : SetupSession.getSetupSessions()) {
            if (world.getName().equalsIgnoreCase(session.getWorldName())) return true;
        }
        return false;
    }
}
