package com.andrei1058.bedwars.seasonal;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.shop.ShopOpenEvent;
import com.andrei1058.bedwars.halloween.shop.PumpkinContent;
import com.andrei1058.bedwars.shop.ShopManager;
import com.andrei1058.bedwars.shop.main.ShopCategory;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SeasonalEffects implements Listener {
    private final boolean enabled;
    private final List<Window> windows;

    private SeasonalEffects() {
        enabled = BedWars.config.getYml().getBoolean("seasonal-effects.enabled", true);
        boolean halloweenShopEnabled = BedWars.config.getBoolean(com.andrei1058.bedwars.api.configuration.ConfigPath.GENERAL_CONFIGURATION_ENABLE_HALLOWEEN);
        windows = new ArrayList<>();
        for (String id : BedWars.config.getYml().getConfigurationSection("seasonal-effects.windows").getKeys(false)) {
            String path = "seasonal-effects.windows." + id;
            if (!BedWars.config.getYml().getBoolean(path + ".enabled", true)) continue;
            try {
                SeasonalEffectSchedule schedule = SeasonalEffectSchedule.parse(
                        BedWars.config.getYml().getString(path + ".start", ""), BedWars.config.getYml().getString(path + ".end", ""));
                String effect = BedWars.config.getYml().getString(path + ".effect", "firework").toLowerCase(Locale.ROOT);
                if (!effect.equals("firework") && !effect.equals("cobweb")) {
                    throw new IllegalArgumentException("effect 仅支持 firework 或 cobweb");
                }
                List<String> triggers = new ArrayList<>();
                for (String trigger : BedWars.config.getYml().getStringList(path + ".triggers")) {
                    triggers.add(trigger.toLowerCase(Locale.ROOT));
                }
                windows.add(new Window(id, effect, schedule, triggers,
                        halloweenShopEnabled && BedWars.config.getYml().getBoolean(path + ".halloween-shop", false)));
            } catch (IllegalArgumentException exception) {
                BedWars.plugin.getLogger().warning("定时效果窗口 " + id + " 配置无效，已跳过：" + exception.getMessage());
            }
        }
    }

    public static void init(Plugin plugin) {
        SeasonalEffects effects = new SeasonalEffects();
        if (!effects.enabled) return;
        Bukkit.getPluginManager().registerEvents(effects, plugin);
        plugin.getLogger().info("已加载定时效果规则（服务器时区 " + ZoneId.systemDefault() + "）。");
    }

    @EventHandler
    public void onShopOpen(ShopOpenEvent event) {
        if (event.getArena() == null || event.getArena().getStatus() != GameState.playing) return;
        for (Window window : windows) {
            if (window.id.equalsIgnoreCase("halloween") && window.halloweenShop && window.schedule.isActiveNow()) {
                addPumpkinShopItem();
                return;
            }
        }
    }

    private void addPumpkinShopItem() {
        if (ShopManager.getShop() == null) return;
        for (ShopCategory category : ShopManager.getShop().getCategoryList()) {
            if (!category.getName().equals("blocks-category")) continue;
            for (com.andrei1058.bedwars.shop.main.CategoryContent content : category.getCategoryContentList()) {
                if (content.getIdentifier().equals("halloween-special-pumpkin")) return;
            }
            PumpkinContent content = new PumpkinContent(category);
            if (content.isLoaded()) category.getCategoryContentList().add(content);
            return;
        }
    }

    @EventHandler
    public void onKill(PlayerKillEvent event) {
        if (event.getKiller() == null) return;
        play("kill", event.getArena(), event.getVictim().getLocation().add(0, 1, 0));
    }

    @EventHandler
    public void onBedBreak(PlayerBedBreakEvent event) {
        play("bed-break", event.getArena(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onVictory(GameEndEvent event) {
        for (UUID winner : event.getWinners()) {
            Player player = Bukkit.getPlayer(winner);
            if (player != null && player.getWorld().equals(event.getArena().getWorld())) {
                play("victory", event.getArena(), player.getLocation());
            }
        }
    }

    private void play(String trigger, IArena arena, Location location) {
        if (location.getWorld() == null || arena == null || arena.getWorld() == null
                || !location.getWorld().equals(arena.getWorld())) return;
        for (Window window : windows) {
            if (!window.triggers.contains(trigger) || !window.schedule.isActiveNow()) continue;
            Effect effect = window.effect.equals("firework") ? Effect.FIREWORKS_SPARK : Effect.TILE_BREAK;
            int data = window.effect.equals("firework") ? 0 : Material.WEB.getId();
            for (Player viewer : arena.getWorld().getPlayers()) {
                viewer.spigot().playEffect(location, effect, data, 0, 0.35f, 0.5f, 0.35f, 0.05f, 24, 32);
            }
        }
    }

    private static final class Window {
        private final String id;
        private final String effect;
        private final SeasonalEffectSchedule schedule;
        private final List<String> triggers;
        private final boolean halloweenShop;

        private Window(String id, String effect, SeasonalEffectSchedule schedule, List<String> triggers, boolean halloweenShop) {
            this.id = id;
            this.effect = effect;
            this.schedule = schedule;
            this.triggers = triggers;
            this.halloweenShop = halloweenShop;
        }
    }
}
