package com.andrei1058.bedwars.stats.match;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ViolationConfigurationTest {
    @Test void recorderReadsDefaultsAndOverridesIntoActualDetectorAndStore() throws Exception {
        MainConfig original=BedWars.config;
        try {
            BedWars.config=mock(MainConfig.class); YamlConfiguration yaml=new YamlConfiguration();
            yaml.set(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID,"test"); when(BedWars.config.getYml()).thenReturn(yaml);
            try(MockedConstruction<MatchStatsStore> stores=mockConstruction(MatchStatsStore.class,(store,context) -> {
                assertTrue((int)context.arguments().get(3)>=100); assertTrue((int)context.arguments().get(4)>=1);
            })) {
                MatchStatsRecorder recorder=new MatchStatsRecorder(null,mock(MatchStatsDatabase.class));
                assertEquals(25,recorder.matchLeaveVlThreshold());
                assertTrue(flag(recorder.getViolationDetector(),"enabled")); assertTrue(flag(recorder.getViolationDetector(),"crossTeamItemTransfer"));
                yaml.set(ConfigPath.MATCH_STATISTICS_VIOLATIONS_ENABLED,false);
                yaml.set(ConfigPath.MATCH_STATISTICS_VIOLATIONS_MATCH_LEAVE_THRESHOLD,47);
                yaml.set(ConfigPath.MATCH_STATISTICS_VIOLATIONS_CROSS_TEAM_ITEM_TRANSFER,false);
                yaml.set(ConfigPath.MATCH_STATISTICS_QUEUE_CAPACITY,1); yaml.set(ConfigPath.MATCH_STATISTICS_RETRY_DELAY_SECONDS,0);
                recorder=new MatchStatsRecorder(null,mock(MatchStatsDatabase.class));
                assertEquals(47,recorder.matchLeaveVlThreshold());
                assertFalse(flag(recorder.getViolationDetector(),"enabled")); assertFalse(flag(recorder.getViolationDetector(),"crossTeamItemTransfer"));
                assertEquals(2,stores.constructed().size());
            }
        } finally { BedWars.config=original; }
    }
    boolean flag(Object target,String name) throws Exception { var field=target.getClass().getDeclaredField(name); field.setAccessible(true); return field.getBoolean(target); }
}
