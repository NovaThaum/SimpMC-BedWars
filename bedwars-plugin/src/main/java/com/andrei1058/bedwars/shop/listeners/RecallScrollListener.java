package com.andrei1058.bedwars.shop.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Five-second BedWars return channel for the 1.8 shop item. */
public final class RecallScrollListener implements Listener {
    public static final String ITEM_DATA = "RECALL_SCROLL";
    private final Map<UUID, Channel> channels = new HashMap<>();
    private BukkitTask ticker;
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!matches(event.getItem())) return;
        event.setCancelled(true);
        if (channels.containsKey(player.getUniqueId())) { player.sendMessage("§e回城卷轴正在引导中。"); return; }
        IArena arena = Arena.getArenaByPlayer(player);
        ITeam team = validTeam(player, arena);
        if (team == null) { player.sendMessage("§c当前无法使用回城卷轴。"); return; }
        channels.put(player.getUniqueId(), new Channel(player, arena, team, 5));
        player.sendMessage("§b回城卷轴将在 §f5 §b秒后生效，移动或受到伤害会取消。");
        ensureTicker();
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) { if (event.getEntity() instanceof Player) cancel((Player) event.getEntity(), "§c回城卷轴已因受到伤害而取消。"); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) { if (event.getTo() != null && changed(event.getFrom(), event.getTo())) cancel(event.getPlayer(), "§c回城卷轴已因移动而取消。"); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { channels.remove(event.getPlayer().getUniqueId()); stopIfIdle(); }
    private void ensureTicker() { if (ticker == null) ticker = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::tick, 20L, 20L); }
    private void tick() {
        Iterator<Channel> it = channels.values().iterator();
        while (it.hasNext()) {
            Channel c = it.next();
            if (validTeam(c.player, c.arena) != c.team) { c.player.sendMessage("§c回城卷轴已失效。"); it.remove(); continue; }
            c.seconds--;
            if (c.seconds > 0) { c.player.sendMessage("§b回城倒计时：§f" + c.seconds + "§b 秒"); continue; }
            if (!consume(c.player)) { c.player.sendMessage("§c背包中没有回城卷轴，传送失败。"); it.remove(); continue; }
            Location spawn = c.team.getSpawn();
            if (spawn != null && spawn.getWorld() != null) { c.player.teleport(spawn); c.player.setFallDistance(0.0F); c.player.sendMessage("§a已返回队伍出生点。"); }
            it.remove();
        }
        stopIfIdle();
    }
    private void cancel(Player p, String msg) { if (channels.remove(p.getUniqueId()) != null) p.sendMessage(msg); stopIfIdle(); }
    private void stopIfIdle() { if (channels.isEmpty() && ticker != null) { ticker.cancel(); ticker = null; } }
    private static boolean matches(ItemStack item) { return item != null && item.getType() != Material.AIR && BedWars.nms.isCustomBedWarsItem(item) && ITEM_DATA.equalsIgnoreCase(BedWars.nms.getCustomData(item)); }
    private static boolean consume(Player player) { ItemStack[] items = player.getInventory().getContents(); for (int i = 0; i < items.length; i++) { ItemStack item = items[i]; if (!matches(item) || item.getAmount() < 1) continue; if (item.getAmount() == 1) player.getInventory().setItem(i, null); else item.setAmount(item.getAmount() - 1); player.updateInventory(); return true; } return false; }
    private static ITeam validTeam(Player player, IArena arena) { if (arena == null || !player.isOnline() || arena.getStatus() != GameState.playing || !arena.isPlayer(player) || arena.isSpectator(player)) return null; ITeam team = arena.getTeam(player); return team == null || team.getSpawn() == null ? null : team; }
    private static boolean changed(Location a, Location b) { return a.getWorld() != b.getWorld() || a.getX() != b.getX() || a.getY() != b.getY() || a.getZ() != b.getZ(); }
    private static final class Channel { final Player player; final IArena arena; final ITeam team; int seconds; Channel(Player p, IArena a, ITeam t, int s) { player=p; arena=a; team=t; seconds=s; } }
}
