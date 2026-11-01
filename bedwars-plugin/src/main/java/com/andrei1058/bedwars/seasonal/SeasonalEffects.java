package com.andrei1058.bedwars.seasonal;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.shop.ShopOpenEvent;
import com.andrei1058.bedwars.halloween.shop.PumpkinContent;
import com.andrei1058.bedwars.shop.ShopManager;
import com.andrei1058.bedwars.shop.main.ShopCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SeasonalEffects implements Listener {

    private static final String ROOT = "seasonal-effects";
    private final boolean enabled;
    private final List<Window> windows;

    SeasonalEffects(ConfigurationSection config) {
        enabled = config.getBoolean("enabled", true);
        windows = loadWindows(config.getConfigurationSection("windows"));
    }

    public static void init(Plugin plugin) {
        ConfigurationSection defaults = BedWars.config.getYml().getConfigurationSection(ROOT);
        if (defaults == null || !defaults.getBoolean("enabled", true)) return;
        SeasonalEffects effects = new SeasonalEffects(BedWars.config.getYml().getConfigurationSection(ROOT));
        BedWars.registerEvents(effects);
        plugin.getLogger().info("已加载定时效果规则（服务器时区 " + java.time.ZoneId.systemDefault() + "）。");
    }

    @EventHandler
    public void onShopOpen(ShopOpenEvent event) {
        if (!enabled || event.getArena() == null
                || event.getArena().getStatus() != com.andrei1058.bedwars.api.arena.GameState.playing
                || activeWindow("halloween").isEmpty()) return;
        installHalloweenShopItem();
    }

    private static void installHalloweenShopItem() {
        if (ShopManager.getShop() == null) return;
        ShopCategory blockCategory = ShopManager.getShop().getCategoryList().stream()
                .filter(category -> category.getName().equals("blocks-category")).findFirst().orElse(null);
        if (blockCategory == null || blockCategory.getCategoryContentList().stream()
                .anyMatch(content -> content.getIdentifier().equals("halloween-special-pumpkin"))) return;
        PumpkinContent content = new PumpkinContent(blockCategory);
        if (content.isLoaded()) blockCategory.getCategoryContentList().add(content);
    }

    static List<Window> loadWindows(ConfigurationSection section) {
        if (section == null) return List.of();
        List<Window> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection window = section.getConfigurationSection(id);
            if (window == null || !window.getBoolean("enabled", true)) continue;
            try {
                SeasonalEffectSchedule schedule = SeasonalEffectSchedule.parse(
                        window.getString("start", ""), window.getString("end", ""));
                List<String> effects = window.getStringList("triggers");
                String effect = window.getString("effect", "firework").trim();
                if (!List.of("firework", "cobweb").contains(effect.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("effect 仅支持 firework 或 cobweb");
                }
                result.add(new Window(id, effect, schedule, effects, window.getBoolean("halloween-shop", false)));
            } catch (IllegalArgumentException | DateTimeException exception) {
                BedWars.plugin.getLogger().warning("定时效果窗口 " + id + " 无效，已跳过：" + exception.getMessage());
            }
        }
        return List.copyOf(result);
    }

    @EventHandler
    public void onKill(PlayerKillEvent event) {
        if (event.getKiller() == null) return;
        Location location = event.getVictim().getLocation().add(0, 1, 0);
        active("kill").forEach(window -> play(window, location, event.getArena(), event.getArena().getWorld().getPlayers()));
    }

    @EventHandler
    public void onBedBreak(PlayerBedBreakEvent event) {
        active("bed-break").forEach(window -> play(window, event.getPlayer().getLocation(), event.getArena(), event.getArena().getWorld().getPlayers()));
    }

    @EventHandler
    public void onVictory(GameEndEvent event) {
        for (Window window : active("victory")) {
            for (java.util.UUID winner : event.getWinners()) {
                Player player = org.bukkit.Bukkit.getPlayer(winner);
                if (player != null && player.getWorld().equals(event.getArena().getWorld())) {
                    play(window, player.getLocation(), event.getArena(), event.getArena().getWorld().getPlayers());
                }
            }
        }
    }

    private List<Window> active(String trigger) {
        if (!enabled) return List.of();
        return windows.stream().filter(window -> window.triggers().contains(trigger))
                .filter(window -> window.schedule().isActiveNow()).toList();
    }

    private List<Window> activeWindow(String id) {
        if (!enabled) return List.of();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return windows.stream().filter(window -> window.id().equalsIgnoreCase(id) && window.halloweenShop())
                .filter(window -> window.schedule().isActive(now)).toList();
    }

    private static void play(Window window, Location location, com.andrei1058.bedwars.api.arena.IArena arena,
                             List<Player> recipients) {
        if (location.getWorld() == null) return;
        if (arena == null || arena.getWorld() == null || !location.getWorld().equals(arena.getWorld())) return;
        List<Player> viewers = recipients.stream().filter(player -> player.getWorld().equals(arena.getWorld())).toList();
        String effect = window.effect();
        if ("firework".equalsIgnoreCase(effect)) {
            viewers.forEach(player -> {
                player.spawnParticle(Particle.FIREWORK, location, 36, 0.6, 0.8, 0.6, 0.02);
                player.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 18, 0.45, 0.65, 0.45, 0.05);
                player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            });
        } else if ("cobweb".equalsIgnoreCase(effect)) {
            viewers.forEach(player -> {
                player.spawnParticle(Particle.BLOCK, location, 24, 0.35, 0.5, 0.35, 0.1,
                        org.bukkit.Material.COBWEB.createBlockData());
                player.playSound(location, Sound.ENTITY_GHAST_SCREAM, 1.0f, 1.0f);
            });
        }
    }

    record Window(String id, String effect, SeasonalEffectSchedule schedule, List<String> triggers,
                  boolean halloweenShop) {
        Window {
            triggers = triggers.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
        }
    }
}
