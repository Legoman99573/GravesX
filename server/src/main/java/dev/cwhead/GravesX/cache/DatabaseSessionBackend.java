package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * Temporary interaction state in the configured database.
 */
public final class DatabaseSessionBackend implements CacheBackend {
    private final Graves plugin;
    private final String table;
    private final String session = UUID.randomUUID().toString();

    public DatabaseSessionBackend(Graves plugin) {
        this.plugin = plugin;
        this.table = plugin.getDataManager().getStoragePrefix() + "sessionstate";
        String type = plugin.getDataManager().getType();
        String binary = switch (type) {
            case "POSTGRESQL" -> "BYTEA";
            case "MSSQL" -> "VARBINARY(MAX)";
            case "MYSQL", "MARIADB" -> "LONGBLOB";
            default -> "BLOB";
        };

        String text = switch (type) {
            case "MSSQL" -> "NVARCHAR(MAX)";
            case "MYSQL", "MARIADB" -> "LONGTEXT";
            case "H2" -> "CLOB";
            default -> "TEXT";
        };

        String prefix = "MSSQL".equals(type)
                ? "IF OBJECT_ID('" + table + "', 'U') IS NULL CREATE TABLE "
                : "CREATE TABLE IF NOT EXISTS ";

        String sql = prefix + table + " (session_id VARCHAR(36) NOT NULL, state_namespace VARCHAR(64) NOT NULL, "
                + "state_key VARCHAR(192) NOT NULL, value_format VARCHAR(16) NOT NULL, value_binary " + binary
                + ", value_text " + text + ", updated_at BIGINT NOT NULL, PRIMARY KEY (session_id, state_namespace, state_key))";

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException ex) {
            throw failure("creating session table", ex);
        }
    }

    private Connection connection() throws SQLException {
        return plugin.getDataManager().getConnection();
    }

    private String where() {
        return " WHERE session_id = ? AND state_namespace = ? AND state_key = ?";
    }

    private void bind(PreparedStatement statement, String namespace, String key) throws SQLException {
        statement.setString(1, session);
        statement.setString(2, namespace);
        statement.setString(3, key);
    }

    @Override
    public synchronized byte[] get(String namespace, String key) {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT value_format, value_binary, value_text FROM " + table + where())) {
            bind(statement, namespace, key);

            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                return "yaml".equals(rows.getString("value_format"))
                        ? rows.getString("value_text").getBytes(StandardCharsets.UTF_8) : rows.getBytes("value_binary");
            }
        } catch (SQLException ex) {
            throw failure("reading session state", ex);
        }
    }

    @Override
    public synchronized void put(String namespace, String key, byte[] value) {
        Objects.requireNonNull(value, "session value");
        try (Connection connection = connection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement remove = connection.prepareStatement("DELETE FROM " + table + where())) {
                    bind(remove, namespace, key);
                    remove.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table
                        + " (session_id, state_namespace, state_key, value_format, value_binary, value_text, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    bind(insert, namespace, key);
                    boolean readable = DebugCacheCodec.isText(value);
                    insert.setString(4, readable ? "yaml" : "binary");

                    if (readable) {
                        insert.setNull(5, Types.BINARY);
                        insert.setString(6, new String(value, StandardCharsets.UTF_8));
                    } else {
                        insert.setBytes(5, value);
                        insert.setNull(6, Types.VARCHAR);
                    }

                    insert.setLong(7, System.currentTimeMillis());
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }

                throw ex;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException ex) {
            throw failure("writing session state", ex);
        }
    }

    @Override
    public synchronized byte[] remove(String namespace, String key) {
        byte[] previous = get(namespace, key);

        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + where())) {
            bind(statement, namespace, key);
            statement.executeUpdate();
            return previous;
        } catch (SQLException ex) {
            throw failure("removing session state", ex);
        }
    }

    @Override
    public synchronized boolean contains(String namespace, String key) {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + where())) {
            bind(statement, namespace, key);

            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException ex) {
            throw failure("checking session state", ex);
        }
    }

    @Override
    public synchronized Set<String> keys(String namespace) {
        Set<String> keys = new LinkedHashSet<>();

        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT state_key FROM " + table + " WHERE session_id = ? AND state_namespace = ?")) {
            statement.setString(1, session); statement.setString(2, namespace);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) keys.add(rows.getString(1));
            }

            return keys;
        } catch (SQLException ex) {
            throw failure("listing session state", ex);
        }
    }

    @Override
    public synchronized int size(String namespace) {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE session_id = ? AND state_namespace = ?")) {
            statement.setString(1, session); statement.setString(2, namespace);

            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw failure("counting session state", ex);
        }
    }

    @Override public synchronized void clearNamespace(String namespace) {
        clear(namespace);
    }

    @Override public synchronized void clear() {
        clear(null);
    }

    @Override public void close() {
        clear();
    }

    private void clear(String namespace) {
        String sql = "DELETE FROM " + table + " WHERE session_id = ?" + (namespace == null ? "" : " AND state_namespace = ?");

        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, session);
            if (namespace != null) statement.setString(2, namespace);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw failure("clearing session state", ex);
        }
    }

    private CacheCodec.CacheException failure(String action, SQLException ex) {
        return new CacheCodec.CacheException("Failed " + action, ex);
    }
}
