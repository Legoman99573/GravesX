package dev.cwhead.GravesXProxy.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import dev.cwhead.GravesXProxy.ProxyCore;
import dev.cwhead.GravesXProxy.velocity.platform.VelocityPlatform;

/** Forwards Velocity events to {@link ProxyCore}. */
public final class VelocityListener {
    private final ProxyCore core;

    public VelocityListener(ProxyCore core) {
        this.core = core;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(VelocityPlatform.CHANNEL)) return;

        // Never let this channel travel on to clients or backends.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        // Only trust messages that come from a backend server, never from a (possibly modded) client.
        if (!(event.getSource() instanceof ServerConnection connection)) return;
        core.handleBackend(connection.getServerInfo().getName(), connection.getPlayer().getUniqueId(), event.getData());
    }

    @Subscribe
    public void onPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(sc ->
                core.onPlayerConnected(player.getUniqueId(), sc.getServerInfo().getName()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        core.onPlayerQuit(event.getPlayer().getUniqueId());
    }
}
