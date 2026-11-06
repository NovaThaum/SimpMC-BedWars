package com.andrei1058.bedwars.api.stats;
import java.util.Objects; import java.util.UUID;
public final class PlayerMatchTotals {
 private final UUID playerUuid; private final long matchesPlayed,kills,finalKills,deaths,bedsDestroyed;
 public PlayerMatchTotals(UUID u,long m,long k,long f,long d,long b){playerUuid=Objects.requireNonNull(u,"playerUuid");if(m<0||k<0||f<0||d<0||b<0)throw new IllegalArgumentException("战绩不能为负数");matchesPlayed=m;kills=k;finalKills=f;deaths=d;bedsDestroyed=b;}
 public UUID playerUuid(){return playerUuid;} public long matchesPlayed(){return matchesPlayed;} public long kills(){return kills;} public long finalKills(){return finalKills;} public long deaths(){return deaths;} public long bedsDestroyed(){return bedsDestroyed;} public double kdRatio(){return KillDeathRatio.calculate(kills,deaths);}
 @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof PlayerMatchTotals other))return false;return matchesPlayed==other.matchesPlayed&&kills==other.kills&&finalKills==other.finalKills&&deaths==other.deaths&&bedsDestroyed==other.bedsDestroyed&&playerUuid.equals(other.playerUuid);}
 @Override public int hashCode(){return Objects.hash(playerUuid,matchesPlayed,kills,finalKills,deaths,bedsDestroyed);}
}
