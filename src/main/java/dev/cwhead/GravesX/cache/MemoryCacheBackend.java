package dev.cwhead.GravesX.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores cache data in memory.
 */
public final class MemoryCacheBackend implements CacheBackend {
    private final Map<String, Map<String, byte[]>> data = new ConcurrentHashMap<>();

    @Override
    public synchronized byte[] get(String namespace, String key) {
        Map<String, byte[]> entries = data.get(namespace);

        byte[] value = entries == null ? null : entries.get(key);

        return value == null ? null : value.clone();
    }

    @Override
    public synchronized void put(String namespace, String key, byte[] value) {
        data.computeIfAbsent(namespace, ignored -> new HashMap<>()).put(key, value.clone());
    }

    @Override
    public synchronized byte[] remove(String namespace, String key) {
        Map<String, byte[]> entries = data.get(namespace);

        if (entries == null)
            return null;

        byte[] value = entries.remove(key);

        if (entries.isEmpty())
            data.remove(namespace);

        return value == null ? null : value.clone();
    }

    @Override
    public synchronized boolean contains(String namespace, String key) {
        Map<String, byte[]> entries = data.get(namespace);
        return entries != null && entries.containsKey(key);
    }

    @Override
    public synchronized Set<String> keys(String namespace) {
        Map<String, byte[]> entries = data.get(namespace);
        return entries == null ? Collections.emptySet() : new HashSet<>(entries.keySet());
    }

    @Override
    public synchronized int size(String namespace) {
        Map<String, byte[]> entries = data.get(namespace);
        return entries == null ? 0 : entries.size();
    }

    @Override
    public synchronized void clearNamespace(String namespace) {
        data.remove(namespace);
    }

    @Override
    public synchronized void clear() {
        data.clear();
    }

    @Override
    public void close() {
        clear();
    }
}