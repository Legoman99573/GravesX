package dev.cwhead.GravesX.module.listener;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.ModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public final class StartupListener implements Listener {
    private final Graves plugin;
    private final ModuleManager manager;
    public StartupListener(Graves plugin, ModuleManager manager) { this.plugin = plugin; this.manager = manager; }

    @EventHandler
    public void onServerLoad(ServerLoadEvent e) {
        if (e.getType() != ServerLoadEvent.LoadType.STARTUP) return;
        manager.loadAll();
        Bukkit.getScheduler().runTask(plugin, manager::enableAll);
    }
}