package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.language.MatchmakingMessages;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerListFormatterTest {
    @Test
    void groupsLobbyBeforeAlphabeticallySortedArenasAndNames() {
        List<Entry> players = Arrays.asList(
                new Entry("zeta", "Arena-B"), new Entry("bob", null),
                new Entry("Alex", "arena-a"), new Entry("amy", null));

        List<PlayerListFormatter.Group> groups = PlayerListFormatter.groupPlayers(players,
                Entry::name, Entry::arena);

        assertEquals(Arrays.asList("amy", "bob"), groups.get(0).players());
        assertEquals("arena-a", groups.get(1).arenaName());
        assertEquals(Arrays.asList("Alex"), groups.get(1).players());
        assertEquals("Arena-B", groups.get(2).arenaName());
        assertEquals(Arrays.asList("zeta"), groups.get(2).players());
    }

    @Test
    void formatIncludesTotalCapacityAndGroupRows() {
        List<PlayerListFormatter.Group> groups = PlayerListFormatter.groupPlayers(
                Arrays.asList(new Entry("alice", null), new Entry("bob", "Arena1")),
                Entry::name, Entry::arena);

        List<String> lines = PlayerListFormatter.format(groups, 2, 100,
                MatchmakingMessages.defaults(true));

        assertEquals("&6在线玩家 &8» &f当前有 2 位玩家在线，可容纳最大在线人数为 100 位玩家。", lines.get(0));
        assertEquals("&a大厅 &8» &falice", lines.get(1));
        assertEquals("&b竞技场 &8[&fArena1&8] &8» &fbob", lines.get(2));
    }

    private static final class Entry {
        private final String name;
        private final String arena;
        private Entry(String name, String arena) { this.name = name; this.arena = arena; }
        private String name() { return name; }
        private String arena() { return arena; }
    }
}
