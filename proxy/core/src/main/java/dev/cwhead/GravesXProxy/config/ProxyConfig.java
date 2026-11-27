package dev.cwhead.GravesXProxy.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ProxyConfig {
    private static final Map<String, String> DEFAULT_MESSAGES = new LinkedHashMap<>();

    static {
        DEFAULT_MESSAGES.put("prefix", "&c☠ &8» &r");
        DEFAULT_MESSAGES.put("unknown-server", "&cUnknown server: &f%server%");
        DEFAULT_MESSAGES.put("excluded-server", "&cGraves are not available on &f%server%&c.");
        DEFAULT_MESSAGES.put("not-installed", "&cGravesX was not detected on &f%server%&c.");
        DEFAULT_MESSAGES.put("no-graves", "&cYou have no graves on &f%server%&c.");
        DEFAULT_MESSAGES.put("bad-index", "&cYou only have &f%count% &cgrave(s) on &f%server%&c.");
        DEFAULT_MESSAGES.put("teleporting", "&7Sending you to your grave on &f%server%&7...");
        DEFAULT_MESSAGES.put("connect-failed", "&cCould not connect you to &f%server%&c.");
    }

    private final Set<String> excluded = new HashSet<>();
    private boolean ignoreWithoutGravesX = true;
    private boolean miniMessage = true;
    private DatabaseConfig database = DatabaseConfig.disabled();
    private long teleportTimeoutMillis = 30_000L;
    private long postConnectDelayMillis = 1_000L;
    private final Map<String, String> messages = new HashMap<>(DEFAULT_MESSAGES);

    @SuppressWarnings("unchecked")
    public static ProxyConfig load(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        Path file = dataDir.resolve("config.yml");

        if (Files.notExists(file)) {
            try (InputStream in = ProxyConfig.class.getResourceAsStream("/proxy-config.yml")) {
                if (in != null) {
                    Files.copy(in, file);
                }
            }
        }

        ProxyConfig cfg = new ProxyConfig();
        if (Files.notExists(file)) {
            return cfg;
        }

        Object root;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = new Yaml().load(reader);
        }

        if (!(root instanceof Map<?, ?> map)) {
            return cfg;
        }

        if (map.get("minimessage") instanceof Boolean b)
            cfg.miniMessage = b;

        if (map.get("database") instanceof Map<?, ?> d) {
            DatabaseConfig def = DatabaseConfig.disabled();
            Map<String, String> options = new LinkedHashMap<>();
            if (d.get("options") instanceof Map<?, ?> o) {
                o.forEach((k, v) -> {
                    if (k != null && v != null) options.put(k.toString(), v.toString());
                });
            }

            cfg.database = new DatabaseConfig(
                    d.get("enabled") instanceof Boolean b && b,
                    str(d.get("type"), def.type()),
                    str(d.get("host"), def.host()),
                    d.get("port") instanceof Number n ? n.intValue() : def.port(),
                    str(d.get("name"), def.name()),
                    str(d.get("username"), def.username()),
                    str(d.get("password"), def.password()),
                    str(d.get("table-prefix"), def.tablePrefix()),
                    Map.copyOf(options));
        }

        Object servers = map.get("servers");
        if (servers instanceof Map<?, ?> s) {
            Object ex = s.get("exclude");

            if (ex instanceof Collection<?> c) {
                for (Object o : c) {
                    if (o != null) cfg.excluded.add(o.toString().toLowerCase(Locale.ROOT));
                }
            }

            Object ignore = s.get("ignore-servers-without-gravesx");
            if (ignore instanceof Boolean b) cfg.ignoreWithoutGravesX = b;
        }

        Object teleport = map.get("teleport");
        if (teleport instanceof Map<?, ?> t) {
            if (t.get("timeout-seconds") instanceof Number n)
                cfg.teleportTimeoutMillis = n.longValue() * 1000L;

            if (t.get("post-connect-delay-ms") instanceof Number n)
                cfg.postConnectDelayMillis = n.longValue();
        }

        Object msgs = map.get("messages");
        if (msgs instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    cfg.messages.put(e.getKey().toString(), e.getValue().toString());
                }
            }
        }
        return cfg;
    }

    public DatabaseConfig database() { return database; }

    private static String str(Object o, String fallback) {
        return o == null ? fallback : o.toString();
    }

    public boolean isExcluded(String server) {
        return server != null && excluded.contains(server.toLowerCase(Locale.ROOT));
    }

    /**
     * Whether messages are parsed as MiniMessage
     */
    public boolean miniMessage() {
        return miniMessage;
    }

    public boolean ignoreWithoutGravesX() {
        return ignoreWithoutGravesX;
    }

    public long teleportTimeoutMillis() {
        return teleportTimeoutMillis;
    }

    public long postConnectDelayMillis() {
        return postConnectDelayMillis;
    }

    public String message(String key, String... replacements) {
        String text = messages.getOrDefault(key, DEFAULT_MESSAGES.getOrDefault(key, key));

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String value = miniMessage ? replacements[i + 1].replace("<", "\\<") : replacements[i + 1];
            text = text.replace("%" + replacements[i] + "%", value);
        }

        return messages.getOrDefault("prefix", "") + text;
    }
}
