package dev.cwhead.GravesX.cache;

import java.util.Set;

/**
 * Represents a cache storage backend.
 */
public interface CacheBackend extends AutoCloseable {

    /**
     * Gets a cached value.
     */
    byte[] get(String namespace, String key);

    /**
     * Stores a cached value.
     */
    void put(String namespace, String key, byte[] value);

    /**
     * Removes and returns a cached value.
     */
    byte[] remove(String namespace, String key);

    /**
     * Checks if a cached value exists.
     */
    boolean contains(String namespace, String key);

    /**
     * Gets all keys in a namespace.
     */
    Set<String> keys(String namespace);

    /**
     * Gets the number of entries in a namespace.
     */
    int size(String namespace);

    /**
     * Clears all entries in a namespace.
     */
    void clearNamespace(String namespace);

    /**
     * Clears all cached entries.
     */
    void clear();

    /**
     * Closes the cache backend.
     */
    @Override
    void close();
}