package com.andrei1058.bedwars.arena.team;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class TeamAllocationPlannerTest {
    @Test void separatesTwoSoloPlayersAndFillsNoMoreTeamsThanNeeded() {
        List<List<String>> result = TeamAllocationPlanner.allocate(Arrays.asList(
                Collections.singletonList("A"), Collections.singletonList("B")), 4, 4, new Random(1));
        assertEquals(4, result.size());
        assertEquals(2, result.stream().filter(team -> !team.isEmpty()).count());
        assertEquals(2, result.stream().mapToInt(List::size).sum());
    }

    @Test void keepsNormalSquadsTogetherAndNeedsAnOpponent() {
        assertTrue(TeamAllocationPlanner.allocate(Collections.singletonList(Arrays.asList("A", "B")), 4, 4,
                new Random(2)).isEmpty());
        List<List<String>> result = TeamAllocationPlanner.allocate(Arrays.asList(
                Arrays.asList("A", "B"), Collections.singletonList("C")), 4, 4, new Random(3));
        assertEquals(2, result.stream().filter(team -> !team.isEmpty()).count());
        assertTrue(result.stream().anyMatch(team -> team.containsAll(Arrays.asList("A", "B"))));
    }

    @Test void splitsOnlyGroupsLargerThanTeamCapacity() {
        List<List<String>> result = TeamAllocationPlanner.allocate(Collections.singletonList(
                Arrays.asList("A", "B", "C", "D", "E")), 3, 2, new Random(4));
        assertEquals(3, result.stream().filter(team -> !team.isEmpty()).count());
        assertTrue(result.stream().allMatch(team -> team.size() <= 2));
    }

    @Test void rejectsImpossibleTotalCapacity() {
        assertTrue(TeamAllocationPlanner.allocate(Arrays.asList(Collections.singletonList("A"),
                Collections.singletonList("B"), Collections.singletonList("C")), 2, 1, new Random(5)).isEmpty());
    }
}
