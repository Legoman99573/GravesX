package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores cache data using the currently configured database.
 */
public final class DatabaseCacheBackend implements CacheBackend {
    private final Graves plugin;

    /**
     * Creates the database cache backend.
     */
    public DatabaseCacheBackend(Graves plugin) {
        this.plugin = plugin;

        plugin.getDataManager().setupTempCacheTable();
        clear();
    }

    /**
     * Gets a cached value.
     */
    @Override
    public synchronized byte[] get(String namespace, String key) {
        String sql = "SELECT cache_value FROM " + plugin.getDataManager().getTempCacheTableName() + " WHERE cache_namespace = ? AND cache_key = ?";

        try (Connection connection = plugin.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);
            statement.setString(2, key);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBytes("cache_value") : null;
            }
        } catch (SQLException e) {
            throw new CacheCodec.CacheException("Failed reading database cache", e);
        }
    }

    /**
     * Stores a cached value.
     */
    @Override
    public synchronized void put(String namespace, String key, byte[] value) {
        plugin.getDataManager().putTempCache(namespace, key, value);
    }

    /**
     * Removes and returns a cached value.
     */
    @Override
    public synchronized byte[] remove(String namespace, String key) {
        byte[] previous = get(namespace, key);

        String sql = "DELETE FROM " + plugin.getDataManager().getTempCacheTableName() + " WHERE cache_namespace = ? AND cache_key = ?";

        try (Connection connection = plugin.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);
            statement.setString(2, key);
            statement.executeUpdate();

            return previous;
        } catch (SQLException e) {
            throw new CacheCodec.CacheException("Failed deleting database cache entry", e);
        }
    }

    /**
     * Checks if a cached value exists.
     */
    @Override
    public synchronized boolean contains(String namespace, String key) {
        String sql = "SELECT 1 FROM " + plugin.getDataManager().getTempCacheTableName() + " WHERE cache_namespace = ? AND cache_key = ?";

        try (Connection connection = plugin.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);
            statement.setString(2, key);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new CacheCodec.CacheException(
                    "Failed checking database cache", e);
        }
    }

    /**
     * Gets all keys in a namespace.
     */
    @Override
    public synchronized Set<String> keys(String namespace) {
        Set<String> keys = new HashSet<>();

        String sql = "SELECT cache_key FROM " + plugin.getDataManager().getTempCacheTableName() + " WHERE cache_namespace = ?";

        try (Connection connection = plugin.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    keys.add(result.getString("cache_key"));
                }
            }

            return keys;
        } catch (SQLException e) {
            throw new CacheCodec.CacheException("Failed listing database cache", e);
        }
    }

    /**
     * Gets the number of entries in a namespace.
     */
    @Override
    public synchronized int size(String namespace) {
        String sql = "SELECT COUNT(*) FROM " + plugin.getDataManager().getTempCacheTableName() + " WHERE cache_namespace = ?";

        try (Connection connection = plugin.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new CacheCodec.CacheException("Failed counting database cache", e);
        }
    }

    /**
     * Clears all entries in a namespace.
     */
    @Override
    public synchronized void clearNamespace(String namespace) {
        String sql = "DELETE FROM " + plugin.getDataManager().getTempCacheTableName() + " WHERE cache_namespace = ?";

        try (Connection connection = plugin.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CacheCodec.CacheException("Failed clearing database cache namespace", e);
        }
    }

    /**
     * Clears all cached entries.
     */
    @Override
    public synchronized void clear() {
        plugin.getDataManager().clearTempCacheTable();
    }

    /**
     * Clears and closes the cache backend.
     */
    @Override
    public void close() {
        clear();
    }
}