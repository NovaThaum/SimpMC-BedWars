package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.support.npc.NativeJoinNpcs;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class NativeNPCTest {
  private com.andrei1058.bedwars.configuration.MainConfig previousConfig;

  @org.junit.jupiter.api.BeforeEach
  void initializePluginConfiguration() {
    previousConfig = com.andrei1058.bedwars.BedWars.config;
    com.andrei1058.bedwars.BedWars.config = mock(com.andrei1058.bedwars.configuration.MainConfig.class);
  }

  @org.junit.jupiter.api.AfterEach
  void restorePluginConfiguration() {
    com.andrei1058.bedwars.BedWars.config = previousConfig;
  }

  @Test
  void screenshotCreateSyntaxAndOldAddAliasPreserveSpacesAndNewlines() throws Exception {
    NativeJoinNpcs manager = mock(NativeJoinNpcs.class);
    Player p = mock(Player.class);
    Location loc = new Location(null, 1, 64, 2);
    when(p.getLocation()).thenReturn(loc);
    try (var n = mockStatic(NativeJoinNpcs.class);
        var groups = mockStatic(MainCommand.class)) {
      n.when(NativeJoinNpcs::get).thenReturn(manager);
      groups.when(() -> MainCommand.isArenaGroup("Solo")).thenReturn(true);
      NativeNPC cmd = new NativeNPC(mock(ParentCommand.class));
      for (String alias : new String[] {"create", "add"})
        assertTrue(
            cmd.execute(
                new String[] {alias, "andrei1058", "Solo", "&9&l单人模式\\n&f{players}", "人正在游戏"}, p));
      verify(manager, times(2)).create(loc, "andrei1058", "Solo", "&9&l单人模式\\n&f{players} 人正在游戏");
      assertNotNull(cmd.getDisplayInfo());
      assertEquals(java.util.List.of("create", "add", "remove"), cmd.getTabComplete());
    }
  }

  @Test
  void unknownGroupCannotCreateNpc() throws Exception {
    NativeJoinNpcs manager = mock(NativeJoinNpcs.class);
    Player p = mock(Player.class);
    try (var n = mockStatic(NativeJoinNpcs.class);
        var groups = mockStatic(MainCommand.class)) {
      n.when(NativeJoinNpcs::get).thenReturn(manager);
      new NativeNPC(mock(ParentCommand.class))
          .execute(new String[] {"create", "Steve", "Missing", "标题"}, p);
      verifyNoInteractions(manager);
    }
  }

  @Test
  void createAllowsNoFloatingText() throws Exception {
    NativeJoinNpcs manager = mock(NativeJoinNpcs.class);
    Player p = mock(Player.class);
    Location loc = new Location(null, 1, 64, 2);
    when(p.getLocation()).thenReturn(loc);
    try (var n = mockStatic(NativeJoinNpcs.class);
        var groups = mockStatic(MainCommand.class)) {
      n.when(NativeJoinNpcs::get).thenReturn(manager);
      groups.when(() -> MainCommand.isArenaGroup("Solo")).thenReturn(true);
      new NativeNPC(mock(ParentCommand.class))
          .execute(new String[] {"create", "andrei1058", "Solo"}, p);
      verify(manager).create(loc, "andrei1058", "Solo", "");
    }
  }

  @Test
  void compositeGroupsMustAllExist() {
    try (var groups = mockStatic(MainCommand.class)) {
      groups.when(() -> MainCommand.isArenaGroup("Solo")).thenReturn(true);
      groups.when(() -> MainCommand.isArenaGroup("Doubles")).thenReturn(true);
      assertTrue(NativeNPC.validGroups("Solo+Doubles"));
      assertFalse(NativeNPC.validGroups("Solo+Missing"));
      assertFalse(NativeNPC.validGroups("Solo+"));
    }
  }

  @Test
  void managementRequiresExistingNpcPermission() {
    NativeNPC cmd = new NativeNPC(mock(ParentCommand.class));
    Player p = mock(Player.class);
    assertFalse(cmd.hasPermission(p));
    when(p.hasPermission(com.andrei1058.bedwars.configuration.Permissions.PERMISSION_NPC))
        .thenReturn(true);
    assertTrue(cmd.hasPermission(p));
  }
}
