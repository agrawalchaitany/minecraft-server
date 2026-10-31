package local.network;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * One plugin for every server behind the proxy; NETWORK_ROLE picks what it does:
 *   lobby  - game menu (compass + /menu) showing which game servers are on, and a protected lobby world
 *   game   - /lobby and /hub, and /lobbyall to move everyone out before the server is switched off
 *   limbo  - the login waiting room: players are frozen, hidden from each other and cannot chat
 */
public final class NetworkPlugin extends JavaPlugin implements Listener {
    private enum Role { LOBBY, GAME, LIMBO }

    private record Game(String id, String name, Material icon, int slot, List<String> description, String host, int port) {}

    private record Status(boolean online, int players) {}

    private static final String CHANNEL = "BungeeCord";
    private static final Pattern PLAYERS = Pattern.compile("\"players\"\\s*:\\s*\\{[^}]*?\"online\"\\s*:\\s*(\\d+)");

    private Role role;
    private NamespacedKey menuKey;
    private final Map<String, Game> games = new LinkedHashMap<>();
    private final Map<String, Status> status = new ConcurrentHashMap<>();
    private ScheduledExecutorService pinger;
    private int menuSize = 27;

    /** Marks our menu inventory so clicks in it are recognised. */
    private static final class Menu implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    @Override
    public void onEnable() {
        String r = System.getenv().getOrDefault("NETWORK_ROLE", "game").trim().toUpperCase(java.util.Locale.ROOT);
        try {
            role = Role.valueOf(r);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Unknown NETWORK_ROLE '" + r + "' (use lobby, game or limbo); acting as a game server");
            role = Role.GAME;
        }
        menuKey = new NamespacedKey(this, "menu");
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);

