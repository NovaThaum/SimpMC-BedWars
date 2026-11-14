package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.commands.bedwars.subcmds.regular.PlayerListFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerListCountPlaceholderTest {
    @Test
    void replacesCountPlaceholderInLegacyCustomRowTemplates() {
        List<PlayerListFormatter.Group> groups = PlayerListFormatter.groupPlayers(
                List.of("alice", "bob"), name -> name, name -> "Arena");
        MatchmakingMessages.Snapshot messages = MatchmakingMessages.snapshot(Map.of(
                MatchmakingMessages.Key.LIST_HEADER, "{online}/{max}",
                MatchmakingMessages.Key.LIST_EMPTY, "none",
                MatchmakingMessages.Key.LIST_LOBBY, "lobby {count}: {players}",
                MatchmakingMessages.Key.LIST_ARENA, "{arena} {count}: {players}"));

        assertEquals("Arena 2: alice, bob", PlayerListFormatter.format(groups, 2, 100, messages).get(2));
    }
}
