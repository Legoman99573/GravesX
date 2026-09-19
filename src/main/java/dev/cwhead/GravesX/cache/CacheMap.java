package dev.cwhead.GravesX.cache;

import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;

/**
 * Provides a map backed by the configured cache type.
 */
public final class CacheMap<K, V> extends AbstractMap<K, V> {
    private final CacheType type;
    private final String namespace;
    private final CacheBackend backend;
    private final CacheCodec codec;
    private final Function<K, String> keyEncoder;
    private final Function<String, K> keyDecoder;
    private final Map<K, V> memory;

    /**
     * Creates a cache-backed map.
     */
    public CacheMap(CacheType type, String namespace, CacheBackend backend, CacheCodec codec,
                    Function<K, String> keyEncoder, Function<String, K> keyDecoder) {
        this.type = type;
        this.namespace = namespace;
        this.backend = backend;
        this.codec = codec;
        this.keyEncoder = keyEncoder;
        this.keyDecoder = keyDecoder;
        this.memory = type == CacheType.NORMAL ? new HashMap<>() : null;
    }

    /**
     * Gets a cached value.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        if (type == CacheType.NORMAL) return memory.get(key);
        if (key == null) return null;
        return (V) codec.decode(backend.get(namespace, keyEncoder.apply((K) key)));
    }

    /**
     * Stores a cached value.
     */
    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "cache key");
        if (type == CacheType.NORMAL) return memory.put(key, value);
        V old = get(key);
        backend.put(namespace, keyEncoder.apply(key), codec.encode(value));
        return old;
    }

    /**
     * Removes and returns a cached value.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        if (type == CacheType.NORMAL) return memory.remove(key);
        if (key == null) return null;
        return (V) codec.decode(backend.remove(namespace, keyEncoder.apply((K) key)));
    }

    /**
     * Checks if a cached key exists.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        if (type == CacheType.NORMAL) return memory.containsKey(key);
        if (key == null) return false;
        return backend.contains(namespace, keyEncoder.apply((K) key));
    }

    /**
     * Gets the number of cached entries.
     */
    @Override
    public int size() {
        return type == CacheType.NORMAL ? memory.size() : backend.size(namespace);
    }

    /**
     * Clears all cached entries.
     */
    @Override
    public void clear() {
        if (type == CacheType.NORMAL) memory.clear();
        else backend.clearNamespace(namespace);
    }

    /**
     * Gets a snapshot of all cached entries.
     */
    @Override
    public @NonNull Set<Entry<K, V>> entrySet() {
        if (type == CacheType.NORMAL) return memory.entrySet();

        Set<Entry<K, V>> snapshot = new LinkedHashSet<>();
        for (String rawKey : backend.keys(namespace)) {
            K key = keyDecoder.apply(rawKey);
            V value = get(key);
            snapshot.add(new SimpleImmutableEntry<>(key, value));
        }
        return snapshot;
    }

    /**
     * Gets a value or creates and stores it if absent.
     */
    @Override
    public V computeIfAbsent(K key, @NonNull Function<? super K, ? extends V> mappingFunction) {
        V current = get(key);
        if (current != null) return current;
        V created = mappingFunction.apply(key);
        if (created != null) put(key, created);
        return created;
    }
}