        if (role == Role.LOBBY) {
            loadGames();
            pinger = Executors.newSingleThreadScheduledExecutor(t -> {
                Thread th = new Thread(t, "network-status");
                th.setDaemon(true);
                return th;
            });
            pinger.scheduleWithFixedDelay(this::pingAll, 0, 3, TimeUnit.SECONDS);
        }
        getLogger().info("Running as the " + role.name().toLowerCase(java.util.Locale.ROOT) + " server"
            + (role == Role.LOBBY ? " with " + games.size() + " games in the menu" : ""));
    }

    @Override
    public void onDisable() {
        if (pinger != null) pinger.shutdownNow();
    }

    // ---- games.yml ----

    private void loadGames() {
        File file = new File(System.getenv().getOrDefault("NETWORK_GAMES_FILE", "/config/network/games.yml"));
        if (!file.isFile()) {
            getLogger().warning(file + " not found; the game menu will be empty");
            return;
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("games");
        if (section == null) {
            getLogger().warning(file + " has no 'games:' section; the game menu will be empty");
            return;
        }
        Set<Integer> used = new HashSet<>();
        int[] defaultSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int next = 0, maxSlot = 0;
        for (String id : section.getKeys(false)) {
            ConfigurationSection g = section.getConfigurationSection(id);
            if (g == null) continue;
            Material icon = Material.matchMaterial(g.getString("icon", "GRASS_BLOCK"));
            if (icon == null || !icon.isItem()) {
                getLogger().warning("games.yml: " + id + " has an unknown icon '" + g.getString("icon") + "'; using a paper icon");
                icon = Material.PAPER;
            }
            int slot = g.getInt("slot", -1);
            if (slot < 0 || slot >= 54 || used.contains(slot)) {
                while (next < defaultSlots.length && used.contains(defaultSlots[next])) next++;
                if (next >= defaultSlots.length) {
                    getLogger().warning("games.yml: no room left in the menu for " + id);
                    continue;
                }
                slot = defaultSlots[next];
            }
            used.add(slot);
            maxSlot = Math.max(maxSlot, slot);
            String address = g.getString("address", id + ":25565");
            int colon = address.lastIndexOf(':');
            String host = colon > 0 ? address.substring(0, colon) : address;
            int port = colon > 0 ? Integer.parseInt(address.substring(colon + 1)) : 25565;
            games.put(id, new Game(id, g.getString("name", id), icon, slot, g.getStringList("description"), host, port));
        }
        menuSize = Math.max(27, Math.min(54, (maxSlot / 9 + 2) * 9));
    }

    // ---- server status (server-list ping straight to each game server) ----

    private void pingAll() {
        for (Game g : games.values()) status.put(g.id(), ping(g.host(), g.port()));
        if (isEnabled()) {
            try {
                getServer().getScheduler().runTask(this, this::refreshOpenMenus);
            } catch (Exception ignored) {
                // The server is shutting down.
            }
        }
    }

    private static Status ping(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1500);
            s.setSoTimeout(1500);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hs);
            writeVarInt(h, 0x00);
            writeVarInt(h, -1);
            byte[] hb = host.getBytes(StandardCharsets.UTF_8);
            writeVarInt(h, hb.length);
            h.write(hb);
            h.writeShort(port);
            writeVarInt(h, 1);
            writeVarInt(out, hs.size());
            out.write(hs.toByteArray());
            writeVarInt(out, 1);
            writeVarInt(out, 0x00);
            out.flush();
            readVarInt(in);
            if (readVarInt(in) != 0x00) return new Status(false, 0);
            int len = readVarInt(in);
            if (len < 0 || len > 1 << 20) return new Status(false, 0);
            byte[] json = new byte[len];
            in.readFully(json);
            Matcher m = PLAYERS.matcher(new String(json, StandardCharsets.UTF_8));
            return new Status(true, m.find() ? Integer.parseInt(m.group(1)) : 0);
        } catch (IOException | RuntimeException e) {
            return new Status(false, 0);
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0, shift = 0;
        byte b;
        do {
            b = in.readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new IOException("VarInt too big");
        } while ((b & 0x80) != 0);
        return value;
    }

    // ---- menu ----

    private void openMenu(Player p) {
        Menu holder = new Menu();
        Inventory inv = getServer().createInventory(holder, menuSize, Component.text("Choose a game"));
        holder.inventory = inv;
        render(inv);
        p.openInventory(inv);
    }

    private void render(Inventory inv) {
        inv.clear();
        for (Game g : games.values()) {
            Status s = status.getOrDefault(g.id(), new Status(false, 0));
            ItemStack item = new ItemStack(g.icon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(plain(g.name(), s.online() ? NamedTextColor.GREEN : NamedTextColor.RED).decorate(TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            for (String line : g.description()) lore.add(plain(line, NamedTextColor.GRAY));
            lore.add(Component.empty());
            if (s.online()) {
                lore.add(plain("● Online - " + s.players() + (s.players() == 1 ? " player" : " players"), NamedTextColor.GREEN));
                lore.add(plain("Click to play", NamedTextColor.YELLOW));
            } else {
                lore.add(plain("● Switched off", NamedTextColor.RED));
                lore.add(plain("Ask the server owner to switch it on", NamedTextColor.DARK_GRAY));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(g.slot(), item);
        }
    }

    private void refreshOpenMenus() {
        for (Player p : getServer().getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof Menu) render(top);
        }
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack menuItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain("Game Menu", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        meta.lore(List.of(plain("Right-click to choose a game", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }

    // ---- moving players between servers (via the proxy's BungeeCord channel) ----

    private void send(Player p, String server) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            p.sendPluginMessage(this, CHANNEL, b.toByteArray());
        } catch (IOException e) {
            p.sendMessage(Component.text("Could not reach the proxy: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void join(Player p, Game g) {
        Status s = status.get(g.id());
        if (s == null || !s.online()) {
            p.sendMessage(Component.text(g.name() + " is switched off right now. Ask the server owner to switch it on.", NamedTextColor.RED));
            return;
        }
        p.closeInventory();
        p.sendMessage(Component.text("Sending you to " + g.name() + "...", NamedTextColor.GREEN));
        send(p, g.id());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(java.util.Locale.ROOT)) {
            case "lobby" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use /lobby; use /lobbyall to move everyone");
                } else if (role == Role.LOBBY) {
                    p.teleport(spawn(p));
                } else {
                    send(p, "lobby");
                }
                return true;
            }
            case "menu" -> {
                if (!(sender instanceof Player p)) return true;
                if (role == Role.LOBBY) openMenu(p);
                else send(p, "lobby");
                return true;
            }
            case "lobbyall" -> {
                int n = 0;
                for (Player p : getServer().getOnlinePlayers()) {
                    p.sendMessage(Component.text("This server is switching off; taking you back to the lobby.", NamedTextColor.YELLOW));
                    send(p, "lobby");
                    n++;
                }
                sender.sendMessage("Sent " + n + " player(s) to the lobby");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ---- lobby and limbo behaviour ----

    private boolean protectedWorld() { return role != Role.GAME; }

    private boolean canBuild(Player p) { return role == Role.LOBBY && p.hasPermission("network.build"); }

    private Location spawn(Player p) {
        return p.getWorld().getSpawnLocation().add(0.5, 0, 0.5);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (role == Role.LIMBO) {
            e.joinMessage(null);
            for (Player other : getServer().getOnlinePlayers()) {
                if (other.equals(p)) continue;
                other.hidePlayer(this, p);
                p.hidePlayer(this, other);
            }
        }
        if (!protectedWorld()) return;
        p.teleport(spawn(p));
        p.setFoodLevel(20);
        p.setFireTicks(0);
        if (!canBuild(p)) p.setGameMode(GameMode.ADVENTURE);
        if (role == Role.LOBBY) {
            p.getInventory().clear();
            p.getInventory().setItem(4, menuItem());
            p.getInventory().setHeldItemSlot(4);
        } else {
            p.getInventory().clear();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (role == Role.LIMBO) e.quitMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (role != Role.LOBBY) return;
        if (e.getHand() == EquipmentSlot.HAND && isMenuItem(e.getItem())
                && e.getAction() != Action.PHYSICAL) {
            e.setCancelled(true);
            openMenu(e.getPlayer());
        } else if (!canBuild(e.getPlayer()) && e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true); // trampling farmland, pressure plates
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory() || !(e.getWhoClicked() instanceof Player p)) return;
            for (Game g : games.values()) {
                if (g.slot() == e.getSlot()) {
                    join(p, g);
                    return;
                }
            }
            return;
        }
        if (role == Role.LOBBY && e.getWhoClicked() instanceof Player p && !canBuild(p)) {
            if (isMenuItem(e.getCurrentItem()) || isMenuItem(e.getCursor()) || e.getClick().isKeyboardClick()) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (protectedWorld() && !canBuild(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (protectedWorld() && !canBuild(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (protectedWorld() && (!canBuild(e.getPlayer()) || isMenuItem(e.getItemDrop().getItemStack()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (protectedWorld() && !canBuild(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!protectedWorld() || !(e.getEntity() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) p.teleport(spawn(p));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHunger(FoodLevelChangeEvent e) {
        if (protectedWorld()) e.setCancelled(true);
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent e) {
        if (protectedWorld() && e.toWeatherState()) e.setCancelled(true);
    }

    // Limbo: stand still until the proxy finishes the login, then the proxy moves the player on.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent e) {
        if (role != Role.LIMBO || e.getTo() == null) return;
        Location from = e.getFrom(), to = e.getTo();
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || to.getY() > from.getY()) {
            Location stay = from.clone();
            stay.setYaw(to.getYaw());
            stay.setPitch(to.getPitch());
            e.setTo(stay);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        if (role != Role.LIMBO) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(loginHint());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (role != Role.LIMBO || e.getPlayer().isOp()) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(loginHint());
    }

    private static Component loginHint() {
        return Component.text("Log in first: /login <password>, or if you are new: /register <password> <password>", NamedTextColor.YELLOW);
    }
}
