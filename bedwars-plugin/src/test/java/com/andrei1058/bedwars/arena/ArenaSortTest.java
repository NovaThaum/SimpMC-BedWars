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
    void retainsStatusAndPopulationPriorityWithDeterministicNameTieBreaks() {
        IArena waitZulu = arena("Zulu", GameState.waiting, 2);
        IArena waitAlpha = arena("Alpha", GameState.waiting, 2);
        IArena waitSmall = arena("Small", GameState.waiting, 1);
        IArena startZulu = arena("ZuluStart", GameState.starting, 1);
        IArena startAlpha = arena("AlphaStart", GameState.starting, 1);
        IArena playingZulu = arena("ZuluPlay", GameState.playing, 10);
        IArena playingAlpha = arena("AlphaPlay", GameState.playing, 0);
        IArena restarting = arena("Restarting", GameState.restarting, 0);

        assertEquals(List.of(startAlpha, startZulu, waitAlpha, waitZulu, waitSmall,
                        playingAlpha, playingZulu, restarting),
                Arena.getSorted(List.of(waitZulu, playingZulu, startZulu, restarting,
                        waitSmall, playingAlpha, startAlpha, waitAlpha)));
    }

    private static IArena arena(String name, GameState state, int players) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        when(arena.getStatus()).thenReturn(state);
        when(arena.getPlayers()).thenReturn(Collections.nCopies(players, mock(org.bukkit.entity.Player.class)));
        return arena;
    }
}
