package dev.cwhead.GravesX.proxy;

import com.ranull.graves.Graves;
import com.ranull.graves.data.LocationData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityTeleport;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bridge between GravesX and the GravesX Velocity/BungeeCord proxy plugin.
 *
 * <p>The backend reports each player's grave count and grave summaries to the proxy over the
 * {@code gravesx:proxy} plugin channel, and carries out teleports the proxy asks for after it has moved
 * the player to this server. The wire format must stay in sync with the proxy plugin's Protocol class.</p>
 */
public class ProxyBridge implements PluginMessageListener {
    public static final String CHANNEL = "gravesx:proxy";
    public static final int PROTOCOL_VERSION = 1;
    private static final UUID LATEST = new UUID(0L, 0L);

    // backend -> proxy
    private static final byte HELLO = 0x01;
    private static final byte GRAVES_UPDATE = 0x02;
    private static final byte TELEPORT_REQUEST = 0x03;
    private static final byte NETWORK_REQUEST = 0x04;
    // proxy -> backend
    private static final byte QUERY = 0x11;
    private static final byte TELEPORT_EXECUTE = 0x12;
    private static final byte NETWORK_RESPONSE = 0x13;
    private static final byte ACK = 0x14;

    private static final String PREFIX = ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET;

    private final Graves plugin;
    private final Map<UUID, Integer> lastFingerprint = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> awaiting = new ConcurrentHashMap<>();
    private final AtomicLong tokens = new AtomicLong();
    private volatile boolean enabled;

    public ProxyBridge(Graves plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ lifecycle

    public void enable() {
        if (enabled) return;
        enabled = true;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);

        long period = Math.max(10L, plugin.getConfig().getLong("settings.integration.proxy.sync-interval-seconds", 60L)) * 20L;
        plugin.getSchedulerManager().runTaskTimer(this::periodicSync, 100L, period);
        plugin.infoMessage("Proxy bridge enabled (channel " + CHANNEL + ").");
    }

