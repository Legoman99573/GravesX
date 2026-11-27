package dev.cwhead.GravesXProxy.bungee;

import com.alessiodp.libby.StandaloneLibraryManager;
import com.alessiodp.libby.logging.adapters.JDKLogAdapter;
import dev.cwhead.GravesXProxy.protocol.Protocol;
import dev.cwhead.GravesXProxy.ProxyCore;
import dev.cwhead.GravesXProxy.library.ProxyLibraries;
import dev.cwhead.GravesXProxy.bungee.command.BungeeAdminCommand;
import dev.cwhead.GravesXProxy.bungee.listener.BungeeListener;
import dev.cwhead.GravesXProxy.bungee.platform.BungeePlatform;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord entry point. Only wires things together; the logic lives in {@link ProxyCore}
 * and the classes in this package. Metadata comes from bungee.yml (filtered from pom.xml).
 */
public final class GravesXBungee extends Plugin {
    private ProxyCore core;

    @Override
    public void onEnable() {
        BungeePlatform platform = new BungeePlatform(this, () -> core != null && core.isMiniMessage());
        // Bungee's plugin class loader is a URLClassLoader, which is what StandaloneLibraryManager appends to.
        ProxyLibraries libraries = new ProxyLibraries(
                new StandaloneLibraryManager(new JDKLogAdapter(getLogger()), getDataFolder().toPath()), platform);
        core = new ProxyCore(platform, getDataFolder().toPath(), libraries);

        getProxy().registerChannel(Protocol.CHANNEL);
        getProxy().getPluginManager().registerListener(this, new BungeeListener(core));
        getProxy().getPluginManager().registerCommand(this, new BungeeAdminCommand(core, platform));

        getLogger().info("GravesX proxy support enabled.");
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
        getProxy().unregisterChannel(Protocol.CHANNEL);
    }
}
