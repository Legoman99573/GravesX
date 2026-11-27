package dev.cwhead.GravesXProxy;

import dev.cwhead.GravesXProxy.protocol.Protocol.GraveSummary;
import dev.cwhead.GravesXProxy.command.ProxyAdminCommand;
import dev.cwhead.GravesXProxy.config.DatabaseConfig;
import dev.cwhead.GravesXProxy.config.ProxyConfig;
import dev.cwhead.GravesXProxy.library.ProxyLibraries;
import dev.cwhead.GravesXProxy.manager.ServerRegistry;
import dev.cwhead.GravesXProxy.platform.ProxyPlatform;
import dev.cwhead.GravesXProxy.storage.GraveStore;
import dev.cwhead.GravesXProxy.storage.MemoryGraveStore;
import dev.cwhead.GravesXProxy.storage.SqlDialect;
import dev.cwhead.GravesXProxy.storage.SqlGraveStore;
import dev.cwhead.GravesXProxy.protocol.Protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Platform-independent proxy logic: server registry, grave tracking, cross-server teleports. */
public final class ProxyCore {
    private record Pending(String server, UUID grave, long expiresAt) {}

    private interface StoreTask {
        void run() throws Exception;
    }

    private static final int DETAIL_LIMIT = 100;
    private static final int TELEPORT_LIMIT = 512;

    private final ProxyPlatform platform;
    private final Path dataDir;
    private final ProxyLibraries libraries;
    private final ServerRegistry registry = new ServerRegistry();
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean saveQueued = new AtomicBoolean();
    private final ProxyAdminCommand adminCommand;
    /** All store access (possibly slow database calls) happens on this single thread. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GravesX-Store");
        t.setDaemon(true);
        return t;
    });
    private volatile ProxyConfig config;
    private volatile GraveStore store;
    private volatile DatabaseConfig storeConfig;

    public ProxyCore(ProxyPlatform platform, Path dataDir, ProxyLibraries libraries) {
        this.platform = platform;
        this.dataDir = dataDir;
        this.libraries = libraries;
        libraries.loadYaml();
        this.adminCommand = new ProxyAdminCommand(this, platform);
        registry.load(dataDir.resolve("servers.txt"));
        reload();
    }

    public void reload() {
        try {
            config = ProxyConfig.load(dataDir);
        } catch (IOException e) {
            platform.warn("Could not load config.yml, using defaults: " + e.getMessage());
            if (config == null) {
                config = new ProxyConfig();
            }
        }
        openStore();
    }

    /** (Re)opens the grave store when the database settings changed. */
    private synchronized void openStore() {
        DatabaseConfig wanted = config.database();
        if (store != null && wanted.equals(storeConfig)) {
            return;
        }
        if (store != null) {
            store.save();
            store.close();
        }

        storeConfig = wanted;
        if (wanted.enabled()) {
            try {
                SqlDialect dialect = SqlDialect.parse(wanted.type());
                if (dialect != null) {
                    libraries.loadDriver(dialect);
                }
                store = new SqlGraveStore(wanted);
                platform.info("Using global grave database: " + store.describe());
                return;
            } catch (Exception e) {
                platform.warn("Could not use the global database (" + e.getMessage()
                        + "). Falling back to the per-proxy file store until /gravesproxy reload.");
            }
        }
        store = new MemoryGraveStore(dataDir.resolve("graves.txt"));
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        registry.save(dataDir.resolve("servers.txt"));
        if (store != null) {
            store.save();
            store.close();
        }
    }

    /** Whether proxy messages should be parsed as MiniMessage (config: minimessage). */
    public boolean isMiniMessage() {
        return config.miniMessage();
    }

    // ------------------------------------------------------------------ visibility

    public boolean isExcluded(String server) {
        return config.isExcluded(server);
    }

    private boolean isVisible(String server) {
        if (isExcluded(server)) return false;
        return !config.ignoreWithoutGravesX() || registry.isInstalled(server);
    }

    private String resolveServer(String name) {
        for (String s : platform.serverNames()) {
            if (s.equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    private void async(String what, StoreTask task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                platform.warn("GravesX store error while " + what + ": " + e);
            }
        });
    }

    // ------------------------------------------------------------------ platform events

    /** Called after a player has finished connecting to a backend server. */
    public void onPlayerConnected(UUID player, String server) {
        if (!isExcluded(server)) {
            // Ask the backend to introduce itself; servers without GravesX simply never answer.
            platform.runLater(() -> platform.sendToBackend(player, simple(Protocol.QUERY, out -> out.writeInt(Protocol.VERSION))),
                    config.postConnectDelayMillis());
        }

        Pending p = pending.get(player);
        if (p != null && p.server().equalsIgnoreCase(server)) {
            pending.remove(player);
            if (System.currentTimeMillis() <= p.expiresAt()) {
                platform.runLater(() -> platform.sendToBackend(player, executePacket(player, p.grave())),
                        config.postConnectDelayMillis());
            }
        }
    }

    public void onPlayerQuit(UUID player) {
        pending.remove(player);
    }

