package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.*;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.gameplay.TeamAssignEvent;
import com.andrei1058.bedwars.arena.*;
import com.andrei1058.bedwars.arena.tasks.GameStartingTask;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class StartBoundaryTest {
    MainConfig original;
    @BeforeEach void setup() { original=BedWars.config; BedWars.config=mock(MainConfig.class); MatchStartGate.resume(); }
    @AfterEach void cleanup() { BedWars.config=original; MatchStartGate.resume(); }
    Player player() { Player p=mock(Player.class); when(p.getUniqueId()).thenReturn(UUID.randomUUID()); return p; }
    ITeam team(Player... players) { ITeam t=mock(ITeam.class); when(t.getMembers()).thenReturn(new ArrayList<>(List.of(players))); when(t.getMembersCache()).thenReturn(new ArrayList<>(List.of(players))); return t; }
    @Test void finalValidationRejectsMissingDuplicateOversizedAndSplitSquadsButAllowsExplicitSoloDebug() {
        IArena arena=mock(IArena.class); Player a=player(),b=player(),c=player();
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(a,b,c))); when(arena.getMinPlayers()).thenReturn(2); when(arena.getMaxInTeam()).thenReturn(2);
        ITeam red=team(a,b), blue=team(c); when(arena.getTeams()).thenReturn(new LinkedList<>(List.of(red,blue)));
        assertTrue(TeamAssigner.isAssignmentValid(arena,List.of(List.of(a,b),List.of(c)),false));
        assertFalse(TeamAssigner.isAssignmentValid(arena,List.of(List.of(a,c),List.of(b)),false));
        blue.getMembers().clear(); assertFalse(TeamAssigner.isAssignmentValid(arena,List.of(),false));
        blue.getMembers().add(a); assertFalse(TeamAssigner.isAssignmentValid(arena,List.of(),false));
        red.getMembers().add(c); assertFalse(TeamAssigner.isAssignmentValid(arena,List.of(),false));
        ITeam solo=team(a), empty=team();
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(a))); when(arena.getTeams()).thenReturn(new LinkedList<>(List.of(solo,empty)));
        assertFalse(TeamAssigner.isAssignmentValid(arena,List.of(List.of(a)),false));
        assertTrue(TeamAssigner.isAssignmentValid(arena,List.of(List.of(a)),true));
    }
    @Test void squadAcceptAndLeaveReevaluateImmediately() {
        Arena arena=mock(Arena.class); Player a=player(),b=player();
        when(arena.getStatus()).thenReturn(GameState.starting); when(arena.getMaxInTeam()).thenReturn(4);
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(a,b))); when(arena.isPlayer(any())).thenReturn(true);
        try(MockedStatic<Arena> arenas=mockStatic(Arena.class)) {
            arenas.when(() -> Arena.getArenaByPlayer(a)).thenReturn(arena); arenas.when(() -> Arena.getArenaByPlayer(b)).thenReturn(arena);
            PreGameSquadManager squads=PreGameSquadManager.getInstance();
            try {
                assertEquals(com.andrei1058.bedwars.api.arena.team.PreGameSquad.Result.SUCCESS,squads.invite(a,b));
                squads.accept(b,a); verify(arena).reevaluateStartEligibility();
                squads.leave(b); verify(arena,times(2)).reevaluateStartEligibility();
            } finally { squads.clearArena(arena); }
        }
    }
    @Test void fullArenaShortensQueuedTaskToFixedFiveWithoutSchedulerProbe() throws Exception {
        Arena arena=mock(Arena.class,CALLS_REAL_METHODS);
        var state=Arena.class.getDeclaredField("status"); state.setAccessible(true); state.set(arena,GameState.starting);
        GameStartingTask task=mock(GameStartingTask.class); when(task.getCountdown()).thenReturn(40);
        var starting=Arena.class.getDeclaredField("startingTask"); starting.setAccessible(true); starting.set(arena,task);
        doReturn(new LinkedList<>(List.of(player(),player()))).when(arena).getPlayers(); doReturn(2).when(arena).getMaxPlayers();
        arena.shortenFullCountdown(); verify(task).setCountdown(5);
        when(task.getCountdown()).thenReturn(3); arena.shortenFullCountdown(); verify(task,times(1)).setCountdown(anyInt());
    }
    @Test void assignmentEventCancellationPreventsAnyPartialAssignment() {
        IArena arena=mock(IArena.class); Player a=player(),b=player(),c=player();
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(a,b,c))); when(arena.getMaxInTeam()).thenReturn(2);
        ITeam red=team(),blue=team(); when(arena.getTeams()).thenReturn(new LinkedList<>(List.of(red,blue)));
        PluginManager plugins=mock(PluginManager.class);
        doAnswer(call -> { TeamAssignEvent e=call.getArgument(0); if(e.getPlayer()==c)e.setCancelled(true);return null; }).when(plugins).callEvent(any(TeamAssignEvent.class));
        try(MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins); new TeamAssigner().assignTeams(arena);
            verify(red,never()).addPlayers(any(Player.class)); verify(blue,never()).addPlayers(any(Player.class));
        }
    }
    @Test void countdownZeroRevalidatesAndRollsBackFailedAssignmentBeforeAnySpawn() {
        Arena arena=mock(Arena.class); Player a=player(),b=player(); ITeam red=team(),blue=team();
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(a,b))); when(arena.getTeams()).thenReturn(new LinkedList<>(List.of(red,blue)));
        when(arena.getMinPlayers()).thenReturn(2); when(arena.getMaxInTeam()).thenReturn(4); when(arena.getStatus()).thenReturn(GameState.starting);
        var assigner=mock(com.andrei1058.bedwars.api.arena.team.ITeamAssigner.class); when(arena.getTeamAssigner()).thenReturn(assigner);
        BukkitScheduler scheduler=mock(BukkitScheduler.class); BukkitTask handle=mock(BukkitTask.class);
        when(scheduler.runTaskTimer(any(),any(Runnable.class),eq(0L),eq(20L))).thenReturn(handle);
        try(MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            GameStartingTask task=new GameStartingTask(arena); task.setCountdown(0); task.run();
            verify(assigner).assignTeams(arena); verify(arena).changeStatus(GameState.waiting);
            verify(arena,never()).captureTeamSizesAtGameStart(); verify(arena,never()).changeStatus(GameState.playing);
            verify(red,never()).firstSpawn(any());
        }
    }
    @Test void taskConstructionShortensFullArenaBeforeItsFirstRun() {
        Arena arena=mock(Arena.class); Player a=player(),b=player();
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(a,b))); when(arena.getMaxPlayers()).thenReturn(2);
        when(BedWars.config.getInt(com.andrei1058.bedwars.api.configuration.ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_REGULAR)).thenReturn(40);
        BukkitScheduler scheduler=mock(BukkitScheduler.class); BukkitTask handle=mock(BukkitTask.class);
        when(scheduler.runTaskTimer(any(),any(Runnable.class),eq(0L),eq(20L))).thenReturn(handle);
        try(MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            GameStartingTask task=new GameStartingTask(arena); assertEquals(5,task.getCountdown());
            verify(scheduler,never()).isCurrentlyRunning(anyInt()); verify(scheduler,never()).isQueued(anyInt());
        }
    }
}
