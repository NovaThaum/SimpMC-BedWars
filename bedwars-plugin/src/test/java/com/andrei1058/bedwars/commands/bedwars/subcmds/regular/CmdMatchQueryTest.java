package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.language.MatchQueryMessages;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CmdMatchQueryTest {
    @Test
    public void historyAllDisplaysEachDescendingPageInAscendingOrder() {
        List<MatchInfo> newestFirst = Arrays.asList(match(30), match(20), match(10));

        List<String> lines = CmdMatchQuery.historyLines(newestFirst, "全部记录", "all", 2,
                true, MatchQueryMessages.defaults(true));

        assertEquals("&e#10 &fMap &7已完成 · 2026-01-01 08:00:00", lines.get(1));
        assertEquals("&e#20 &fMap &7已完成 · 2026-01-01 08:00:00", lines.get(2));
        assertEquals("&e#30 &fMap &7已完成 · 2026-01-01 08:00:00", lines.get(3));
        assertEquals("&7使用 /bw match <编号> 查看单局战绩；/bw history all 3 查看更早记录。", lines.get(4));
    }

    @Test
    public void historyUsesDescendingOrderForIndividualPlayers() {
        List<String> lines = CmdMatchQuery.historyLines(Arrays.asList(match(30), match(20)),
                "player", "player", 1, false, MatchQueryMessages.defaults(true));

        assertEquals("&e#30 &fMap &7已完成 · 2026-01-01 08:00:00", lines.get(1));
        assertEquals("&e#20 &fMap &7已完成 · 2026-01-01 08:00:00", lines.get(2));
    }

    @Test
    public void emptyHistoryShowsNoRowsAndStillOffersNextPage() {
        List<String> lines = CmdMatchQuery.historyLines(Collections.emptyList(), "all", "all", 1,
                true, MatchQueryMessages.defaults(true));

        assertEquals(3, lines.size());
        assertEquals("&7这一页没有对局记录。", lines.get(1));
        assertEquals("&7使用 /bw match <编号> 查看单局战绩；/bw history all 2 查看更早记录。", lines.get(2));
    }

    @Test
    public void pageOffsetRejectsInvalidAndOverflowingPages() {
        assertEquals(0, CmdMatchQuery.pageOffset("1"));
        assertEquals(20, CmdMatchQuery.pageOffset("3"));
        assertInvalidPage("0");
        assertInvalidPage("abc");
        assertInvalidPage("214748366");
    }

    private static void assertInvalidPage(String value) {
        assertThrows(IllegalArgumentException.class, () -> CmdMatchQuery.pageOffset(value));
    }

    private static MatchInfo match(long number) {
        return new MatchInfo(number, UUID.randomUUID(), "Map", "arena-1", "Default", "local",
                "FINISHED", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:10:00Z"), "red");
    }
}
