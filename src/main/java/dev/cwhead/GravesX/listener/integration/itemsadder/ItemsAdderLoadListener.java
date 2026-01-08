package dev.cwhead.GravesX.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.integration.ItemsAdder;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Enables the GravesX ItemsAdder integration when ItemsAdder finishes loading its data.
 *
 * <p>{@link ItemsAdderLoadDataEvent} may fire asynchronously, so readiness is updated via
 * the GravesX scheduler to ensure it happens safely on the server thread.</p>
 */
public final class ItemsAdderLoadListener implements Listener {

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
    public ItemsAdderLoadListener(Graves plugin, ItemsAdder integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    /**
     * Called when ItemsAdder reports its content data has been (re)loaded.
     * Sets the integration to ready and logs a debug message.
     *
     * @param event ItemsAdder load data event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        plugin.getSchedulerManager().runTask(() ->
                plugin.getSchedulerManager().runTaskLater(() -> {
                    integration.setReady(true);
                    plugin.debugMessage("ItemsAdder content loaded. ItemsAdder integration is now ready.", 1);
                }, 1L)
        );
    }
}