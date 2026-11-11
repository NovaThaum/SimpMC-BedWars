package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.api.arena.*;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.UUID;
import static org.mockito.Mockito.*;
class IllegalTeamDetectorLegacyTest {
    @Test void pickupEventScoresRepeatedEnemyTransfersOnlyWhenBothSwitchesEnabled() {
        MainConfig old=BedWars.config; BedWars.config=mock(MainConfig.class);
        IArena arena=mock(IArena.class); Player owner=mock(Player.class),recipient=mock(Player.class);
        UUID ownerId=UUID.randomUUID(),recipientId=UUID.randomUUID();
        when(owner.getUniqueId()).thenReturn(ownerId); when(recipient.getUniqueId()).thenReturn(recipientId);
        when(owner.isOnline()).thenReturn(true); when(recipient.isOnline()).thenReturn(true);
        when(arena.isPlayer(any())).thenReturn(true); when(arena.getStatus()).thenReturn(GameState.playing);
        ITeam red=mock(ITeam.class),blue=mock(ITeam.class); when(red.getName()).thenReturn("Red"); when(blue.getName()).thenReturn("Blue");
        when(arena.getExTeam(ownerId)).thenReturn(red); when(arena.getExTeam(recipientId)).thenReturn(blue);
        MatchStatsRecorder recorder=mock(MatchStatsRecorder.class); MatchRecord record=mock(MatchRecord.class);
        when(recorder.recordForDetector(arena)).thenReturn(record);
        try(MockedStatic<Arena> arenas=mockStatic(Arena.class)) {
            arenas.when(() -> Arena.getArenaByPlayer(owner)).thenReturn(arena); arenas.when(() -> Arena.getArenaByPlayer(recipient)).thenReturn(arena);
            for(boolean[] switches : new boolean[][]{{false,true},{true,false},{true,true}}) {
                reset(recorder); when(recorder.recordForDetector(arena)).thenReturn(record);
                IllegalTeamDetector detector=new IllegalTeamDetector(null,recorder,switches[0],25,switches[1]);
                for(int i=0;i<2;i++) {
                    Item item=mock(Item.class); when(item.getUniqueId()).thenReturn(UUID.randomUUID()); when(item.getItemStack()).thenReturn(new ItemStack(Material.DIAMOND,2));
                    detector.onDrop(new PlayerDropItemEvent(owner,item)); detector.onPickup(new PlayerPickupItemEvent(recipient,item,0));
                }
                if(switches[0]&&switches[1]) {
                    verify(recorder).addDetectedViolation(eq(arena),eq(ownerId),eq(5),eq(false),eq("REPEATED_RESOURCE_TRANSFER"),anyString());
                    verify(recorder).addDetectedViolation(eq(arena),eq(recipientId),eq(5),eq(false),eq("REPEATED_RESOURCE_TRANSFER"),anyString());
                } else verify(recorder,never()).addDetectedViolation(any(),any(),anyInt(),anyBoolean(),anyString(),anyString());
            }
        } finally { BedWars.config=old; }
    }
    @Test void thresholdUsesStrictCrossingAndOnlyPositiveIncrements() {
        MatchStatsRecorder recorder=mock(MatchStatsRecorder.class); IArena arena=mock(IArena.class); UUID id=UUID.randomUUID();
        IllegalTeamDetector detector=new IllegalTeamDetector(null,recorder,true,25,true);
        detector.scoreChanged(arena,id,25,1,"TEST"); detector.scoreChanged(arena,id,26,-1,"TEST");
        verifyNoInteractions(recorder); detector.scoreChanged(arena,id,26,1,"TEST");
        verify(recorder).ejectForViolation(arena,id,26,"TEST");
    }
}
