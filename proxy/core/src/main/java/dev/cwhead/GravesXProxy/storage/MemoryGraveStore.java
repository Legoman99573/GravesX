package dev.cwhead.GravesXProxy.storage;

import dev.cwhead.GravesXProxy.protocol.Protocol.GraveSummary;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Per-proxy store kept in memory and saved to graves.txt. Used when no global database is configured. */
public final class MemoryGraveStore implements GraveStore {
    private final Path file;
    private final Map<UUID, Map<String, List<GraveSummary>>> graves = new ConcurrentHashMap<>();

    public MemoryGraveStore(Path file) {
        this.file = file;
        load();
    }

    @Override
    public void replaceOwner(String server, UUID owner, List<GraveSummary> list) {
        String key = server.toLowerCase(Locale.ROOT);
        Map<String, List<GraveSummary>> perServer = graves.computeIfAbsent(owner, k -> new ConcurrentHashMap<>());
        if (list.isEmpty()) {
            perServer.remove(key);
        } else {
            perServer.put(key, List.copyOf(list));
        }
    }

    @Override
    public Map<String, Integer> counts(UUID owner) {
        Map<String, Integer> result = new HashMap<>();
        graves.getOrDefault(owner, Map.of()).forEach((server, list) -> result.put(server, list.size()));
        return result;
    }

    @Override
    public List<GraveSummary> graves(UUID owner, String server, int limit) {
        List<GraveSummary> list = graves.getOrDefault(owner, Map.of()).getOrDefault(server.toLowerCase(Locale.ROOT), List.of());
        List<GraveSummary> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingLong(GraveSummary::created).reversed());
        return sorted.size() > limit ? sorted.subList(0, limit) : sorted;
    }

    @Override
    public void forgetServer(String server) {
        String key = server.toLowerCase(Locale.ROOT);
        graves.values().forEach(m -> m.remove(key));
    }

    @Override
    public synchronized void save() {
        List<String> lines = new ArrayList<>();
        graves.forEach((owner, perServer) -> perServer.forEach((server, list) -> {
            for (GraveSummary g : list) {
                lines.add("G|" + owner + "|" + enc(server) + "|" + g.id() + "|" + enc(g.world()) + "|"
                        + g.x() + "|" + g.y() + "|" + g.z() + "|" + g.created());
            }
        }));
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // best effort; state is rebuilt from backends as players join
        }
    }

    private void load() {
        if (Files.notExists(file)) return;
        Map<String, List<GraveSummary>> loaded = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\\|");
                try {
                    if (p[0].equals("G")) {
                        loaded.computeIfAbsent(p[1] + "|" + p[2], k -> new ArrayList<>())
                                .add(new GraveSummary(UUID.fromString(p[3]), dec(p[4]), Double.parseDouble(p[5]),
                                        Double.parseDouble(p[6]), Double.parseDouble(p[7]), Long.parseLong(p[8])));
                    }
                } catch (RuntimeException ignored) {
                    // skip malformed line
                }
            }
        } catch (IOException ignored) {
            return;
        }
        loaded.forEach((key, list) -> {
            String[] k = key.split("\\|");
            replaceOwner(dec(k[1]), UUID.fromString(k[0]), list);
        });
    }

    @Override
    public String describe() {
        return "file (graves.txt, per proxy)";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String dec(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
