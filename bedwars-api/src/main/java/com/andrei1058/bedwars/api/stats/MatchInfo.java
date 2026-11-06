package com.andrei1058.bedwars.api.stats;
import java.time.Instant; import java.util.Objects; import java.util.UUID;
public final class MatchInfo {
 private final long matchNumber; private final UUID matchUuid; private final String arenaName,runtimeArenaName,arenaGroup,serverId,status,winnerTeam; private final Instant startedAt,endedAt;
 public MatchInfo(long n,UUID u,String a,String r,String g,String s,String st,Instant start,Instant end,String w){if(n<0)throw new IllegalArgumentException("对局编号不能为负数");matchNumber=n;matchUuid=Objects.requireNonNull(u,"matchUuid");arenaName=a;runtimeArenaName=r;arenaGroup=g;serverId=s;status=Objects.requireNonNull(st,"status");startedAt=Objects.requireNonNull(start,"startedAt");endedAt=end;winnerTeam=w;}
 public long matchNumber(){return matchNumber;} public UUID matchUuid(){return matchUuid;} public String arenaName(){return arenaName;} public String runtimeArenaName(){return runtimeArenaName;} public String arenaGroup(){return arenaGroup;} public String serverId(){return serverId;} public String status(){return status;} public Instant startedAt(){return startedAt;} public Instant endedAt(){return endedAt;} public String winnerTeam(){return winnerTeam;}
}
