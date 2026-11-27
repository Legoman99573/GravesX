package dev.cwhead.GravesX.module.listener;

import dev.cwhead.GravesX.module.ModuleManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Objects;

/**
 * Listens for plugin enable events and asks the {@link ModuleManager} to
 * retry enabling modules that were waiting on dependencies.
 */
public final class DependencyEnableListener implements Listener {
    private final ModuleManager manager;

    /**
     * Creates a listener tied to a module manager.
     *
     * @param manager Module manager to notify when plugins are enabled.
     */
    public DependencyEnableListener(ModuleManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    /**
     * Handles a plugin enable event and retries enabling pending modules.
     *
     * @param e The plugin enable event.
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        manager.tryEnablePending();
    }
}
