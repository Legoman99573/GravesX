package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for handling InventoryOpenEvent to manage interactions with grave inventories.
 */
public class InventoryOpenListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an InventoryOpenListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public InventoryOpenListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the InventoryOpenEvent to manage interactions with grave inventories.
     * Creates and triggers a GraveOpenEvent when a Grave inventory is opened,
     * and cancels the open event if the GraveOpenEvent is cancelled.
     *
     * @param event The InventoryOpenEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isGraveInventory(event)) return;
        handleGraveInventoryOpen(event);
    }

    /**
     * Checks if the inventory holder is a Grave.
     *
     * @param event The InventoryOpenEvent.
     * @return True if the inventory holder is a Grave, false otherwise.
     */
    private boolean isGraveInventory(InventoryOpenEvent event) {
        return event.getInventory().getHolder() instanceof Grave;
    }

    /**
     * Handles the opening of a Grave inventory by creating and triggering a GraveOpenEvent.
     *
     * @param event The InventoryOpenEvent.
     */
    private void handleGraveInventoryOpen(InventoryOpenEvent event) {
        Inventory topInventory = CompatibilityInventoryView.getTopInventory(event);
        InventoryHolder holder = topInventory.getHolder();

        if (!(holder instanceof Grave grave) || !(event.getPlayer() instanceof Player player)) return;

        GraveOpenEvent modern =
                new GraveOpenEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveOpenEvent legacy =
                new com.ranull.graves.event.GraveOpenEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(legacy);

        boolean cancelled = modern.isCancelled() || legacy.isCancelled();
        boolean addon = modern.isAddon() || legacy.isAddon();

        if (cancelled && !addon) {
            event.setCancelled(true);
        }
    }
}