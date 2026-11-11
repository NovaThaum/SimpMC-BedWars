/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.ITeamAssigner;
import com.andrei1058.bedwars.api.events.gameplay.TeamAssignEvent;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TeamAssigner implements ITeamAssigner {

    public static boolean canFormValidTeams(IArena arena) {
        if (arena == null || arena.getPlayers().size() < arena.getMinPlayers()
                || arena.getTeams().size() < 2 || arena.getMaxInTeam() < 1
                || arena.getPlayers().size() > (long) arena.getTeams().size() * arena.getMaxInTeam()) return false;
        List<List<Player>> groups = PreGameSquadManager.getInstance().getAssignmentGroups(arena);
        return !TeamAllocationPlanner.allocate(groups, arena.getTeams().size(), arena.getMaxInTeam(), new Random()).isEmpty();
    }

    public static boolean isAssignmentValid(IArena arena, List<List<Player>> groups, boolean debug) {
        if (arena.getPlayers().isEmpty() || (!debug && (arena.getPlayers().size() < arena.getMinPlayers()
                || arena.getTeams().size() < 2)) || arena.getMaxInTeam() < 1) return false;
        java.util.Set<java.util.UUID> expected = new java.util.HashSet<>();
        for (Player player : arena.getPlayers()) if (!expected.add(player.getUniqueId())) return false;
        java.util.Map<java.util.UUID, ITeam> assigned = new java.util.HashMap<>();
        int active = 0;
        for (ITeam team : arena.getTeams()) {
            if (team.getMembers().size() > arena.getMaxInTeam()) return false;
            if (!team.getMembers().isEmpty()) active++;
            for (Player player : team.getMembers()) {
                if (!expected.contains(player.getUniqueId()) || assigned.put(player.getUniqueId(), team) != null) return false;
            }
        }
        if (active < (debug ? 1 : 2) || !assigned.keySet().equals(expected)) return false;
        // 同时检查分队前快照和插件事件回调中可能改变的实时小队。
        List<List<Player>> allGroups = new ArrayList<>(groups);
        allGroups.addAll(PreGameSquadManager.getInstance().getAssignmentGroups(arena));
        for (List<Player> group : allGroups) {
            if (group.size() > arena.getMaxInTeam()) continue;
            ITeam destination = null;
            for (Player player : group) {
                ITeam current = assigned.get(player.getUniqueId());
                if (current == null || destination != null && destination != current) return false;
                destination = current;
            }
        }
        return true;
    }

    public static void clearAssignment(IArena arena) {
        for (ITeam team : arena.getTeams()) {
            if (team instanceof BedWarsTeam concrete) {
                for (Player player : new ArrayList<>(team.getMembers())) {
                    BedWarsTeam.BedHolo holo = concrete.getBedHolo(player);
                    if (holo != null) holo.destroy();
                }
            }
            team.getMembers().clear();
            team.getMembersCache().clear();
        }
    }

    public void assignTeams(IArena arena) { assignTeams(arena, false); }

    public void assignTeams(IArena arena, boolean debug) {
        List<List<Player>> groups = PreGameSquadManager.getInstance().getAssignmentGroups(arena);
        List<List<Player>> planned = TeamAllocationPlanner.allocate(groups, arena.getTeams().size(),
                arena.getMaxInTeam(), new Random());
        if (debug && planned.isEmpty() && !arena.getTeams().isEmpty()
                && arena.getPlayers().size() <= arena.getMaxInTeam()) {
            planned = new ArrayList<>();
            planned.add(new ArrayList<>(arena.getPlayers()));
        }
        if (planned.isEmpty()) return;
        List<ITeam> teams = new ArrayList<>(arena.getTeams());
        List<List<Player>> approved = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) approved.add(new ArrayList<>());
        for (int i = 0; i < planned.size(); i++) {
            ITeam team = teams.get(i);
            for (Player player : planned.get(i)) {
                TeamAssignEvent event = new TeamAssignEvent(player, team, arena);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) approved.get(i).add(player);
            }
        }
        int activeTeams = 0;
        for (List<Player> team : approved) {
            if (team.size() > arena.getMaxInTeam()) return;
            if (!team.isEmpty()) activeTeams++;
        }
        if (activeTeams < (debug ? 1 : 2)
                || approved.stream().mapToInt(List::size).sum() != arena.getPlayers().size()) return;
        for (int i = 0; i < teams.size(); i++) for (Player player : approved.get(i)) {
            player.closeInventory();
            teams.get(i).addPlayers(player);
        }

    }
}
