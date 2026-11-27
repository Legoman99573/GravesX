package dev.cwhead.GravesXProxy.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.cwhead.GravesXProxy.protocol.Protocol;
import dev.cwhead.GravesXProxy.platform.ProxyPlatform;
import dev.cwhead.GravesXProxy.velocity.message.VelocityMessageFormatter;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Velocity implementation of {@link ProxyPlatform}. */
public final class VelocityPlatform implements ProxyPlatform {
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(Protocol.CHANNEL);

    private final Object plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final BooleanSupplier miniMessage;

    public VelocityPlatform(Object plugin, ProxyServer proxy, Logger logger, BooleanSupplier miniMessage) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.miniMessage = miniMessage;
    }

    /** Formats text with '&' codes (and MiniMessage, if enabled in the config) for this player/sender. */
    public Component format(String text) {
        return VelocityMessageFormatter.format(text, miniMessage.getAsBoolean());
    }

    @Override
    public Collection<String> serverNames() {
        return proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).collect(Collectors.toList());
    }

    @Override
    public String currentServer(UUID player) {
        return proxy.getPlayer(player)
                .flatMap(Player::getCurrentServer)
                .map(sc -> sc.getServerInfo().getName())
                .orElse(null);
    }

    @Override
    public void sendToBackend(UUID player, byte[] data) {
        proxy.getPlayer(player).flatMap(Player::getCurrentServer).ifPresent(sc -> sc.sendPluginMessage(CHANNEL, data));
    }

    @Override
    public void connect(UUID player, String server, Consumer<Boolean> result) {
        Optional<Player> p = proxy.getPlayer(player);
        Optional<RegisteredServer> s = proxy.getServer(server);
        if (p.isEmpty() || s.isEmpty()) {
            result.accept(false);
            return;
        }
        p.get().createConnectionRequest(s.get()).connect()
                .thenAccept(r -> result.accept(r.isSuccessful()))
                .exceptionally(t -> {
                    result.accept(false);
                    return null;
                });
    }

    @Override
    public void message(UUID player, String text) {
        proxy.getPlayer(player).ifPresent(p -> p.sendMessage(format(text)));
    }

    @Override
    public void runLater(Runnable task, long delayMillis) {
        proxy.getScheduler().buildTask(plugin, task).delay(delayMillis, TimeUnit.MILLISECONDS).schedule();
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }
}