    public void disable() {
        if (!enabled) return;
        enabled = false;
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        lastFingerprint.clear();
        dirty.clear();
        pending.clear();
        awaiting.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ------------------------------------------------------------------ hooks called by GravesX

    /** A player joined this server: introduce ourselves and push their graves. */
    public void onPlayerJoin(Player player) {
        if (!enabled) return;
        plugin.getSchedulerManager().runTaskLater(player, () -> {
            if (!player.isOnline()) return;
            sendHello(player);
            lastFingerprint.remove(player.getUniqueId());
            sendGraves(player.getUniqueId(), player);
            flushDirty(player);
        }, 20L);
    }

    /**
     * A grave owned by this player was created or removed (called by DataManager for every add/remove).
     * Bursts, for example a purge of many graves, are coalesced into one update per owner.
     */
    public void onGraveChanged(UUID owner) {
        if (!enabled || owner == null) return;
        if (!pending.add(owner)) return; // an update for this owner is already scheduled
        plugin.getSchedulerManager().runTaskLater(() -> {
            pending.remove(owner);
            if (!sync(owner)) {
                dirty.add(owner);
            }
        }, 20L);
    }

    private void periodicSync() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendGraves(player.getUniqueId(), player);
        }
        Player carrier = anyPlayer();
        if (carrier != null) flushDirty(carrier);
    }

    private void flushDirty(Player carrier) {
        for (UUID owner : new ArrayList<>(dirty)) {
            if (dirty.remove(owner)) {
                sendGraves(owner, carrier);
            }
        }
    }

    private Player anyPlayer() {
        for (Player p : Bukkit.getOnlinePlayers()) return p;
        return null;
    }

    /** @return false if no player was online to carry the message. */
    private boolean sync(UUID owner) {
        Player carrier = Bukkit.getPlayer(owner);
        if (carrier == null) carrier = anyPlayer();
        if (carrier == null) return false;
        lastFingerprint.remove(owner);
        sendGraves(owner, carrier);
        return true;
    }

    // ------------------------------------------------------------------ outgoing

    private void sendHello(Player carrier) {
        send(carrier, build(HELLO, out -> {
            out.writeInt(PROTOCOL_VERSION);
            out.writeUTF(plugin.getDescription().getVersion());
        }));
    }

    private void sendGraves(UUID owner, Player carrier) {
        boolean onlyLoaded = plugin.getConfig().getBoolean("settings.integration.proxy.only-loaded-worlds", false);
        int max = Math.max(1, plugin.getConfig().getInt("settings.integration.proxy.max-synced-graves", 200));

        List<Grave> graves = new ArrayList<>();
        for (Grave grave : plugin.getGraveManager().getGraveList(owner)) {
            LocationData ld = grave.getLocationDeathData();
            if (ld == null || ld.getWorldName() == null) continue;
            if (onlyLoaded && Bukkit.getWorld(ld.getWorldName()) == null) continue;
            graves.add(grave);
        }
        graves.sort(Comparator.comparingLong(Grave::getTimeCreation).reversed());

        int fingerprint = graves.size();
        for (Grave g : graves) fingerprint = 31 * fingerprint + g.getUUID().hashCode();
        Integer previous = lastFingerprint.put(owner, fingerprint);
        if (previous != null && previous == fingerprint) return;

        List<Grave> shown = graves.size() > max ? graves.subList(0, max) : graves;
        send(carrier, build(GRAVES_UPDATE, out -> {
            writeUuid(out, owner);
            out.writeInt(graves.size());
            out.writeInt(shown.size());
            for (Grave g : shown) {
                LocationData ld = g.getLocationDeathData();
                writeUuid(out, g.getUUID());
                out.writeUTF(ld.getWorldName());
                out.writeDouble(ld.getX());
                out.writeDouble(ld.getY());
                out.writeDouble(ld.getZ());
                out.writeLong(g.getTimeCreation());
            }
        }));
    }

    private void send(Player carrier, byte[] data) {
        if (carrier == null || !enabled) return;
        plugin.getSchedulerManager().execute(carrier, () -> {
            if (carrier.isOnline()) {
                carrier.sendPluginMessage(plugin, CHANNEL, data);
            }
        });
    }

    // ------------------------------------------------------------------ incoming

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!enabled || !CHANNEL.equals(channel)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            byte op = in.readByte();
            switch (op) {
                case QUERY -> {
                    sendHello(player);
                    lastFingerprint.remove(player.getUniqueId());
                    sendGraves(player.getUniqueId(), player);
                }
                case ACK -> awaiting.remove(player.getUniqueId());
                case TELEPORT_EXECUTE -> {
                    UUID target = readUuid(in);
                    UUID grave = readUuid(in);
                    if (target.equals(player.getUniqueId())) {
                        executeTeleport(player, grave);
                    }
                }
                case NETWORK_RESPONSE -> {
                    awaiting.remove(player.getUniqueId());
                    showNetwork(player, in);
                }
                default -> plugin.debugMessage("ProxyBridge: unknown opcode " + op, 2);
            }
        } catch (IOException | RuntimeException e) {
            plugin.debugMessage("ProxyBridge: malformed message from proxy: " + e, 2);
        }
    }

    private void executeTeleport(Player player, UUID graveId) {
        // Give the player a moment to finish joining this server.
        plugin.getSchedulerManager().runTaskLater(player, () -> {
            if (!player.isOnline()) return;

            if (!plugin.getPermissionManager().hasGrantedPermission("graves.network.teleport", player)) {
                plugin.getEntityManager().sendMessage("message.permission-denied", player);
                return;
            }

            Grave grave = null;
            List<Grave> graves = plugin.getGraveManager().getGraveList(player);
            if (!LATEST.equals(graveId)) {
                for (Grave g : graves) {
                    if (g.getUUID().equals(graveId)) {
                        grave = g;
                        break;
                    }
                }
            } else if (!graves.isEmpty()) {
                grave = graves.stream().max(Comparator.comparingLong(Grave::getTimeCreation)).orElse(null);
            }

            if (grave == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "That grave no longer exists on this server.");
                return;
            }

            Location deathLoc = grave.getLocationDeath();
            if (deathLoc == null || deathLoc.getWorld() == null) {
                player.sendMessage(PREFIX + "Grave location is unavailable.");
                return;
            }

            if (plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.free", player)) {
                CompatibilityTeleport.teleportSafely(player, deathLoc, plugin).thenAccept(ok -> {
                    if (!ok) {
                        player.sendMessage(PREFIX + "Teleport failed.");
                    }
                });
            } else {
                List<Location> options = plugin.getGraveManager().getGraveLocationList(player.getLocation(), grave);
                if (options.isEmpty()) {
                    player.sendMessage(PREFIX + "Teleport failed.");
                    return;
                }
                plugin.getEntityManager().teleportEntity(player, options.get(0), grave);
            }
        }, 20L);
    }

    // ------------------------------------------------------------------ commands

    /** Handles {@code /graves network ...}. */
    public void handleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use the network command.");
            return;
        }
        if (!plugin.getPermissionManager().hasGrantedPermission("graves.network", player)) {
            plugin.getEntityManager().sendMessage("message.permission-denied", player);
            return;
        }
        if (!enabled) {
            player.sendMessage(PREFIX + ChatColor.RED + "Proxy support is disabled in the GravesX config.");
            return;
        }

        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "" -> request(player, build(NETWORK_REQUEST, out -> {
                writeUuid(out, player.getUniqueId());
                out.writeUTF("");
            }));
            case "list" -> {
                if (args.length < 3) {
                    player.sendMessage(PREFIX + ChatColor.RED + "/graves network list {server}");
                    return;
                }
                request(player, build(NETWORK_REQUEST, out -> {
                    writeUuid(out, player.getUniqueId());
                    out.writeUTF(args[2]);
                }));
            }
            case "tp", "teleport" -> {
                if (!plugin.getPermissionManager().hasGrantedPermission("graves.network.teleport", player)) {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player);
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(PREFIX + ChatColor.RED + "/graves network tp {server} [number]");
                    return;
                }
                int index = 0;
                if (args.length > 3) {
                    try {
                        index = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Grave number must be a number.");
                        return;
                    }
                    if (index < 1) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Grave number must be 1 or higher.");
                        return;
                    }
                }
                final int finalIndex = index;
                request(player, build(TELEPORT_REQUEST, out -> {
                    out.writeUTF(args[2]);
                    out.writeInt(finalIndex);
                }));
            }
            default -> {
                player.sendMessage(PREFIX + ChatColor.RED + "/graves network" + ChatColor.DARK_GRAY + " - " + ChatColor.RESET + "Grave counts per server");
                player.sendMessage(PREFIX + ChatColor.RED + "/graves network list {server}" + ChatColor.DARK_GRAY + " - " + ChatColor.RESET + "Graves on a server");
                player.sendMessage(PREFIX + ChatColor.RED + "/graves network tp {server} [number]" + ChatColor.DARK_GRAY + " - " + ChatColor.RESET + "Teleport to a grave on another server");
            }
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 2) {
            result.add("list");
            result.add("tp");
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        result.removeIf(s -> !s.startsWith(prefix));
        return result;
    }

    private void request(Player player, byte[] data) {
        long token = tokens.incrementAndGet();
        awaiting.put(player.getUniqueId(), token);
        send(player, data);

        plugin.getSchedulerManager().runTaskLater(player, () -> {
            if (awaiting.remove(player.getUniqueId(), token)) {
                player.sendMessage(PREFIX + ChatColor.RED + "No GravesX proxy responded. Is the GravesX Velocity/BungeeCord plugin installed on your proxy?");
            }
        }, 60L);
    }

    private void showNetwork(Player player, DataInputStream in) throws IOException {
        readUuid(in); // player id, already known
        String detail = in.readUTF();
        int servers = in.readInt();

        boolean canTeleport = plugin.getPermissionManager().hasGrantedPermission("graves.network.teleport", player);

        player.sendMessage(PREFIX + ChatColor.RED + "Graves across the network");
        if (servers == 0) {
            player.sendMessage(ChatColor.GRAY + "No GravesX servers are available.");
        }
        for (int i = 0; i < servers; i++) {
            String name = in.readUTF();
            boolean here = in.readBoolean();
            boolean installed = in.readBoolean();
            int count = in.readInt();

            TextComponent line = new TextComponent(ChatColor.WHITE + name + ChatColor.DARK_GRAY + " - ");
            if (!installed) {
                line.addExtra(ChatColor.GRAY + "GravesX not detected");
            } else {
                line.addExtra((count > 0 ? ChatColor.RED : ChatColor.GRAY) + String.valueOf(count) + ChatColor.GRAY
                        + (count == 1 ? " grave" : " graves"));
                if (here) {
                    line.addExtra(ChatColor.DARK_GRAY + " (you are here)");
                }
                if (count > 0) {
                    line.addExtra(button(" [List]", ChatColor.YELLOW, "/graves network list " + name));
                    if (canTeleport) {
                        line.addExtra(button(" [Teleport]", ChatColor.GREEN, "/graves network tp " + name));
                    }
                }
            }
            player.spigot().sendMessage(line);
        }

        int graves = in.readInt();
        if (!detail.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Graves on " + ChatColor.WHITE + detail + ChatColor.RED + ":");
            if (graves == 0) {
                player.sendMessage(ChatColor.GRAY + "None.");
            }
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < graves; i++) {
            readUuid(in);
            String world = in.readUTF();
            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            long created = in.readLong();

            TextComponent line = new TextComponent(ChatColor.DARK_GRAY + "#" + (i + 1) + " " + ChatColor.WHITE + world
                    + ChatColor.GRAY + " " + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z)
                    + ChatColor.DARK_GRAY + " (" + age(now - created) + ")");
            if (canTeleport && !detail.isEmpty()) {
                line.addExtra(button(" [Teleport]", ChatColor.GREEN, "/graves network tp " + detail + " " + (i + 1)));
            }
            player.spigot().sendMessage(line);
        }
    }

    private static TextComponent button(String text, ChatColor color, String command) {
        TextComponent component = new TextComponent(color + text);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        return component;
    }

    private static String age(long millis) {
        long minutes = Math.max(0L, millis) / 60_000L;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 48) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    // ------------------------------------------------------------------ codec

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] build(byte op, Writer writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(op);
            writer.write(out);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeUuid(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }
}
