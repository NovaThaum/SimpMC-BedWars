package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 1.8-compatible inventory interface for fixed pre-game squads. */
public final class PreGameSquadGUI implements Listener {
    private static final PreGameSquadGUI INSTANCE = new PreGameSquadGUI();
    private static final int[] MEMBER_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private final PreGameSquadManager squads = PreGameSquadManager.getInstance();

    private PreGameSquadGUI() { }

    public static PreGameSquadGUI getInstance() { return INSTANCE; }

    public void open(Player player) {
        IArena arena = preGameArena(player);
        if (arena == null) {
            player.sendMessage(PreGameSquadMessages.get(player, "not-pregame"));
            return;
        }
        if (arena.getMaxInTeam() <= 1) {
            player.sendMessage(PreGameSquadMessages.get(player, "solo"));
            return;
        }

        SquadHolder holder = new SquadHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, PreGameSquadMessages.get(player, "title"));
        holder.inventory = inventory;
        renderMembers(inventory, player, arena);
        renderActions(inventory, holder, player, arena);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof SquadHolder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        SquadHolder holder = (SquadHolder) event.getView().getTopInventory().getHolder();
        if (!holder.viewer.equals(player.getUniqueId())) return;
        SlotAction action = holder.actions.get(event.getRawSlot());
        if (action == null) return;
        if (action.close) { player.closeInventory(); return; }
        String command = event.getClick() == ClickType.RIGHT && action.secondary != null
                ? action.secondary : action.primary;
        if (command == null) return;
        Bukkit.dispatchCommand(player, BedWars.mainCmd + " team " + command);
        Bukkit.getScheduler().runTask(BedWars.plugin, new Runnable() {
            @Override public void run() {
                if (player.isOnline() && preGameArena(player) != null) open(player);
            }
        });
    }

    private void renderMembers(Inventory inventory, Player player, IArena arena) {
        List<Player> members = squads.getMembers(player);
        Player leader = squads.getLeader(player);
        for (int i = 0; i < members.size() && i < MEMBER_SLOTS.length; i++) {
            Player member = members.get(i);
            String suffix = member.equals(leader) ? PreGameSquadMessages.get(player, "leader") : "";
            inventory.setItem(MEMBER_SLOTS[i], head(member,
                    PreGameSquadMessages.get(player, "member").replace("{name}", member.getName()).replace("{leader}", suffix),
                    PreGameSquadMessages.get(player, "member-lore")));
        }
        inventory.setItem(4, named(Material.BOOK, PreGameSquadMessages.get(player, "info")
                        .replace("{count}", String.valueOf(members.size())).replace("{max}", String.valueOf(arena.getMaxInTeam())),
                PreGameSquadMessages.get(player, "lore-invite"), PreGameSquadMessages.get(player, "lore-pending"), "",
                PreGameSquadMessages.get(player, "lore-command")));
    }

    private void renderActions(Inventory inventory, SquadHolder holder, Player player, IArena arena) {
        int index = 0;
        for (Player inviter : squads.getPendingInviters(player)) {
            if (index >= 36) break;
            int slot = 9 + index++;
            inventory.setItem(slot, head(inviter, PreGameSquadMessages.get(player, "accept").replace("{name}", inviter.getName()),
                    PreGameSquadMessages.get(player, "accept-left"), PreGameSquadMessages.get(player, "accept-right")));
            holder.actions.put(slot, new SlotAction("accept " + inviter.getName(), "decline " + inviter.getName(), false));
        }
        if (squads.isLeader(player) && squads.getMembers(player).size() < arena.getMaxInTeam()) {
            for (Player target : squads.getAvailableTargets(player)) {
                if (index >= 36) break;
                int slot = 9 + index++;
                inventory.setItem(slot, head(target, PreGameSquadMessages.get(player, "invite").replace("{name}", target.getName()),
                        PreGameSquadMessages.get(player, "invite-lore")));
                holder.actions.put(slot, new SlotAction("invite " + target.getName(), null, false));
            }
        }
        if (squads.getMembers(player).size() > 1) {
            inventory.setItem(49, named(Material.BARRIER, PreGameSquadMessages.get(player, "leave"),
                    PreGameSquadMessages.get(player, "leave-lore")));
            holder.actions.put(49, new SlotAction("leave", null, false));
        }
        inventory.setItem(53, named(Material.WOOD_DOOR, PreGameSquadMessages.get(player, "close")));
        holder.actions.put(53, new SlotAction(null, null, true));
    }

    private static ItemStack head(Player player, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwner(player.getName());
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return null;
        GameState state = arena.getStatus();
        return state == GameState.waiting || state == GameState.starting ? arena : null;
    }

    static final class SquadHolder implements InventoryHolder {
        private final UUID viewer;
        private final Map<Integer, SlotAction> actions = new HashMap<>();
        private Inventory inventory;
        SquadHolder(UUID viewer) { this.viewer = viewer; }
        @Override public Inventory getInventory() { return inventory; }
    }

    static final class SlotAction {
        private final String primary, secondary;
        private final boolean close;
        SlotAction(String primary, String secondary, boolean close) {
            this.primary = primary; this.secondary = secondary; this.close = close;
        }
    }
}
