package dev.cwhead.GravesX.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.integration.ItemsAdder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

/**
 * Gates the GravesX ItemsAdder integration when ItemsAdder is disabled.
 */
public final class ItemsAdderDisableListener implements Listener {

    /** GravesX plugin instance. */
    private final Graves plugin;

    /** ItemsAdder integration wrapper. */
    private final ItemsAdder integration;

    /**
     * Creates the listener.
     *
     * @param plugin GravesX plugin instance
     * @param integration ItemsAdder integration wrapper
     */
    public ItemsAdderDisableListener(Graves plugin, ItemsAdder integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    /**
     * Detects when ItemsAdder is disabled and marks the integration as not ready.
     *
     * @param event plugin disable event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) {
        if (!event.getPlugin().getName().equalsIgnoreCase("ItemsAdder")) return;

        integration.setReady(false);
        plugin.debugMessage("ItemsAdder disabled. Integration is now gated.", 1);
    }
}