package com.andrei1058.bedwars.support.npc;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Sounds;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/** 1.8.8 原生数据包 NPC：不创建真实玩家或世界实体，无需安装 Citizens。 */
public final class NativeJoinNpcs implements Listener {
  private static NativeJoinNpcs instance;
  private final BedWars plugin;
  private final java.util.function.Supplier<Collection<? extends Player>> online;
  private final Path file;
  private final YamlConfiguration config = new YamlConfiguration();
  private final Map<UUID, JoinNpcDefinition> definitions = new LinkedHashMap<>();
  private final Map<UUID, Model> models = new HashMap<>();
  private final Map<UUID, Set<UUID>> visible = new HashMap<>();
  private final Map<Integer, UUID> clickIds = new ConcurrentHashMap<>();
  private final Map<UUID, Long> clicked = new HashMap<>();
  private final Set<String> fetching = new HashSet<>();
  private volatile boolean running = true;
  private static final String HANDLER = "simpmc_join_npc";
  private static final java.lang.reflect.Field USE_ID;

  static {
    try {
      USE_ID = PacketPlayInUseEntity.class.getDeclaredField("a");
      USE_ID.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static NativeJoinNpcs get() {
    return instance;
  }

  public NativeJoinNpcs(BedWars plugin) {
    this(plugin, () -> Bukkit.getOnlinePlayers());
  }

  NativeJoinNpcs(BedWars plugin, java.util.function.Supplier<Collection<? extends Player>> online) {
    this.plugin = plugin;
    this.online = online;
    this.file = plugin.getDataFolder().toPath().resolve("join-npcs.yml");
    try {
      if (Files.exists(file)) config.load(file.toFile());
      if (config.isSet("npcs") && !config.isConfigurationSection("npcs"))
        throw new IOException("npcs 必须是配置节");
      if (config.isConfigurationSection("npcs"))
        for (String id : config.getConfigurationSection("npcs").getKeys(false)) {
          String p = "npcs." + id + ".";
          JoinNpcDefinition d =
              new JoinNpcDefinition(
                  UUID.fromString(id),
                  config.getString(p + "world"),
                  config.getDouble(p + "x"),
                  config.getDouble(p + "y"),
                  config.getDouble(p + "z"),
                  (float) config.getDouble(p + "yaw"),
                  (float) config.getDouble(p + "pitch"),
                  config.getString(p + "skin"),
                  config.getString(p + "group"),
                  config.getString(p + "text"));
          definitions.put(d.id(), d);
        }
    } catch (Exception e) {
      throw new IllegalStateException("无法读取 join-npcs.yml，已停止内置 NPC 初始化，原文件保持不变", e);
    }
    instance = this;
    Bukkit.getPluginManager().registerEvents(this, plugin);
    definitions.values().forEach(this::build);
    for (Player player : online.get()) {
      inject(player);
      sync(player);
    }
  }

  public JoinNpcDefinition create(Location l, String skin, String group, String text)
      throws IOException {
    JoinNpcDefinition d =
        new JoinNpcDefinition(
            UUID.randomUUID(),
            l.getWorld().getName(),
            l.getX(),
            l.getY(),
            l.getZ(),
            l.getYaw(),
            l.getPitch(),
            skin,
            group,
            text);
    definitions.put(d.id(), d);
    try {
      persist();
    } catch (IOException e) {
      definitions.remove(d.id());
      throw e;
    }
    build(d);
    online.get().forEach(this::sync);
    return d;
  }

  public boolean removeTarget(Player player) throws IOException {
    JoinNpcDefinition target =
        definitions.values().stream()
            .filter(d -> player.getWorld().getName().equals(d.world()))
            .min(
                Comparator.comparingDouble(
                    d -> JoinNpcDefinition.targetDistance(player.getEyeLocation(), d.location())))
            .orElse(null);
    if (target == null
        || !Double.isFinite(
            JoinNpcDefinition.targetDistance(player.getEyeLocation(), target.location())))
      return false;
    definitions.remove(target.id());
    try {
      persist();
    } catch (IOException e) {
      definitions.put(target.id(), target);
      throw e;
    }
    discard(target.id());
    return true;
  }

  private void persist() throws IOException {
    config.set("npcs", null);
    for (JoinNpcDefinition d : definitions.values()) {
      String p = "npcs." + d.id() + ".";
      config.set(p + "world", d.world());
      config.set(p + "x", d.x());
      config.set(p + "y", d.y());
      config.set(p + "z", d.z());
      config.set(p + "yaw", d.yaw());
      config.set(p + "pitch", d.pitch());
      config.set(p + "skin", d.skin());
      config.set(p + "group", d.group());
      config.set(p + "text", d.text());
    }
    Files.createDirectories(file.getParent());
    Path temp = Files.createTempFile(file.getParent(), "join-npcs-", ".tmp");
    try {
      Files.writeString(
          temp,
          "# 内置大厅 NPC；使用 /bw npc create 和 /bw npc remove 管理。\n" + config.saveToString(),
          StandardCharsets.UTF_8);
      try {
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private void build(JoinNpcDefinition d) {
    if (Bukkit.getWorld(d.world()) == null || models.containsKey(d.id())) return;
    Location l = d.location();
    WorldServer world = ((CraftWorld) l.getWorld()).getHandle();
    GameProfile profile =
        new GameProfile(d.id(), "bw" + d.id().toString().replace("-", "").substring(0, 14));
    String path = "skins." + d.skin().toLowerCase(Locale.ROOT) + ".";
    if (config.isString(path + "value") && config.isString(path + "signature"))
      profile
          .getProperties()
          .put(
              "textures",
              new Property(
                  "textures",
                  config.getString(path + "value"),
                  config.getString(path + "signature")));
    EntityPlayer entity =
        new EntityPlayer(
            ((CraftServer) Bukkit.getServer()).getServer(),
            world,
            profile,
            new PlayerInteractManager(world));
    entity.setLocation(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    entity.getDataWatcher().watch(10, (byte) 127);
    ScoreboardTeam team =
        new ScoreboardTeam(new Scoreboard(), "bw" + Integer.toHexString(entity.getId()));
    team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.NEVER);
    team.getPlayerNameSet().add(profile.getName());
    List<EntityArmorStand> lines = new ArrayList<>();
    List<String> text = d.rendered(Arena.getPlayers(d.group()));
    for (int i = 0; i < text.size(); i++) {
      EntityArmorStand stand =
          new EntityArmorStand(
              world, l.getX(), l.getY() + 1.75 + (text.size() - 1 - i) * .28, l.getZ());
      stand.setInvisible(true);
      stand.setGravity(false);
      stand.n(true);
      stand.setCustomName(text.get(i));
      stand.setCustomNameVisible(true);
      lines.add(stand);
    }
    models.put(d.id(), new Model(entity, team, lines));
    clickIds.put(entity.getId(), d.id());
    if (!profile.getProperties().containsKey("textures")) fetchSkin(d.skin());
  }

  private boolean eligible(Player p, JoinNpcDefinition d) {
    return p.isOnline()
        && !Arena.isInArena(p)
        && !SetupSession.isInSetupSession(p.getUniqueId())
        && p.getWorld().getName().equals(d.world())
        && p.getLocation().distanceSquared(d.location()) <= 48 * 48;
  }

  private void sync(Player p) {
    if (!running) return;
    Set<UUID> seen = visible.computeIfAbsent(p.getUniqueId(), u -> new HashSet<>());
    for (JoinNpcDefinition d : definitions.values()) {
      Model model = models.get(d.id());
      if (model == null) continue;
      if (eligible(p, d)) {
        if (seen.add(d.id())) show(p, d.id(), model);
      } else if (seen.remove(d.id())) hide(p, model);
    }
  }

  private void show(Player p, UUID id, Model m) {
    send(p, new PacketPlayOutScoreboardTeam(m.team, 0));
    send(
        p,
        new PacketPlayOutPlayerInfo(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, m.player));
    send(p, new PacketPlayOutNamedEntitySpawn(m.player));
    send(p, new PacketPlayOutEntityHeadRotation(m.player, (byte) (m.player.yaw * 256 / 360)));
    for (EntityArmorStand line : m.lines) send(p, new PacketPlayOutSpawnEntityLiving(line));
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (running
                  && models.get(id) == m
                  && visible.getOrDefault(p.getUniqueId(), Set.of()).contains(id)
                  && p.isOnline())
                send(
                    p,
                    new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, m.player));
            },
            40L);
  }

  private static void send(Player p, Packet<?> packet) {
    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
  }

  private void hide(Player p, Model m) {
    int[] ids = new int[m.lines.size() + 1];
    ids[0] = m.player.getId();
    for (int i = 0; i < m.lines.size(); i++) ids[i + 1] = m.lines.get(i).getId();
    send(p, new PacketPlayOutEntityDestroy(ids));
    send(
        p,
        new PacketPlayOutPlayerInfo(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, m.player));
    send(p, new PacketPlayOutScoreboardTeam(m.team, 1));
  }

  private void discard(UUID id) {
    Model m = models.remove(id);
    if (m == null) return;
    clickIds.remove(m.player.getId());
    for (Player p : online.get())
      if (visible.containsKey(p.getUniqueId()) && visible.get(p.getUniqueId()).remove(id))
        hide(p, m);
  }

  public static void updateGroup(String group) {
    if (instance == null || !instance.running) return;
    for (JoinNpcDefinition d : instance.definitions.values())
      if (Arrays.stream(d.group().split("\\+")).anyMatch(g -> g.equalsIgnoreCase(group))) {
        Model m = instance.models.get(d.id());
        if (m == null) continue;
        List<String> text = d.rendered(Arena.getPlayers(d.group()));
        for (int i = 0; i < m.lines.size(); i++) {
          EntityArmorStand line = m.lines.get(i);
          line.setCustomName(text.get(i));
          for (Player p : instance.online.get())
            if (instance.visible.getOrDefault(p.getUniqueId(), Set.of()).contains(d.id()))
              send(p, new PacketPlayOutEntityMetadata(line.getId(), line.getDataWatcher(), true));
        }
      }
    for (Player p : instance.online.get()) instance.sync(p);
  }

  private static boolean clearSight(Player player, JoinNpcDefinition d) {
    Location eye = player.getEyeLocation();
    org.bukkit.util.Vector delta = d.location().add(0, 1, 0).toVector().subtract(eye.toVector());
    if (delta.lengthSquared() < .01) return true;
    org.bukkit.util.BlockIterator ray =
        new org.bukkit.util.BlockIterator(
            player.getWorld(),
            eye.toVector(),
            delta.normalize(),
            0,
            (int) Math.ceil(eye.distance(d.location().add(0, 1, 0))));
    while (ray.hasNext()) if (ray.next().getType().isSolid()) return false;
    return true;
  }

  private void resetViewer(Player p) {
    Set<UUID> seen = visible.remove(p.getUniqueId());
    if (seen != null)
      for (UUID id : seen) {
        Model m = models.get(id);
        if (m != null) hide(p, m);
      }
  }

  private void click(Player p, UUID id) {
    JoinNpcDefinition d = definitions.get(id);
    if (!running
        || d == null
        || !eligible(p, d)
        || !clearSight(p, d)
        || p.isSneaking()
        || p.getLocation().distanceSquared(d.location()) > 36
        || !visible.getOrDefault(p.getUniqueId(), Set.of()).contains(id)) return;
    long now = System.nanoTime();
    Long last = clicked.get(p.getUniqueId());
    if (last != null && now - last < 500_000_000L) return;
    clicked.put(p.getUniqueId(), now);
    if (!Arena.joinRandomFromGroup(p, d.group())) {
      p.sendMessage(Language.getMsg(p, Messages.COMMAND_JOIN_NO_EMPTY_FOUND));
      Sounds.playSound("join-denied", p);
    } else Sounds.playSound("join-allowed", p);
    sync(p);
  }

  private void inject(Player p) {
    Channel channel = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel;
    channel
        .eventLoop()
        .execute(() -> {
          if (!running || channel.pipeline().get(HANDLER) != null) return;
          channel
              .pipeline()
              .addBefore(
                  "packet_handler",
                  HANDLER,
                  new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg)
                        throws Exception {
                      if (msg instanceof PacketPlayInUseEntity packet) {
                        UUID id = clickIds.get(USE_ID.getInt(packet));
                        if (id != null) {
                          if (packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT
                              && running) Bukkit.getScheduler().runTask(plugin, () -> click(p, id));
                          return;
                        }
                      }
                      super.channelRead(ctx, msg);
                    }
                  });
        });
  }

