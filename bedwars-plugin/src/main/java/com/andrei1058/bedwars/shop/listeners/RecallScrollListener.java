package com.andrei1058.bedwars.shop.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SafeSpawnResolver;
import com.andrei1058.bedwars.language.RecallMessages;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public final class RecallScrollListener implements Listener {
    public static final String ITEM_DATA = "RECALL_SCROLL";
    private final Map<UUID, Channel> channels = new HashMap<>();
    private BukkitTask ticker;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!matches(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (channels.containsKey(player.getUniqueId())) { show(player, RecallMessages.Key.ALREADY_CHANNELING); return; }
        IArena arena = Arena.getArenaByPlayer(player);
        ITeam team = validTeam(player, arena);
        if (team == null) { show(player, RecallMessages.Key.UNAVAILABLE); return; }
        Channel channel = new Channel(player, arena, team);
        channels.put(player.getUniqueId(), channel);
        announce(channel);
        if (ticker == null) ticker = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::tick, 20L, 20L);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) cancel(player);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo(), from = event.getFrom();
        if (to != null && (from.getWorld() != to.getWorld() || from.getX() != to.getX()
                || from.getY() != to.getY() || from.getZ() != to.getZ())) cancel(event.getPlayer());
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        channels.remove(event.getPlayer().getUniqueId()); stopIfIdle();
    }
    private void tick() {
        // 传送或插件回调可能同步修改 channels，不能持有 Map 迭代器。
        for (Channel channel : new ArrayList<>(channels.values())) {
            Player player = channel.player;
            if (channels.get(player.getUniqueId()) != channel) continue;
            if (validTeam(player, channel.arena) != channel.team) { cancel(player); continue; }
            if (--channel.seconds > 0) { announce(channel); continue; }
            channels.remove(player.getUniqueId());
            finish(channel);
        }
        stopIfIdle();
    }
    private static void finish(Channel channel) {
        Player player = channel.player;
        Location destination = SafeSpawnResolver.resolve(channel.team.getSpawn());
        int slot = scrollSlot(player);
        if (destination == null || slot < 0 || validTeam(player, channel.arena) != channel.team) {
            show(player, RecallMessages.Key.FAILED); return;
        }
        // 暂存扣除，防止传送事件回调移动物品；失败时完整归还。
        ItemStack reserved = player.getInventory().getItem(slot).clone();
        reserved.setAmount(1);
        ItemStack stack = player.getInventory().getItem(slot).clone();
        if (stack.getAmount() == 1) player.getInventory().setItem(slot, null);
        else { stack.setAmount(stack.getAmount() - 1); player.getInventory().setItem(slot, stack); }
        boolean success = false;
        try { success = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN); }
        catch (RuntimeException error) {
            if (BedWars.plugin != null) BedWars.plugin.getLogger().warning("回城卷轴传送失败：" + error);
        }
        if (!success) {
            for (ItemStack remaining : player.getInventory().addItem(reserved).values())
                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        } else player.setFallDistance(0.0F);
        player.updateInventory();
        if (player.isOnline()) show(player, success ? RecallMessages.Key.COMPLETED : RecallMessages.Key.FAILED);
    }
    private static int scrollSlot(Player player) {
        ItemStack[] storage = player.getInventory().getContents();
        for (int slot = 0; slot < storage.length; slot++)
            if (matches(storage[slot]) && storage[slot].getAmount() > 0) return slot;
        return -1;
    }
    private void announce(Channel channel) {
        BedWars.nms.playAction(channel.player, RecallMessages.text(channel.player, RecallMessages.Key.COUNTDOWN)
                .replace("{time}", String.valueOf(channel.seconds)));
        Location source = channel.player.getLocation();
        java.util.Set<Player> viewers = new java.util.HashSet<>(channel.arena.getPlayers());
        viewers.addAll(channel.arena.getSpectators());
        for (Player viewer : viewers)
            if (viewer != channel.player && viewer.getWorld().equals(source.getWorld())
                    && viewer.getLocation().distanceSquared(source) <= 24.0 * 24.0)
                BedWars.nms.playRecallEffect(viewer, source.clone().add(0, 1, 0));
    }
    private static void show(Player player, RecallMessages.Key key) { BedWars.nms.playAction(player, RecallMessages.text(player, key)); }
    private void cancel(Player player) {
        if (channels.remove(player.getUniqueId()) != null && player.isOnline()) show(player, RecallMessages.Key.CANCELLED);
        stopIfIdle();
    }
    private void stopIfIdle() { if (channels.isEmpty() && ticker != null) { ticker.cancel(); ticker = null; } }
    private static boolean matches(ItemStack item) {
        return item != null && item.getType() != Material.AIR && BedWars.nms != null
                && BedWars.nms.isCustomBedWarsItem(item) && ITEM_DATA.equalsIgnoreCase(BedWars.nms.getCustomData(item));
    }
    private static ITeam validTeam(Player player, IArena arena) {
        if (arena == null || !player.isOnline() || player.isDead() || Arena.getArenaByPlayer(player) != arena
                || arena.getStatus() != GameState.playing || !arena.isPlayer(player)
                || arena.isSpectator(player) || arena.isReSpawning(player)
                || !player.getWorld().equals(arena.getWorld())) return null;
        ITeam team = arena.getTeam(player);
        return team == null || team.getSpawn() == null || !arena.getWorld().equals(team.getSpawn().getWorld()) ? null : team;
    }
    private static final class Channel {
        final Player player; final IArena arena; final ITeam team; int seconds = 5;
        Channel(Player player, IArena arena, ITeam team) { this.player = player; this.arena = arena; this.team = team; }
    }
}
