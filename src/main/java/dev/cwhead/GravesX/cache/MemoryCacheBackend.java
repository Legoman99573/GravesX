package dev.cwhead.GravesX.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoryCacheBackend implements CacheBackend {
    private final Map<String, Map<String, byte[]>> data = new ConcurrentHashMap<>();

    private Map<String, byte[]> namespace(String namespace) {
        return data.computeIfAbsent(namespace, ignored -> new ConcurrentHashMap<>());
    }

    @Override public byte[] get(String namespace, String key) {
        byte[] value = namespace(namespace).get(key);
        return value == null ? null : value.clone();
    }

    @Override public void put(String namespace, String key, byte[] value) {
        namespace(namespace).put(key, value.clone());
    }

    @Override public byte[] remove(String namespace, String key) {
        byte[] value = namespace(namespace).remove(key);
        return value == null ? null : value.clone();
    }

    @Override public boolean contains(String namespace, String key) {
        return namespace(namespace).containsKey(key);
    }

    @Override public Set<String> keys(String namespace) {
        return new HashSet<>(namespace(namespace).keySet());
    }

    @Override public int size(String namespace) {
        return namespace(namespace).size();
    }

    @Override public void clearNamespace(String namespace) {
        namespace(namespace).clear();
    }

    @Override public void clear() {
        data.clear();
    }

    @Override public void close() {
        clear();
    }
}
