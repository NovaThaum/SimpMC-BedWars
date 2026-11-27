package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArenaSortTest {
    private MainConfig previousConfig;

    @org.junit.jupiter.api.BeforeEach
    void initializePluginConfiguration() {
        previousConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
    }

    @org.junit.jupiter.api.AfterEach
    void restorePluginConfiguration() {
        BedWars.config = previousConfig;
    }

    @Test
    void sortsMapsByNaturalNameRegardlessOfStatusOrPopulation() {
        IArena map10 = arena("Map10", GameState.playing, 10);
        IArena map2 = arena("map2", GameState.restarting, 0);
        IArena map1 = arena("map1", GameState.waiting, 1);
        IArena map01 = arena("map01", GameState.starting, 8);

        assertEquals(List.of(map1, map01, map2, map10),
                Arena.getSorted(List.of(map10, map2, map1, map01)));
    }

    private static IArena arena(String name, GameState state, int players) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        when(arena.getStatus()).thenReturn(state);
        when(arena.getPlayers()).thenReturn(Collections.nCopies(players, mock(org.bukkit.entity.Player.class)));
        return arena;
    }
}
