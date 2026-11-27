package com.andrei1058.bedwars.support.npc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/** 内置大厅 NPC 的持久化配置，不依赖 Citizens。 */
public record JoinNpcDefinition(
    UUID id,
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    String skin,
    String group,
    String text) {
  public JoinNpcDefinition {
    if (id == null
        || world == null
        || world.isBlank()
        || skin == null
        || !skin.matches("[A-Za-z0-9_]{1,16}")
        || group == null
        || group.isBlank()
        || !Double.isFinite(x)
        || !Double.isFinite(y)
        || !Double.isFinite(z)
        || !Float.isFinite(yaw)
        || !Float.isFinite(pitch)
        || text == null
        || (!text.isBlank()
            && (lines(text).size() > 6 || lines(text).stream().anyMatch(s -> s.length() > 128))))
      throw new IllegalArgumentException("NPC 配置无效：皮肤名须为正版玩家名，悬浮文字最多 6 行、每行 128 字符。");
  }

  public static List<String> lines(String text) {
    if (text == null || text.isBlank()) return List.of();
    return Arrays.asList(text.replace("\\n", "\n").split("\n", -1));
  }

  public List<String> rendered(int players) {
    return lines(text).stream()
        .map(s ->
            ChatColor.translateAlternateColorCodes(
                '&', s.replace("{players}", Integer.toString(players))))
        .toList();
  }

  public Location location() {
    return new Location(org.bukkit.Bukkit.getWorld(world), x, y, z, yaw, pitch);
  }

  /** 视线到 NPC 身体中心的射线距离；背后或偏离视线的目标不命中。 */
  public static double targetDistance(Location eye, Location feet) {
    if (eye.getWorld() != feet.getWorld()) return Double.POSITIVE_INFINITY;
    Vector offset = feet.toVector().add(new Vector(0, 1, 0)).subtract(eye.toVector());
    double along = offset.dot(eye.getDirection());
    if (along < 0 || along > 6 || offset.lengthSquared() - along * along > .8 * .8)
      return Double.POSITIVE_INFINITY;
    return along;
  }
}
