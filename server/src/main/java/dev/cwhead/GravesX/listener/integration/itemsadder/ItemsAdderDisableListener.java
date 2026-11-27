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

    /**
     * Creates the listener.
     *
     * @param plugin GravesX plugin instance
     */
    public ItemsAdderDisableListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Detects when ItemsAdder is disabled and marks the integration as not ready.
     *
     * @param event plugin disable event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) {
        if (!event.getPlugin().getName().equalsIgnoreCase("ItemsAdder")) return;

        plugin.getIntegrationManager().getItemsAdder().setReady(false);
        plugin.debugMessage("ItemsAdder disabled. Integration is now gated.", 1);
    }
}