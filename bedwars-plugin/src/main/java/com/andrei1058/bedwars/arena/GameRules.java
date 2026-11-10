package com.andrei1058.bedwars.arena;
import org.bukkit.Difficulty;
import org.bukkit.World;
/** Common world rules shared by arena and lobby environments on 1.8. */
public final class GameRules {
    private GameRules() { }
    public static void set(World world, String rule, String value) { if (world != null && rule != null && value != null) world.setGameRuleValue(rule, value); }
    public static void enforceArenaEnvironment(World world) {
        if (world == null) return;
        set(world, "doDaylightCycle", "false"); set(world, "doWeatherCycle", "false"); set(world, "doMobSpawning", "false");
        set(world, "doFireTick", "false"); set(world, "mobGriefing", "false"); set(world, "reducedDebugInfo", "true");
        world.setStorm(false); world.setThundering(false); world.setTime(6000L);
    }
    public static void enforceLobbyEnvironment(World world) { enforceArenaEnvironment(world); if (world.getDifficulty() != Difficulty.PEACEFUL) world.setDifficulty(Difficulty.PEACEFUL); world.setPVP(false); }
}
