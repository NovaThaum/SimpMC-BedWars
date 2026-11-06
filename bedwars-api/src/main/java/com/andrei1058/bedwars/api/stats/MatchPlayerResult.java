package com.andrei1058.bedwars.api.stats;
import java.util.Objects; import java.util.UUID;
public final class MatchPlayerResult {
 private final UUID playerUuid; private final String playerName,teamId,outcome; private final long kills,finalKills,deaths,bedsDestroyed;
 public MatchPlayerResult(UUID u,String n,String t,long k,long f,long d,long b,String o){playerUuid=Objects.requireNonNull(u,"playerUuid");if(k<0||f<0||d<0||b<0)throw new IllegalArgumentException("战绩不能为负数");playerName=n;teamId=t;kills=k;finalKills=f;deaths=d;bedsDestroyed=b;outcome=o;}
 public UUID playerUuid(){return playerUuid;} public String playerName(){return playerName;} public String teamId(){return teamId;} public long kills(){return kills;} public long finalKills(){return finalKills;} public long deaths(){return deaths;} public long bedsDestroyed(){return bedsDestroyed;} public String outcome(){return outcome;} public double kdRatio(){return KillDeathRatio.calculate(kills,deaths);}
}
