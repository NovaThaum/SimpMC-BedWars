package com.andrei1058.bedwars.shop.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.*;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.arena.*;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RecallScrollListenerTest {
    MainConfig original; VersionSupport nms; MockedStatic<Arena> arenas;
    MockedStatic<Bukkit> bukkit; MockedStatic<Language> language; MockedStatic<SafeSpawnResolver> safe;
    Player player; IArena arena; ITeam team; PlayerInventory inventory; ItemStack[] contents;
    ItemStack scroll; World world; Location spawn; RecallScrollListener listener; BukkitTask task;
    @BeforeEach void setup() {
        original=BedWars.config; BedWars.config=mock(MainConfig.class); nms=BedWars.nms;
        BedWars.nms=mock(VersionSupport.class);
        arenas=mockStatic(Arena.class); bukkit=mockStatic(Bukkit.class); language=mockStatic(Language.class); safe=mockStatic(SafeSpawnResolver.class);
        player=mock(Player.class); arena=mock(IArena.class); team=mock(ITeam.class); world=mock(World.class);
        inventory=mock(PlayerInventory.class); contents=new ItemStack[36]; scroll=new ItemStack(Material.PAPER,2); contents[4]=scroll;
        when(player.getInventory()).thenReturn(inventory); when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true); when(player.getWorld()).thenReturn(world);
        spawn=new Location(world,.5,64,.5); when(player.getLocation()).thenReturn(spawn);
        when(arena.getWorld()).thenReturn(world); when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.isPlayer(player)).thenReturn(true); when(arena.getTeam(player)).thenReturn(team); when(team.getSpawn()).thenReturn(spawn);
        arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
        safe.when(() -> SafeSpawnResolver.resolve(spawn)).thenReturn(spawn);
        when(inventory.getContents()).thenAnswer(c -> contents);
        when(inventory.getItem(anyInt())).thenAnswer(c -> contents[c.getArgument(0)]);
        doAnswer(c -> { contents[c.getArgument(0)]=c.getArgument(1); return null; }).when(inventory).setItem(anyInt(),nullable(ItemStack.class));
        when(inventory.addItem(any(ItemStack.class))).thenAnswer(c -> {
            ItemStack item=c.getArgument(0); if(contents[4]==null) contents[4]=item; else contents[4].setAmount(contents[4].getAmount()+item.getAmount());
            return new HashMap<Integer,ItemStack>();
        });
        when(BedWars.nms.isCustomBedWarsItem(any())).thenAnswer(c -> c.getArgument(0)!=null && ((ItemStack)c.getArgument(0)).getType()==Material.PAPER);
        when(BedWars.nms.getCustomData(any())).thenReturn(RecallScrollListener.ITEM_DATA);
        BukkitScheduler scheduler=mock(BukkitScheduler.class); task=mock(BukkitTask.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTaskTimer(any(),any(Runnable.class),eq(20L),eq(20L))).thenReturn(task);
        listener=new RecallScrollListener();
    }
    @AfterEach void cleanup() { safe.close(); language.close(); bukkit.close(); arenas.close(); BedWars.nms=nms; BedWars.config=original; }
    void use() {
        PlayerInteractEvent event=mock(PlayerInteractEvent.class); when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getItem()).thenReturn(scroll); when(event.getPlayer()).thenReturn(player); listener.onUse(event);
        verify(event).setCancelled(true);
    }
    void ticks(int count) throws Exception { var method=RecallScrollListener.class.getDeclaredMethod("tick"); method.setAccessible(true); for(int i=0;i<count;i++) method.invoke(listener); }
    @Test void waitsFiveSecondsConsumesExactlyOneOnlyOnSuccess() throws Exception {
        when(player.teleport(spawn,PlayerTeleportEvent.TeleportCause.PLUGIN)).thenReturn(true);
        use(); ticks(4); verify(player,never()).teleport(any(Location.class),any(PlayerTeleportEvent.TeleportCause.class));
        assertEquals(2,contents[4].getAmount()); ticks(1); assertEquals(1,contents[4].getAmount());
        verify(player).setFallDistance(0); verify(task).cancel();
        verify(BedWars.nms).playAction(player,"§a已返回队伍出生点。");
    }
    @Test void cancelledAndExceptionalTeleportsRefundAndNeverAnnounceSuccess() throws Exception {
        use(); ticks(5); assertEquals(2,contents[4].getAmount());
        verify(BedWars.nms,never()).playAction(player,"§a已返回队伍出生点。");
        when(player.teleport(any(Location.class),any(PlayerTeleportEvent.TeleportCause.class))).thenThrow(new IllegalStateException("cancel"));
        use(); ticks(5); assertEquals(2,contents[4].getAmount());
    }
    @Test void missingScrollOrUnsafeSpawnNeverTeleportsOrConsumes() throws Exception {
        use(); contents[4]=null; ticks(5);
        verify(player,never()).teleport(any(Location.class),any(PlayerTeleportEvent.TeleportCause.class));
        contents[4]=scroll; safe.when(() -> SafeSpawnResolver.resolve(spawn)).thenReturn(null);
        use(); ticks(5); assertEquals(2,contents[4].getAmount());
    }
    @Test void deadRespawningOrArenaChangedInvalidatesChannel() throws Exception {
        when(player.isDead()).thenReturn(true); use(); ticks(5);
        when(player.isDead()).thenReturn(false); use(); when(arena.isReSpawning(player)).thenReturn(true); ticks(1);
        when(arena.isReSpawning(player)).thenReturn(false); use(); arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(null); ticks(5);
        verify(player,never()).teleport(any(Location.class),any(PlayerTeleportEvent.TeleportCause.class)); assertEquals(2,contents[4].getAmount());
    }
    @Test void rotationKeepsChannelButMovementCancelsAndParticlesOnlyReachNearbySameArena() throws Exception {
        Player nearby=mock(Player.class), distant=mock(Player.class);
        when(nearby.getWorld()).thenReturn(world); when(distant.getWorld()).thenReturn(world);
        when(nearby.getLocation()).thenReturn(spawn.clone().add(3,0,0)); when(distant.getLocation()).thenReturn(spawn.clone().add(25,0,0));
        when(arena.getPlayers()).thenReturn(new LinkedList<>(List.of(player,nearby,distant)));
        use(); verify(BedWars.nms).playRecallEffect(eq(nearby),any()); verify(BedWars.nms,never()).playRecallEffect(eq(distant),any());
        listener.onMove(new PlayerMoveEvent(player,spawn, new Location(world,.5,64,.5,90,0))); ticks(1);
        listener.onMove(new PlayerMoveEvent(player,spawn,spawn.clone().add(.01,0,0))); ticks(5);
        verify(player,never()).teleport(any(Location.class),any(PlayerTeleportEvent.TeleportCause.class)); assertEquals(2,contents[4].getAmount());
    }
    @Test void languageOverridesSurviveInstallation() {
        Language lang=mock(Language.class); var yaml=new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("recall-scroll.completed","&a自定义完成"); when(lang.getYml()).thenReturn(yaml);
        com.andrei1058.bedwars.language.RecallMessages.install(lang);
        language.when(() -> Language.getPlayerLanguage(player)).thenReturn(lang);
        assertEquals("§a自定义完成",com.andrei1058.bedwars.language.RecallMessages.text(player,com.andrei1058.bedwars.language.RecallMessages.Key.COMPLETED));
    }
}
