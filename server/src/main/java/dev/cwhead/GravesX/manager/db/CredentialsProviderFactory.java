package dev.cwhead.GravesX.manager.db;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.DataManager;
import com.zaxxer.hikari.HikariCredentialsProvider;
import com.zaxxer.hikari.util.Credentials;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

/**
 * Creates the correct HikariCredentialsProvider for the active DB type.
 * Reads from plugin config on every call so reloads/secret-rotation can take effect
 * for newly-created connections.
 */
public final class CredentialsProviderFactory {
    private CredentialsProviderFactory() {}

    public static HikariCredentialsProvider forType(Graves plugin, DataManager.Type type) {
        return switch (type) {
            case POSTGRESQL -> new ConfigPathProvider(plugin, "settings.storage.postgresql.username", "settings.storage.postgresql.password");
            case MYSQL, MARIADB -> new ConfigPathProvider(plugin, "settings.storage.mysql.username", "settings.storage.mysql.password");
            case MSSQL -> new ConfigPathProvider(plugin, "settings.storage.mssql.username", "settings.storage.mssql.password");
            case H2 -> new ConfigPathProvider(plugin, "settings.storage.h2.username", "settings.storage.h2.password");

            case SQLITE, INVALID -> null;
        };
    }

    /**
     * Simple provider that fetches user/pass from config keys.
     * You can subclass this for Vault/HTTP/etc later.
     */
    public static class ConfigPathProvider implements HikariCredentialsProvider {
        private final Graves plugin;
        private final String userPath;
        private final String passPath;

        public ConfigPathProvider(Graves plugin, String userPath, String passPath) {
            this.plugin = Objects.requireNonNull(plugin, "plugin");
            this.userPath = Objects.requireNonNull(userPath, "userPath");
            this.passPath = Objects.requireNonNull(passPath, "passPath");
        }

        @Override
        public Credentials getCredentials() {
            FileConfiguration cfg = plugin.getConfig();
            String user = cfg.getString(userPath, "");
            String pass = cfg.getString(passPath, "");
            return Credentials.of(user, pass);
        }
    }
}