    /**
     * Handles a plugin message that came from a backend server.
     *
     * @param server  name of the backend that sent it (as registered on the proxy)
     * @param carrier the player whose connection carried the message
     */
    public void handleBackend(String server, UUID carrier, byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            byte op = in.readByte();
            switch (op) {
                case Protocol.HELLO -> {
                    int version = in.readInt();
                    String gravesVersion = in.readUTF();
                    if (isExcluded(server)) return;
                    if (registry.markInstalled(server)) {
                        platform.info("Detected GravesX " + gravesVersion + " on server '" + server + "'.");
                        queueSave();
                    }
                    if (version != Protocol.VERSION) {
                        platform.warn("Server '" + server + "' speaks GravesX proxy protocol v" + version
                                + " but this proxy speaks v" + Protocol.VERSION + ". Use the same GravesX jar everywhere.");
                    }
                }
                case Protocol.GRAVES_UPDATE -> handleGravesUpdate(server, in);
                case Protocol.TELEPORT_REQUEST -> handleTeleportRequest(carrier, in.readUTF(), in.readInt());
                case Protocol.NETWORK_REQUEST -> handleNetworkRequest(carrier, in.readUTF());
                default -> { }
            }
        } catch (IOException | RuntimeException e) {
            platform.warn("Ignoring malformed GravesX message from '" + server + "': " + e);
        }
    }

    private void handleGravesUpdate(String server, DataInputStream in) throws IOException {
        UUID owner = Protocol.readUuid(in);
        in.readInt(); // total count on the backend; the store holds the (capped) list itself
        int n = in.readInt();
        List<GraveSummary> list = new ArrayList<>(Math.min(n, TELEPORT_LIMIT));
        for (int i = 0; i < n; i++) {
            GraveSummary g = GraveSummary.read(in);
            if (i < TELEPORT_LIMIT) list.add(g);
        }
        if (isExcluded(server)) return;
        if (registry.markInstalled(server)) {
            queueSave();
        }
        async("saving graves of " + owner + " on " + server, () -> store.replaceOwner(server, owner, list));
        queueSave();
    }

    // ------------------------------------------------------------------ requests

    private void handleNetworkRequest(UUID player, String detailServer) {
        String current = platform.currentServer(player);
        String detail = detailServer.isEmpty() ? null : resolveServer(detailServer);

        List<String> shown = new ArrayList<>();
        for (String s : platform.serverNames()) {
            if (isVisible(s)) shown.add(s);
        }
        shown.sort(String.CASE_INSENSITIVE_ORDER);

        async("reading graves of " + player, () -> {
            Map<String, Integer> counts = store.counts(player);
            List<GraveSummary> detailList = (detail != null && isVisible(detail))
                    ? store.graves(player, detail, DETAIL_LIMIT) : List.of();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(Protocol.NETWORK_RESPONSE);
            Protocol.writeUuid(out, player);
            out.writeUTF(detail == null ? "" : detail);
            out.writeInt(shown.size());
            for (String s : shown) {
                out.writeUTF(s);
                out.writeBoolean(s.equalsIgnoreCase(current));
                out.writeBoolean(registry.isInstalled(s));
                out.writeInt(counts.getOrDefault(s.toLowerCase(Locale.ROOT), 0));
            }
            out.writeInt(detailList.size());
            for (GraveSummary g : detailList) {
                g.write(out);
            }
            platform.sendToBackend(player, bytes.toByteArray());
        });
    }

    private void handleTeleportRequest(UUID player, String requested, int index) {
        platform.sendToBackend(player, simple(Protocol.ACK, out -> { }));

        String target = resolveServer(requested);
        if (target == null) {
            platform.message(player, config.message("unknown-server", "server", requested));
            return;
        }
        if (isExcluded(target)) {
            platform.message(player, config.message("excluded-server", "server", target));
            return;
        }
        if (!registry.isInstalled(target)) {
            platform.message(player, config.message("not-installed", "server", target));
            return;
        }

        async("looking up graves of " + player, () -> {
            List<GraveSummary> graves = store.graves(player, target, TELEPORT_LIMIT);
            if (graves.isEmpty()) {
                platform.message(player, config.message("no-graves", "server", target));
                return;
            }

            UUID grave = Protocol.LATEST;
            if (index > 0) {
                if (index > graves.size()) {
                    platform.message(player, config.message("bad-index", "server", target,
                            "count", String.valueOf(graves.size())));
                    return;
                }
                grave = graves.get(index - 1).id();
            }

            String current = platform.currentServer(player);
            if (current != null && current.equalsIgnoreCase(target)) {
                platform.sendToBackend(player, executePacket(player, grave));
                return;
            }

            pending.put(player, new Pending(target, grave, System.currentTimeMillis() + config.teleportTimeoutMillis()));
            platform.message(player, config.message("teleporting", "server", target));
            platform.connect(player, target, ok -> {
                if (!ok) {
                    pending.remove(player);
                    platform.message(player, config.message("connect-failed", "server", target));
                }
            });
        });
    }

    // ------------------------------------------------------------------ admin command

    /** Runs /gravesproxy. Returns the lines to show the sender. */
    public List<String> command(boolean admin, String[] args) {
        return adminCommand.run(admin, args);
    }

    public List<String> tabComplete(String[] args) {
        return adminCommand.tabComplete(args);
    }

    // package-private access for ProxyAdminCommand
    public ProxyConfig config() { return config; }

    public ServerRegistry registry() { return registry; }

    public String storeDescription() { return store == null ? "none" : store.describe(); }

    public void forgetServer(String server) {
        registry.forget(server);
        queueSave();
        async("forgetting server " + server, () -> store.forgetServer(server));
    }

    // ------------------------------------------------------------------ helpers

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] simple(byte op, Writer writer) {
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

    private static byte[] executePacket(UUID player, UUID grave) {
        return simple(Protocol.TELEPORT_EXECUTE, out -> {
            Protocol.writeUuid(out, player);
            Protocol.writeUuid(out, grave);
        });
    }

    void queueSave() {
        if (saveQueued.compareAndSet(false, true)) {
            platform.runLater(() -> {
                saveQueued.set(false);
                registry.save(dataDir.resolve("servers.txt"));
                async("saving", () -> store.save());
            }, 5_000L);
        }
    }
}
