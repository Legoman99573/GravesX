package dev.cwhead.GravesXProxy.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Which backend servers are known to run GravesX (they answered the handshake). Saved to servers.txt. */
public final class ServerRegistry {
    private final Set<String> installed = ConcurrentHashMap.newKeySet();

    /** @return true if the server was not known as GravesX-enabled before. */
    public boolean markInstalled(String server) {
        return installed.add(server.toLowerCase(Locale.ROOT));
    }

    public boolean isInstalled(String server) {
        return installed.contains(server.toLowerCase(Locale.ROOT));
    }

    public void forget(String server) {
        installed.remove(server.toLowerCase(Locale.ROOT));
    }

    public synchronized void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, List.copyOf(installed));
        } catch (IOException ignored) {
            // best effort; rediscovered when players join
        }
    }

    public synchronized void load(Path file) {
        if (Files.notExists(file)) return;
        try {
            installed.addAll(Files.readAllLines(file));
        } catch (IOException ignored) {
            // rediscovered when players join
        }
    }
}
