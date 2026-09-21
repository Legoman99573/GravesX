package dev.cwhead.GravesX.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores cache data in memory.
 */
public final class MemoryCacheBackend implements CacheBackend {
    private final Map<String, Map<String, byte[]>> data = new ConcurrentHashMap<>();

    /**
     * Gets or creates a cache namespace.
     */
    private Map<String, byte[]> namespace(String namespace) {
        return data.computeIfAbsent(namespace, ignored -> new ConcurrentHashMap<>());
    }

    /**
     * Gets a cached value.
     */
    @Override
    public byte[] get(String namespace, String key) {
        byte[] value = namespace(namespace).get(key);
        return value == null ? null : value.clone();
    }

    /**
     * Stores a cached value.
     */
    @Override
    public void put(String namespace, String key, byte[] value) {
        namespace(namespace).put(key, value.clone());
    }

    /**
     * Removes and returns a cached value.
     */
    @Override
    public byte[] remove(String namespace, String key) {
        byte[] value = namespace(namespace).remove(key);
        return value == null ? null : value.clone();
    }

    /**
     * Checks if a cached value exists.
     */
    @Override
    public boolean contains(String namespace, String key) {
        return namespace(namespace).containsKey(key);
    }

    /**
     * Gets all keys in a namespace.
     */
    @Override
    public Set<String> keys(String namespace) {
        return new HashSet<>(namespace(namespace).keySet());
    }

    /**
     * Gets the number of entries in a namespace.
     */
    @Override
    public int size(String namespace) {
        return namespace(namespace).size();
    }

    /**
     * Clears all entries in a namespace.
     */
    @Override
    public void clearNamespace(String namespace) {
        namespace(namespace).clear();
    }

    /**
     * Clears all cached entries.
     */
    @Override
    public void clear() {
        data.clear();
    }

    /**
     * Clears and closes the cache backend.
     */
    @Override
    public void close() {
        clear();
    }
}