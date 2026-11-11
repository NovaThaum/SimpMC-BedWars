package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/** 只检查已加载区块；1.8 没有爬行姿势，因此必须有两格安全站立空间。 */
public final class SafeSpawnResolver {
    private SafeSpawnResolver() { }
    public static Location resolve(Location configured) {
        if (configured == null || configured.getWorld() == null) return null;
        World world = configured.getWorld();
        int[] heights = {0, 1, -1, 2, -2};
        for (int radius = 0; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : heights) {
                        int x = configured.getBlockX() + dx, y = configured.getBlockY() + dy, z = configured.getBlockZ() + dz;
                        if (y < 1 || y + 1 >= world.getMaxHeight() || !world.isChunkLoaded(x >> 4, z >> 4)) continue;
                        Block floor = world.getBlockAt(x, y - 1, z);
                        if (!fullFloor(floor.getType()) || unsafe(floor.getType())
                                || !clear(world.getBlockAt(x, y, z)) || !clear(world.getBlockAt(x, y + 1, z))) continue;
                        Location destination = new Location(world, x + 0.5, y, z + 0.5, configured.getYaw(), configured.getPitch());
                        Location center = world.getWorldBorder().getCenter();
                        double half = world.getWorldBorder().getSize() / 2.0 - 0.3;
                        if (Math.abs(destination.getX() - center.getX()) <= half
                                && Math.abs(destination.getZ() - center.getZ()) <= half) return destination;
                    }
                }
            }
        }
        return null;
    }
    private static boolean clear(Block block) { return block.getType() == Material.AIR; }
    private static boolean fullFloor(Material material) {
        // 栅栏、楼梯和半砖虽为 solid，其顶面却不是 y+1，不能作为本解析器的落脚面。
        return material.isOccluding() || material == Material.GLASS || material == Material.STAINED_GLASS
                || material == Material.SLIME_BLOCK;
    }
    private static boolean unsafe(Material material) {
        return material == Material.CACTUS || material == Material.FIRE || material == Material.LAVA
                || material == Material.STATIONARY_LAVA || material == Material.WATER
                || material == Material.STATIONARY_WATER;
    }
}
