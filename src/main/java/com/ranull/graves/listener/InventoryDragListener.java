package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for handling InventoryClickEvent to manage interactions with grave inventories
 * and prevent actions in grave lists.
 */
public class InventoryDragListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an InventoryDragListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public InventoryDragListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the InventoryClickEvent to update grave inventories and manage grave lists.
     * Updates the grave inventory in the database when a Grave inventory is interacted with,
     * and cancels the event if interacting with a GraveList inventory.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();

        if (inventoryHolder instanceof Grave) {
            Grave grave = (Grave) inventoryHolder;

            // Update the grave inventory in the database after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    plugin.getDataManager().updateGrave(grave, "inventory",
                            InventoryUtil.inventoryToString(grave.getInventory())), 1L);
        } else if (inventoryHolder instanceof GraveList) {
            // Cancel interaction with GraveList inventories
            event.setCancelled(true);
        }
    }
}