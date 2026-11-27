package com.andrei1058.bedwars.support.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class JoinNpcDefinitionTest {
  private JoinNpcDefinition definition(String skin, String text) {
    return new JoinNpcDefinition(UUID.randomUUID(), "lobby", 1, 2, 3, 90, 0, skin, "Solo", text);
  }

  @Test
  void rendersScreenshotFormatAndEveryLine() {
    assertEquals(
        List.of("§9§l单人模式", "§f12 人正在游戏", "§e右键加入"),
        definition("andrei1058", "&9&l单人模式\\n&f{players} 人正在游戏\\n&e右键加入").rendered(12));
  }

  @Test
  void acceptsLiteralNewlinesAndPreservesText() {
    assertEquals(List.of("第一行", "第二行"), definition("Player_1", "第一行\n第二行").rendered(0));
  }

  @Test
  void supportsNpcWithoutFloatingText() {
    assertTrue(definition("Player_1", "").rendered(0).isEmpty());
    assertTrue(JoinNpcDefinition.lines("   ").isEmpty());
  }

  @Test
  void rejectsUnsafeSkinNamesAndExcessiveText() {
    assertThrows(IllegalArgumentException.class, () -> definition("../name", "文字"));
    assertThrows(IllegalArgumentException.class, () -> definition("玩家", "文字"));
    assertThrows(IllegalArgumentException.class, () -> definition("Steve", "x".repeat(129)));
    assertThrows(
        IllegalArgumentException.class,
        () -> definition("Steve", String.join("\\n", java.util.Collections.nCopies(7, "行"))));
  }

  @Test
  void selectsOnlyTargetInFrontWithinReachAndWorld() {
    World w = mock(World.class), other = mock(World.class);
    Location eye = new Location(w, 0, 1.62, 0, 0, 0);
    assertTrue(Double.isFinite(JoinNpcDefinition.targetDistance(eye, new Location(w, 0, 0, 3))));
    assertEquals(
        Double.POSITIVE_INFINITY, JoinNpcDefinition.targetDistance(eye, new Location(w, 0, 0, -3)));
    assertEquals(
        Double.POSITIVE_INFINITY, JoinNpcDefinition.targetDistance(eye, new Location(w, 0, 0, 7)));
    assertEquals(
        Double.POSITIVE_INFINITY, JoinNpcDefinition.targetDistance(eye, new Location(w, 2, 0, 3)));
    assertEquals(
        Double.POSITIVE_INFINITY,
        JoinNpcDefinition.targetDistance(eye, new Location(other, 0, 0, 3)));
  }

  @Test
  void rejectsNonfiniteCoordinates() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JoinNpcDefinition(
                UUID.randomUUID(), "lobby", Double.NaN, 0, 0, 0, 0, "Steve", "Solo", "标题"));
  }
}
