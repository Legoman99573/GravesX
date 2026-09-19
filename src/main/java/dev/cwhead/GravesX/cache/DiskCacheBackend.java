package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class DiskCacheBackend implements CacheBackend {
    private final Graves plugin;
    private final Path root;

    public DiskCacheBackend(Graves plugin) {
        this.plugin = plugin;
        this.root = plugin.getDataFolder().toPath().resolve(".cache");
        clear();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create GravesX disk cache at " + root, e);
        }
    }

    private Path dir(String namespace) {
        return root.resolve(namespace);
    }

    private String fileName(String key) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".cache";
    }

    private String keyFromFile(String file) {
        String encoded = file.substring(0, file.length() - ".cache".length());
        return new String(Base64.getUrlDecoder().decode(encoded),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private Path file(String namespace, String key) {
        return dir(namespace).resolve(fileName(key));
    }

    @Override public synchronized byte[] get(String namespace, String key) {
        Path path = file(namespace, key);
        if (!Files.exists(path)) return null;
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed reading disk cache " + path, e);
        }
    }

    @Override public synchronized void put(String namespace, String key, byte[] value) {
        Path directory = dir(namespace);
        Path target = file(namespace, key);
        try {
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, ".write-", ".tmp");
            Files.write(temp, value, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed writing disk cache " + target, e);
        }
    }

    @Override public synchronized byte[] remove(String namespace, String key) {
        byte[] previous = get(namespace, key);
        try {
            Files.deleteIfExists(file(namespace, key));
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed deleting disk cache entry", e);
        }
        return previous;
    }

    @Override public synchronized boolean contains(String namespace, String key) {
        return Files.exists(file(namespace, key));
    }

    @Override public synchronized Set<String> keys(String namespace) {
        Path directory = dir(namespace);
        if (!Files.isDirectory(directory)) return Collections.emptySet();
        Set<String> result = new HashSet<>();
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".cache"))
                    .forEach(n -> {
                        try { result.add(keyFromFile(n)); }
                        catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Ignoring malformed cache file " + n);
                        }
                    });
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed listing disk cache " + directory, e);
        }
        return result;
    }

    @Override public int size(String namespace) {
        return keys(namespace).size();
    }

    @Override public synchronized void clearNamespace(String namespace) {
        deleteTree(dir(namespace));
    }

    @Override public synchronized void clear() {
        deleteTree(root);
    }

    private void deleteTree(Path path) {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); }
                catch (IOException e) {
                    throw new CacheCodec.CacheException("Failed deleting " + p, e);
                }
            });
        } catch (IOException e) {
            throw new CacheCodec.CacheException("Failed clearing cache path " + path, e);
        }
    }

    @Override public void close() {
        clear();
    }
}