  private void uninject(Player p) {
    Channel c = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel;
    c.eventLoop()
        .execute(() -> {
          if (c.pipeline().get(HANDLER) != null) c.pipeline().remove(HANDLER);
        });
  }

  @EventHandler
  public void join(PlayerJoinEvent e) {
    inject(e.getPlayer());
    later(e.getPlayer());
  }

  @EventHandler
  public void quit(PlayerQuitEvent e) {
    visible.remove(e.getPlayer().getUniqueId());
    clicked.remove(e.getPlayer().getUniqueId());
    uninject(e.getPlayer());
  }

  @EventHandler
  public void move(PlayerMoveEvent e) {
    if (e.getTo() != null
        && (e.getFrom().getBlockX() != e.getTo().getBlockX()
            || e.getFrom().getBlockZ() != e.getTo().getBlockZ()
            || e.getFrom().getBlockY() != e.getTo().getBlockY())) sync(e.getPlayer());
  }

  @EventHandler
  public void world(PlayerChangedWorldEvent e) {
    resetViewer(e.getPlayer());
    later(e.getPlayer());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void teleport(PlayerTeleportEvent e) {
    resetViewer(e.getPlayer());
    later(e.getPlayer());
  }

  @EventHandler
  public void respawn(PlayerRespawnEvent e) {
    resetViewer(e.getPlayer());
    later(e.getPlayer());
  }

  @EventHandler
  public void arena(PlayerJoinArenaEvent e) {
    later(e.getPlayer());
  }

  @EventHandler
  public void leave(PlayerLeaveArenaEvent e) {
    later(e.getPlayer());
  }

  private void later(Player p) {
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (running && p.isOnline()) sync(p);
            },
            2L);
  }

  @EventHandler
  public void load(WorldLoadEvent e) {
    definitions.values().stream()
        .filter(d -> d.world().equals(e.getWorld().getName()))
        .forEach(this::build);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void unload(WorldUnloadEvent e) {
    new ArrayList<>(definitions.values())
        .stream()
            .filter(d -> d.world().equals(e.getWorld().getName()))
            .forEach(d -> discard(d.id()));
  }

  public void close() {
    running = false;
    for (UUID id : new ArrayList<>(models.keySet())) discard(id);
    for (Player p : online.get()) uninject(p);
    HandlerList.unregisterAll(this);
    visible.clear();
    clicked.clear();
    clickIds.clear();
    instance = null;
  }

  private void fetchSkin(String skin) {
    String key = skin.toLowerCase(Locale.ROOT);
    if (!fetching.add(key)) return;
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                JsonObject profile =
                    json("https://api.mojang.com/users/profiles/minecraft/" + skin);
                String uuid = profile.get("id").getAsString();
                if (!uuid.matches("[0-9a-fA-F]{32}")) throw new IOException("皮肤 UUID 无效");
                JsonObject session =
                    json(
                        "https://sessionserver.mojang.com/session/minecraft/profile/"
                            + uuid
                            + "?unsigned=false");
                JsonObject texture = null;
                for (com.google.gson.JsonElement e : session.getAsJsonArray("properties"))
                  if ("textures".equals(e.getAsJsonObject().get("name").getAsString()))
                    texture = e.getAsJsonObject();
                if (texture == null) throw new IOException("未找到皮肤纹理");
                String value = texture.get("value").getAsString(),
                    signature = texture.get("signature").getAsString();
                if (!running) return;
                Bukkit.getScheduler()
                    .runTask(
                        plugin,
                        () -> {
                          if (!running) return;
                          fetching.remove(key);
                          config.set("skins." + key + ".value", value);
                          config.set("skins." + key + ".signature", signature);
                          try {
                            persist();
                          } catch (IOException e) {
                            plugin.getLogger().warning("NPC 皮肤缓存保存失败：" + e.getMessage());
                          }
                          for (JoinNpcDefinition d : new ArrayList<>(definitions.values()))
                            if (d.skin().equalsIgnoreCase(skin)) {
                              discard(d.id());
                              build(d);
                            }
                          online.get().forEach(this::sync);
                        });
              } catch (Exception e) {
                if (running)
                  plugin
                      .getLogger()
                      .warning("无法获取 NPC 皮肤 " + skin + "，暂用默认外观，重启后重试：" + e.getMessage());
              }
            });
  }

  private static JsonObject json(String url) throws IOException {
    HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
    c.setConnectTimeout(5000);
    c.setReadTimeout(5000);
    c.setRequestProperty("User-Agent", "SimpMC-BW1.8.8");
    try {
      if (c.getResponseCode() != 200) throw new IOException("皮肤服务 HTTP " + c.getResponseCode());
      try (Reader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
        return new JsonParser().parse(r).getAsJsonObject();
      }
    } finally {
      c.disconnect();
    }
  }

  private record Model(EntityPlayer player, ScoreboardTeam team, List<EntityArmorStand> lines) {}
}
