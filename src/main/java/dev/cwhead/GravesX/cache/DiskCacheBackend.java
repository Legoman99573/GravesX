package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Stores cache data on disk.
 */
public final class DiskCacheBackend implements CacheBackend {
    private final String extension;
    private final boolean debug;

    private final Graves plugin;
    private final Path root;

    /**
     * Creates the disk cache backend.
     */
    public DiskCacheBackend(Graves plugin) {
        this(plugin, ".cache");
    }

    public DiskCacheBackend(Graves plugin, String directory) {
        this.plugin = plugin;
        this.debug = DebugCacheCodec.enabled(plugin);
        this.extension = debug ? ".gxrcache" : ".gxcache";
        this.root = plugin.getDataFolder().toPath().resolve(directory);
        clear();

        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create GravesX disk cache at " + root, e);
        }
    }

    /**
     * Gets the directory for a namespace.
     */
    private Path dir(String namespace) {
        return root.resolve(namespace);
    }

    /**
     * Gets the cache file name for a key.
     */
    private String fileName(String key) {
        return (debug ? URLEncoder.encode(key, StandardCharsets.UTF_8)
                : Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8))) + extension;
    }

    /**
     * Gets the cache key from a file name.
     */
    private String keyFromFile(String file) {
        String encoded = file.substring(0, file.length() - extension.length());

        return debug ? URLDecoder.decode(encoded, StandardCharsets.UTF_8)
                : new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    /**
     * Gets the cache file for a key.
     */
    private Path file(String namespace, String key) {
        return dir(namespace).resolve(fileName(key));
    }

    /**
     * Gets a cached value.
     */
    @Override
    public synchronized byte[] get(String namespace, String key) {
        Path path = file(namespace, key);
        if (!Files.exists(path)) return null;

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed reading disk cache " + path, e);
        }
    }

    /**
     * Stores a cached value.
     */
    @Override
    public synchronized void put(String namespace, String key, byte[] value) {
        Path directory = dir(namespace);
        Path target = file(namespace, key);

        try {
            Files.createDirectories(directory);

            Path temp = Files.createTempFile(directory, ".write-", ".tmp");
            Files.write(temp, value, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed writing disk cache " + target, e);
        }
    }

    /**
     * Removes and returns a cached value.
     */
    @Override
    public synchronized byte[] remove(String namespace, String key) {
        byte[] previous = get(namespace, key);

        try {
            Files.deleteIfExists(file(namespace, key));
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed deleting disk cache entry", e);
        }

        return previous;
    }

    /**
     * Checks if a cached value exists.
     */
    @Override
    public synchronized boolean contains(String namespace, String key) {
        return Files.exists(file(namespace, key));
    }

    /**
     * Gets all keys in a namespace.
     */
    @Override
    public synchronized Set<String> keys(String namespace) {
        Path directory = dir(namespace);
        if (!Files.isDirectory(directory)) return Collections.emptySet();

        Set<String> result = new HashSet<>();

        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(extension))
                    .forEach(n -> {
                        try {
                            result.add(keyFromFile(n));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Ignoring malformed cache file " + n);
                        }
                    });
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed listing disk cache " + directory, e);
        }

        return result;
    }

    /**
     * Gets the number of entries in a namespace.
     */
    @Override
    public int size(String namespace) {
        return keys(namespace).size();
    }

    /**
     * Clears all entries in a namespace.
     */
    @Override
    public synchronized void clearNamespace(String namespace) {
        deleteTree(dir(namespace));
    }

    /**
     * Clears all cached entries.
     */
    @Override
    public synchronized void clear() {
        deleteTree(root);
    }

    /**
     * Deletes a cache directory and its contents.
     */
    private void deleteTree(Path path) {
        if (!Files.exists(path)) return;

        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new CacheCodec.CacheException("Failed deleting " + p, e);
                }
            });
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed clearing cache path " + path, e);
        }
    }

    /**
     * Clears and closes the cache backend.
     */
    @Override
    public void close() {
        clear();
    }
}