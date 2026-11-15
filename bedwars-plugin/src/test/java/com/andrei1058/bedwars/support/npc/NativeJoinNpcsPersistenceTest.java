package com.andrei1058.bedwars.support.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andrei1058.bedwars.BedWars;
import java.nio.file.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeJoinNpcsPersistenceTest {
  @TempDir Path dir;

  private BedWars plugin() {
    BedWars p = mock(BedWars.class);
    when(p.getDataFolder()).thenReturn(dir.toFile());
    return p;
  }

  @SuppressWarnings("unchecked")
  private Map<UUID, JoinNpcDefinition> entries(NativeJoinNpcs m) throws Exception {
    var f = NativeJoinNpcs.class.getDeclaredField("definitions");
    f.setAccessible(true);
    return (Map<UUID, JoinNpcDefinition>) f.get(m);
  }

  @Test
  void savesAndRestoresWorldSkinGroupAndAllTextWithoutCitizens() throws Exception {
    try (var b = mockStatic(Bukkit.class)) {
      b.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
      World w = mock(World.class);
      when(w.getName()).thenReturn("lobby");
      NativeJoinNpcs first = new NativeJoinNpcs(plugin(), List::of);
      JoinNpcDefinition d =
          first.create(
              new Location(w, 1.25, 64, -3, 90, 10),
              "andrei1058",
              "Solo",
              "&9单人,模式\\n&f{players} 人");
      first.close();
      NativeJoinNpcs second = new NativeJoinNpcs(plugin(), List::of);
      assertEquals(d, entries(second).get(d.id()));
      b.when(() -> Bukkit.getWorld("lobby")).thenReturn(w);
      Player p = mock(Player.class);
      when(p.getWorld()).thenReturn(w);
      when(p.getEyeLocation()).thenReturn(new Location(w, 1.25, 65.62, -6, 0, 0));
      assertTrue(second.removeTarget(p));
      assertTrue(entries(second).isEmpty());
      second.close();
      b.when(() -> Bukkit.getWorld("lobby")).thenReturn(null);
      NativeJoinNpcs third = new NativeJoinNpcs(plugin(), List::of);
      assertTrue(entries(third).isEmpty());
      third.close();
    }
  }

  @Test
  void failedWriteRollsBackCreation() throws Exception {
    try (var b = mockStatic(Bukkit.class)) {
      b.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
      NativeJoinNpcs m = new NativeJoinNpcs(plugin(), List::of);
      Files.createDirectory(dir.resolve("join-npcs.yml"));
      Files.writeString(dir.resolve("join-npcs.yml/keep"), "保留");
      World w = mock(World.class);
      when(w.getName()).thenReturn("lobby");
      assertThrows(
          java.io.IOException.class,
          () -> m.create(new Location(w, 0, 64, 0), "Steve", "Solo", "标题"));
      assertTrue(entries(m).isEmpty());
      assertEquals("保留", Files.readString(dir.resolve("join-npcs.yml/keep")));
      m.close();
    }
  }

  @Test
  void invalidFileIsNeverOverwritten() throws Exception {
    Path f = dir.resolve("join-npcs.yml");
    String invalid = "npcs: [unclosed";
    Files.writeString(f, invalid);
    assertThrows(IllegalStateException.class, () -> new NativeJoinNpcs(plugin(), List::of));
    assertEquals(invalid, Files.readString(f));
  }

  @Test
  void wrongConfigurationShapeIsRejectedWithoutOverwrite() throws Exception {
    Path f = dir.resolve("join-npcs.yml");
    String invalid = "npcs: legacy-data\n";
    Files.writeString(f, invalid);
    assertThrows(IllegalStateException.class, () -> new NativeJoinNpcs(plugin(), List::of));
    assertEquals(invalid, Files.readString(f));
  }
}
