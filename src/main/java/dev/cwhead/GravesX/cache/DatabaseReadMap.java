package dev.cwhead.GravesX.cache;

import java.util.*;
import java.util.function.*;

/**
 * A read-only map view over persistent rows, with no retained values or keys.
 */
public final class DatabaseReadMap<K, V> extends AbstractMap<K, V> {
    private final Function<K, V> reader;
    private final Supplier<Set<K>> keys;
    private final Predicate<K> exists;
    private final IntSupplier count;

    public DatabaseReadMap(Function<K, V> reader, Supplier<Set<K>> keys,
                           Predicate<K> exists, IntSupplier count) {
        this.reader = reader;
        this.keys = keys;
        this.exists = exists;
        this.count = count;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        if (key == null) return null;

        try {
            return reader.apply((K) key);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        if (key == null) return false;

        try {
            return exists.test((K) key);
        } catch (ClassCastException ex) {
            return false;
        }
    }

    @Override
    public int size() {
        return count.getAsInt();
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() {
                return DatabaseReadMap.this.size();
            }

            @Override public boolean contains(Object key) {
                return containsKey(key);
            }

            @Override public Iterator<K> iterator() {
                return keys.get().iterator();
            }
        });
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() {
                return DatabaseReadMap.this.size();
            }

            @Override public Iterator<Entry<K, V>> iterator() {
                Iterator<K> identifiers = keys.get().iterator();
                return new Iterator<>() {
                    private Entry<K, V> next;

                    @Override public boolean hasNext() {
                        while (next == null && identifiers.hasNext()) {
                            K key = identifiers.next();
                            V value = reader.apply(key);
                            if (value != null)
                                next = new SimpleImmutableEntry<>(key, value);
                        }

                        return next != null;
                    }

                    @Override public Entry<K, V> next() {
                        if (!hasNext())
                            throw new NoSuchElementException();

                        Entry<K, V> result = next;
                        next = null;
                        return result;
                    }
                };
            }
        });
    }

    @Override public V put(K key, V value) {
        throw readOnly();
    }

    @Override public V remove(Object key) {
        throw readOnly();
    }

    @Override public void clear() {
        throw readOnly();
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("DATABASE cache is disabled; change persistent data through DataManager");
    }
}
