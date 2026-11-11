package com.andrei1058.bedwars.arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SafeSpawnResolverTest {
    @Test void resolvesNearbyStandingSpaceAndPreservesLookDirection() {
        World world = mock(World.class); WorldBorder border = mock(WorldBorder.class);
        when(world.getWorldBorder()).thenReturn(border); when(border.getSize()).thenReturn(100.0);
        when(border.getCenter()).thenReturn(new Location(world,0,0,0)); when(world.getMaxHeight()).thenReturn(256);
        when(world.isChunkLoaded(anyInt(),anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(),anyInt(),anyInt())).thenAnswer(call -> {
            int x=call.getArgument(0), y=call.getArgument(1), z=call.getArgument(2);
            Block block=mock(Block.class);
            when(block.getType()).thenReturn(x==1 && z==0 && y==63 ? Material.STONE : Material.AIR);
            return block;
        });
        Location result=SafeSpawnResolver.resolve(new Location(world,.5,64,.5,90,20));
        assertNotNull(result); assertEquals(1.5,result.getX()); assertEquals(64,result.getY());
        assertEquals(90,result.getYaw()); assertEquals(20,result.getPitch());
    }
    @Test void refusesUnloadedVoidHazardsAndOneBlockHighSpace() {
        World world=mock(World.class); when(world.getMaxHeight()).thenReturn(256);
        assertNull(SafeSpawnResolver.resolve(new Location(world,0,64,0)));
        verify(world,never()).getBlockAt(anyInt(),anyInt(),anyInt());
        when(world.isChunkLoaded(anyInt(),anyInt())).thenReturn(true);
        Block block=mock(Block.class); when(world.getBlockAt(anyInt(),anyInt(),anyInt())).thenReturn(block);
        for(Material material : new Material[]{Material.AIR,Material.CACTUS,Material.STONE,Material.LAVA,Material.FENCE,Material.STEP}) {
            when(block.getType()).thenReturn(material);
            assertNull(SafeSpawnResolver.resolve(new Location(world,0,64,0)));
        }
        assertNull(SafeSpawnResolver.resolve(null));
    }
}
