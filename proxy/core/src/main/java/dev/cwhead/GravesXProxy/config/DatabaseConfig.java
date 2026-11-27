package dev.cwhead.GravesXProxy.config;

import java.util.Map;

/** The "database" section of the proxy config. */
public record DatabaseConfig(boolean enabled, String type, String host, int port, String name,
                             String username, String password, String tablePrefix, Map<String, String> options) {
    public static DatabaseConfig disabled() {
        return new DatabaseConfig(false, "MYSQL", "localhost", 0, "gravesx", "", "", "gravesx_", Map.of());
    }
}
