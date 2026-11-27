package dev.cwhead.GravesXProxy.bungee.platform;

import dev.cwhead.GravesXProxy.protocol.Protocol;
import dev.cwhead.GravesXProxy.platform.ProxyPlatform;
import dev.cwhead.GravesXProxy.bungee.message.BungeeMessageFormatter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * BungeeCord implementation of {@link ProxyPlatform}.
 */
public final class BungeePlatform implements ProxyPlatform {
    private final Plugin plugin;
    private final BooleanSupplier miniMessage;

    public BungeePlatform(Plugin plugin, BooleanSupplier miniMessage) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    /**
     * Formats text with '&' codes (and the MiniMessage subset, if enabled in the config).
     */
    public BaseComponent[] format(String text) {
        return BungeeMessageFormatter.format(text, miniMessage.getAsBoolean());
    }

    @Override
    public Collection<String> serverNames() {
        return plugin.getProxy().getServers().keySet();
    }

    @Override
    public String currentServer(UUID player) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(player);
        return (p == null || p.getServer() == null) ? null : p.getServer().getInfo().getName();
    }

    @Override
    public void sendToBackend(UUID player, byte[] data) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(player);

        if (p != null && p.getServer() != null) {
            p.getServer().sendData(Protocol.CHANNEL, data);
        }
    }

    @Override
    public void connect(UUID player, String server, Consumer<Boolean> result) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(player);
        ServerInfo info = plugin.getProxy().getServerInfo(server);

        if (p == null || info == null) {
            result.accept(false);
            return;
        }

        p.connect(info, (success, error) -> result.accept(success != null && success));
    }

    @Override
    public void message(UUID player, String text) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(player);

        if (p != null) {
            p.sendMessage(format(text));
        }
    }

    @Override
    public void runLater(Runnable task, long delayMillis) {
        plugin.getProxy().getScheduler().schedule(plugin, task, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }
}
