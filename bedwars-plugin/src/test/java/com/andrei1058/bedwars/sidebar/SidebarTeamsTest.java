package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import com.andrei1058.bedwars.api.messages.Messages;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SidebarTeamsTest {
    @Test void filtersGenericAndNamedLinesByStartSnapshotThroughEliminationAndSettlement() throws Exception {
        MainConfig previousConfig = BedWars.config;
        BedWars previousPlugin = BedWars.plugin;
        BedWars.config = mock(MainConfig.class);
        BedWars.plugin = mock(BedWars.class);
        when(BedWars.config.getString(anyString())).thenReturn("");
        when(BedWars.plugin.getDescription()).thenReturn(new PluginDescriptionFile("test", "test", "test"));
        IArena arena = mock(IArena.class);
        ITeam red = team("Red"), blue = team("Blue"), green = team("Green");
        when(arena.getTeams()).thenReturn(List.of(red, green, blue));
        when(arena.getActiveTeamsAtGameStart()).thenReturn(List.of(red, blue));
        when(arena.getDisplayName()).thenReturn("测试地图");
        when(arena.getArenaName()).thenReturn("test");
        when(arena.getDisplayGroup(nullable(Player.class))).thenReturn("测试模式");
        BwSidebar sidebar = mock(BwSidebar.class, CALLS_REAL_METHODS);
        var field = BwSidebar.class.getDeclaredField("arena"); field.setAccessible(true); field.set(sidebar, arena);
        doReturn(mock(Player.class)).when(sidebar).getPlayer();
        ChineseMessages messages = mock(ChineseMessages.class);
        when(messages.m(Messages.FORMATTING_SCOREBOARD_TEAM_GENERIC)).thenReturn("{TeamName}: {TeamStatus}");
        try (var chinese = mockStatic(ChineseMessages.class); var bedwars = mockStatic(BedWars.class)) {
            chinese.when(ChineseMessages::get).thenReturn(messages);
            var api = mock(com.andrei1058.bedwars.api.BedWars.class, RETURNS_DEEP_STUBS);
            bedwars.when(BedWars::getAPI).thenReturn(api);
            List<String> generic = List.of("{team}", "{team}", "{team}", "{team}");
            for (GameState state : List.of(GameState.waiting, GameState.starting)) {
                when(arena.getStatus()).thenReturn(state);
                assertEquals(3, sidebar.normalizeLines(generic).size());
            }
            // 参赛队全部退出后依然展示；不能改用当前成员或可变成员缓存判断。
            when(red.getSize()).thenReturn(0);
            when(red.getMembersCache()).thenReturn(List.of());
            for (GameState state : List.of(GameState.playing, GameState.restarting)) {
                when(arena.getStatus()).thenReturn(state);
                var lines = sidebar.normalizeLines(generic);
                assertEquals(2, lines.size());
                assertTrue(lines.get(0).getLine().contains("Red"));
                assertTrue(lines.get(1).getLine().contains("Blue"));
                var named = sidebar.normalizeLines(List.of("标题", "{TeamGreenName} {TeamGreenStatus}",
                        "{TeamRedName} {TeamRedStatus}", "{TeamGreenColor}绿队", "{TeamGreenLetter}", "页尾"));
                assertEquals(3, named.size());
            }
        } finally { BedWars.config = previousConfig; BedWars.plugin = previousPlugin; }
    }

    @Test void teamReferencesRequireCompletePlaceholderNames() {
        assertFalse(BwSidebar.referencesTeam("{TeamRedwoodName}", team("Red")));
        assertTrue(BwSidebar.referencesTeam("{TeamRedName}", team("Red")));
    }

    @Test void debugMatchesKeepEveryConfiguredTeamVisible() {
        List<ITeam> all = List.of(team("Red"), team("Blue"), team("Green"));
        List<ITeam> active = List.of(all.get(0));
        assertEquals(all, BwSidebar.selectSidebarTeams(true, true, all, active));
        assertEquals(active, BwSidebar.selectSidebarTeams(false, true, all, active));
    }

    private static ITeam team(String name) {
        ITeam team = mock(ITeam.class, RETURNS_DEEP_STUBS);
        when(team.getName()).thenReturn(name);
        when(team.getDisplayName()).thenReturn(name);
        when(team.getColor().chat()).thenReturn(ChatColor.RED);
        return team;
    }
}
