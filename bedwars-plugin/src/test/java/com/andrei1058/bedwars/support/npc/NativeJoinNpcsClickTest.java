package com.andrei1058.bedwars.support.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.Sounds;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeJoinNpcsClickTest {
  @TempDir Path dir;

  @SuppressWarnings("unchecked")
  @Test
  void clicksUseGroupMatchmakingAndRejectDuplicatesDistantAndHiddenPlayers() throws Exception {
    BedWars plugin = mock(BedWars.class);
    when(plugin.getDataFolder()).thenReturn(dir.toFile());
    when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
    BedWars previousPlugin = BedWars.plugin;
    BedWars.plugin = plugin;
    var previousConfig = BedWars.config;
    BedWars.config = mock(com.andrei1058.bedwars.configuration.MainConfig.class);
    try (var b = mockStatic(Bukkit.class);
        var a = mockStatic(Arena.class);
        var sounds = mockStatic(Sounds.class);
        var language = mockStatic(ChineseMessages.class)) {
      b.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
      NativeJoinNpcs manager = new NativeJoinNpcs(plugin, List::of);
      World w = mock(World.class);
      when(w.getName()).thenReturn("lobby");
      Block air = mock(Block.class, RETURNS_SELF);
      when(air.getType()).thenReturn(Material.AIR);
      when(w.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
      JoinNpcDefinition d = manager.create(new Location(w, 0, 64, 3), "Steve", "Solo", "标题");
      b.when(() -> Bukkit.getWorld("lobby")).thenReturn(w);
      Player p = mock(Player.class);
      UUID uuid = UUID.randomUUID();
      when(p.getUniqueId()).thenReturn(uuid);
      when(p.isOnline()).thenReturn(true);
      when(p.getWorld()).thenReturn(w);
      when(p.getLocation()).thenReturn(new Location(w, 0, 64, 0));
      when(p.getEyeLocation()).thenReturn(new Location(w, 0, 65.62, 0));
      var field = NativeJoinNpcs.class.getDeclaredField("visible");
      field.setAccessible(true);
      Map<UUID, Set<UUID>> visible = (Map<UUID, Set<UUID>>) field.get(manager);
      var click = NativeJoinNpcs.class.getDeclaredMethod("click", Player.class, UUID.class);
      click.setAccessible(true);
      click.invoke(manager, p, d.id());
      a.verify(() -> Arena.joinRandomFromGroup(p, "Solo"), never());
      visible.put(uuid, new HashSet<>(Set.of(d.id())));
      click.invoke(manager, p, d.id());
      click.invoke(manager, p, d.id());
      a.verify(() -> Arena.joinRandomFromGroup(p, "Solo"), times(1));
      when(p.getLocation()).thenReturn(new Location(w, 0, 64, 20));
      click.invoke(manager, p, d.id());
      click.invoke(manager, p, UUID.randomUUID());
      a.verify(() -> Arena.joinRandomFromGroup(p, "Solo"), times(1));
      manager.close();
      assertNull(NativeJoinNpcs.get());
      assertTrue(visible.isEmpty());
    } finally {
      BedWars.plugin = previousPlugin;
      BedWars.config = previousConfig;
    }
  }
}
