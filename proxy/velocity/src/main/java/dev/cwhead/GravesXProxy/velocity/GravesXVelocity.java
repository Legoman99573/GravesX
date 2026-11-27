package dev.cwhead.GravesXProxy.velocity;

import com.alessiodp.libby.VelocityLibraryManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.cwhead.GravesXProxy.ProxyCore;
import dev.cwhead.GravesXProxy.library.ProxyLibraries;
import dev.cwhead.GravesXProxy.velocity.command.VelocityAdminCommand;
import dev.cwhead.GravesXProxy.velocity.listener.VelocityListener;
import dev.cwhead.GravesXProxy.velocity.platform.VelocityPlatform;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity entry point.
 */
public final class GravesXVelocity {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private ProxyCore core;

    @Inject
    public GravesXVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        VelocityPlatform platform = new VelocityPlatform(this, proxy, logger, () -> core != null && core.isMiniMessage());
        ProxyLibraries libraries = new ProxyLibraries(new VelocityLibraryManager<>(this, logger, dataDirectory, proxy.getPluginManager()), platform);
        core = new ProxyCore(platform, dataDirectory, libraries);

        proxy.getChannelRegistrar().register(VelocityPlatform.CHANNEL);
        proxy.getEventManager().register(this, new VelocityListener(core));
        VelocityAdminCommand.register(this, proxy, core, platform);

        logger.info("GravesX proxy support enabled.");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.shutdown();
        }
    }
}
