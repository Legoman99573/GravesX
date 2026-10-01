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
    public synchronized V put(K key, V value) {
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
    public synchronized V remove(Object key) {
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
    public synchronized void clear() {
        if (type == CacheType.NORMAL) memory.clear();
        else backend.clearNamespace(namespace);
    }

    /**
     * Returns a backed view. External caches snapshot keys only and decode one
     * value per iterator step; neither the map nor the view retains grave data.
     * Iterator removal and entry replacement write through to the backend.
     */
    @Override
    public @NonNull Set<Entry<K, V>> entrySet() {
        if (type == CacheType.NORMAL) return memory.entrySet();
        return new AbstractSet<>() {
            @Override
            public int size() { return CacheMap.this.size(); }

            @Override
            public void clear() { CacheMap.this.clear(); }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                Iterator<K> keys = keySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() { return keys.hasNext(); }

                    @Override
                    public Entry<K, V> next() {
                        K key = keys.next();
                        return new SimpleEntry<>(key, get(key)) {
                            @Override
                            public V setValue(V value) {
                                V previous = CacheMap.this.put(key, value);
                                super.setValue(value);
                                return previous;
                            }
                        };
                    }

                    @Override
                    public void remove() { keys.remove(); }
                };
            }
        };
    }

    /** Returns a backed key view without reading or decoding cached values. */
    @Override
    public @NonNull Set<K> keySet() {
        if (type == CacheType.NORMAL) return memory.keySet();
        return new AbstractSet<>() {
            @Override
            public int size() { return CacheMap.this.size(); }

            @Override
            public boolean contains(Object key) { return containsKey(key); }

            @Override
            public void clear() { CacheMap.this.clear(); }

            @Override
            public boolean remove(Object key) {
                if (!containsKey(key)) return false;
                CacheMap.this.remove(key);
                return true;
            }

            @Override
            public Iterator<K> iterator() {
                Iterator<String> keys = backend.keys(namespace).iterator();
                return new Iterator<>() {
                    private String current;
                    private boolean removable;

                    @Override
                    public boolean hasNext() { return keys.hasNext(); }

                    @Override
                    public K next() {
                        current = keys.next();
                        removable = true;
                        return keyDecoder.apply(current);
                    }

                    @Override
                    public void remove() {
                        if (!removable) throw new IllegalStateException();
                        CacheMap.this.remove(keyDecoder.apply(current));
                        removable = false;
                    }
                };
            }
        };
    }

    /**
     * Writes an existing value without decoding its previous copy. A delayed
     * write after grave removal must not recreate the removed entry.
     */
    public synchronized void saveExisting(K key, V value) {
        if (type == CacheType.NORMAL) {
            if (memory.containsKey(key)) memory.put(key, value);
        } else if (backend.contains(namespace, keyEncoder.apply(key))) {
            backend.put(namespace, keyEncoder.apply(key), codec.encode(value));
        }
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