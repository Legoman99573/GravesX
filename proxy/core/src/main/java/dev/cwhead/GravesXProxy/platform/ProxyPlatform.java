package dev.cwhead.GravesXProxy.platform;

import dev.cwhead.GravesXProxy.ProxyCore;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/** Thin abstraction over Velocity / BungeeCord so all logic lives in {@link ProxyCore}. */
public interface ProxyPlatform {
    /** Names of every server registered on the proxy. */
    Collection<String> serverNames();

    /** Name of the server the player is currently on, or null if unknown/offline. */
    String currentServer(UUID player);

    /** Sends a plugin message to the backend the player is currently connected to. */
    void sendToBackend(UUID player, byte[] data);

    /** Connects the player to a server; callback receives whether it succeeded. */
    void connect(UUID player, String server, Consumer<Boolean> result);

    /** Sends a legacy ('&amp;' or section colored) chat message to a player. */
    void message(UUID player, String legacyText);

    void runLater(Runnable task, long delayMillis);

    void info(String message);

    void warn(String message);
}
