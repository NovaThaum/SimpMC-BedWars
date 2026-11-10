package com.andrei1058.bedwars.arena.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Packs intact squads into the fewest possible teams while keeping capacity. */
final class TeamAllocationPlanner {
    private TeamAllocationPlanner() { }

    static <T> List<List<T>> allocate(List<List<T>> input, int teamCount, int capacity, Random random) {
        if (teamCount < 2 || capacity < 1) return Collections.emptyList();
        List<List<T>> groups = new ArrayList<>();
        int players = 0;
        for (List<T> source : input) {
            if (source == null || source.isEmpty()) continue;
            players += source.size();
            if (source.size() <= capacity) groups.add(new ArrayList<>(source));
            else {
                List<T> oversized = new ArrayList<>(source);
                Collections.shuffle(oversized, random);
                for (int i = 0; i < oversized.size(); i += capacity) {
                    groups.add(new ArrayList<>(oversized.subList(i, Math.min(i + capacity, oversized.size()))));
                }
            }
        }
        if (players > (long) teamCount * capacity || players < 2) return Collections.emptyList();
        Collections.shuffle(groups, random);
        groups.sort(Comparator.comparingInt((List<T> group) -> group.size()).reversed());
        int minimumTeams = Math.max(2, (players + capacity - 1) / capacity);
        for (int count = minimumTeams; count <= teamCount; count++) {
            int[] loads = new int[count];
            int[] destinations = new int[groups.size()];
            if (!place(groups, 0, capacity, loads, destinations)) continue;
            List<List<T>> result = new ArrayList<>();
            for (int i = 0; i < teamCount; i++) result.add(new ArrayList<>());
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < count; i++) order.add(i);
            Collections.shuffle(order, random);
            for (int i = 0; i < groups.size(); i++) result.get(order.get(destinations[i])).addAll(groups.get(i));
            return result;
        }
        return Collections.emptyList();
    }

    private static <T> boolean place(List<List<T>> groups, int index, int capacity, int[] loads, int[] dest) {
        if (index == groups.size()) {
            int active = 0;
            for (int load : loads) if (load > 0) active++;
            return active >= 2;
        }
        int size = groups.get(index).size();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < loads.length; i++) if (loads[i] + size <= capacity) candidates.add(i);
        candidates.sort(Comparator.comparingInt(i -> -loads[i]));
        int previous = -1;
        for (int team : candidates) {
            if (loads[team] == previous) continue;
            previous = loads[team];
            loads[team] += size;
            dest[index] = team;
            if (place(groups, index + 1, capacity, loads, dest)) return true;
            loads[team] -= size;
        }
        return false;
    }
}
