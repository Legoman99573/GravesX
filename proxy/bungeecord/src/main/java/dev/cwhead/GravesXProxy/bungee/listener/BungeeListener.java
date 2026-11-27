package dev.cwhead.GravesXProxy.bungee.listener;

import dev.cwhead.GravesXProxy.protocol.Protocol;
import dev.cwhead.GravesXProxy.ProxyCore;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 *  Forwards BungeeCord events to {@link ProxyCore}.
 */
public final class BungeeListener implements Listener {
    private final ProxyCore core;

    public BungeeListener(ProxyCore core) {
        this.core = core;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(Protocol.CHANNEL))
            return;

        event.setCancelled(true);

        if (!(event.getSender() instanceof Server server))
            return;

        if (!(event.getReceiver() instanceof ProxiedPlayer player))
            return;

        core.handleBackend(server.getInfo().getName(), player.getUniqueId(), event.getData());
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (player.getServer() != null) {
            core.onPlayerConnected(player.getUniqueId(), player.getServer().getInfo().getName());
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        core.onPlayerQuit(event.getPlayer().getUniqueId());
    }
}
