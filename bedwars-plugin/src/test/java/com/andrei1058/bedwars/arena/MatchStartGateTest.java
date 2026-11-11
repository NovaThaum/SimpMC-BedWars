package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.arena.tasks.GameStartingTask;
import com.andrei1058.bedwars.arena.team.TeamAssigner;
import com.andrei1058.bedwars.commands.bedwars.subcmds.regular.CmdMatchmakingGate;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.LinkedList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchStartGateTest {
    private MainConfig original;
    @BeforeEach void prepare() {
        original = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        MatchStartGate.resume();
    }
    @AfterEach void cleanup() {
        BedWars.config = original;
        MatchStartGate.resume();
    }
    @Test void repeatedStopAndResumeAreIdempotent() {
        assertTrue(MatchStartGate.isOpen());
        assertTrue(MatchStartGate.stop());
        assertFalse(MatchStartGate.stop());
        assertFalse(MatchStartGate.isOpen());
        assertTrue(MatchStartGate.resume());
        assertFalse(MatchStartGate.resume());
    }
    @Test void commandStopsOnlyCountdownsAndResumeReevaluatesArenasOnce() {
        Arena starting = mock(Arena.class), playing = mock(Arena.class), waiting = mock(Arena.class);
        when(starting.getStatus()).thenReturn(GameState.starting);
        when(playing.getStatus()).thenReturn(GameState.playing);
        when(waiting.getStatus()).thenReturn(GameState.waiting);
        CommandSender admin = mock(org.bukkit.command.ConsoleCommandSender.class);
        when(admin.hasPermission(anyString())).thenReturn(true);
        ParentCommand parent = mock(ParentCommand.class);
        when(parent.getName()).thenReturn("bw");
        try (MockedStatic<Arena> arenas = mockStatic(Arena.class)) {
            arenas.when(Arena::getArenas).thenReturn(new LinkedList<>(List.of(starting, playing, waiting)));
            CmdMatchmakingGate stop = new CmdMatchmakingGate(parent, "stopGames", CmdMatchmakingGate.Action.STOP);
            CmdMatchmakingGate resume = new CmdMatchmakingGate(parent, "resumeGames", CmdMatchmakingGate.Action.RESUME);
            assertTrue(stop.execute(new String[0], admin));
            assertTrue(stop.execute(new String[0], admin));
            verify(starting, times(1)).changeStatus(GameState.waiting);
            verify(playing, never()).changeStatus(any());
            verify(waiting, never()).changeStatus(any());
            assertTrue(resume.execute(new String[0], admin));
            assertTrue(resume.execute(new String[0], admin));
            verify(starting).reevaluateStartEligibility();
            verify(playing).reevaluateStartEligibility();
            verify(waiting).reevaluateStartEligibility();
        }
    }
    @Test void permissionAndInvalidArgumentsCannotChangeGate() {
        ParentCommand parent = mock(ParentCommand.class);
        CommandSender denied = mock(CommandSender.class);
        CmdMatchmakingGate stop = new CmdMatchmakingGate(parent, "stopGames", CmdMatchmakingGate.Action.STOP);
        assertFalse(stop.canSee(denied, null));
        stop.execute(new String[0], denied);
        assertTrue(MatchStartGate.isOpen());
        CommandSender allowed = mock(CommandSender.class);
        when(allowed.hasPermission("bw.command.stopgames")).thenReturn(true);
        assertTrue(stop.canSee(allowed, null));
        assertFalse(stop.execute(new String[]{"extra"}, allowed));
        assertTrue(MatchStartGate.isOpen());
    }
    @Test void closedGateStopsCountdownEvenAtZeroBeforeAssigningTeams() {
        Arena arena = mock(Arena.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask scheduled = mock(BukkitTask.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(), any(Runnable.class), eq(0L), eq(20L))).thenReturn(scheduled);
            GameStartingTask task = new GameStartingTask(arena);
            task.setCountdown(0);
            MatchStartGate.stop();
            task.run();
            verify(scheduled).cancel();
            verify(arena).changeStatus(GameState.waiting);
            verify(arena, never()).getTeamAssigner();
            verify(arena, never()).changeStatus(GameState.playing);
        }
    }
    @Test void directStatusChangesCannotBypassClosedGate() {
        Arena arena = mock(Arena.class, CALLS_REAL_METHODS);
        MatchStartGate.stop();
        arena.changeStatus(GameState.starting);
        assertNull(arena.getStatus());
    }
    @Test void forceStartAndDebugCannotBypassClosedGate() {
        org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        Arena arena = mock(Arena.class);
        when(arena.isPlayer(player)).thenReturn(true);
        when(arena.getStatus()).thenReturn(GameState.waiting);
        com.andrei1058.bedwars.commands.bedwars.MainCommand main = mock(com.andrei1058.bedwars.commands.bedwars.MainCommand.class);
        try (MockedStatic<Arena> arenas = mockStatic(Arena.class);
             MockedStatic<com.andrei1058.bedwars.commands.bedwars.MainCommand> commands = mockStatic(com.andrei1058.bedwars.commands.bedwars.MainCommand.class)) {
            arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
            commands.when(com.andrei1058.bedwars.commands.bedwars.MainCommand::getInstance).thenReturn(main);
            com.andrei1058.bedwars.commands.bedwars.subcmds.regular.CmdStart start =
                    new com.andrei1058.bedwars.commands.bedwars.subcmds.regular.CmdStart(mock(ParentCommand.class), "start");
            MatchStartGate.stop();
            assertTrue(start.execute(new String[0], player));
            assertTrue(start.execute(new String[]{"debug"}, player));
            verify(arena, never()).changeStatus(any());
            verify(arena, never()).getStartingTask();
        }
    }
    @Test void messageDefaultsPreserveAdministratorOverrides() {
        com.andrei1058.bedwars.api.language.Language language = mock(com.andrei1058.bedwars.api.language.Language.class);
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("cmd-matchmaking-blocked", "&c维护中，请稍候");
        when(language.getYml()).thenReturn(yaml);
        com.andrei1058.bedwars.language.MatchmakingMessages.install(language);
        assertEquals("&c维护中，请稍候", yaml.getString("cmd-matchmaking-blocked"));
        assertEquals("&a已恢复新对局。", yaml.getString("cmd-matchmaking-resumed"));
        verify(language).save();
    }
    @Test void reevaluationUsesCurrentEligibilityAndLeavesRunningMatchesAlone() throws Exception {
        Arena arena = mock(Arena.class, CALLS_REAL_METHODS);
        doNothing().when(arena).changeStatus(any());
        java.lang.reflect.Field state = Arena.class.getDeclaredField("status");
        state.setAccessible(true);
        state.set(arena, GameState.waiting);
        try (MockedStatic<TeamAssigner> teams = mockStatic(TeamAssigner.class)) {
            teams.when(() -> TeamAssigner.canFormValidTeams(arena)).thenReturn(false);
            assertFalse(arena.reevaluateStartEligibility());
            teams.when(() -> TeamAssigner.canFormValidTeams(arena)).thenReturn(true);
            assertTrue(arena.reevaluateStartEligibility());
            verify(arena).changeStatus(GameState.starting);
            state.set(arena, GameState.starting);
            MatchStartGate.stop();
            assertTrue(arena.reevaluateStartEligibility());
            verify(arena).changeStatus(GameState.waiting);
            state.set(arena, GameState.playing);
            assertFalse(arena.reevaluateStartEligibility());
        }
    }
}
