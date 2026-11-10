package com.andrei1058.bedwars.api.arena.team;

import org.bukkit.entity.Player;
import java.util.List;

/** Temporary arena-local groups formed before a match starts. */
public interface PreGameSquad {
    enum Result { SUCCESS, NOT_IN_PRE_GAME, DIFFERENT_ARENA, CANNOT_INVITE_SELF, NOT_LEADER,
        TARGET_ALREADY_GROUPED, SQUAD_FULL, ALREADY_INVITED, NO_INVITE, INVITE_EXPIRED, NOT_IN_SQUAD }
    Result invite(Player inviter, Player target);
    Result accept(Player target, Player inviter);
    Result decline(Player target, Player inviter);
    Result leave(Player player);
    boolean isLeader(Player player);
    List<Player> getMembers(Player player);
    List<Player> getAvailableTargets(Player player);
}
