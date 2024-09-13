package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

/**
 * Listener for handling InventoryDragEvent to manage grave-related inventory interactions.
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
     * Handles the InventoryDragEvent to perform actions based on the type of inventory holder.
     * Updates grave inventories and handles interactions with GraveList and GraveMenu inventories.
     *
     * @param event The InventoryDragEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        if (inventoryHolder != null) {
            if (inventoryHolder instanceof Grave) {
                handleGraveInventoryDrag(event, player, (Grave) inventoryHolder);
            } else if (event.getWhoClicked() instanceof Player) {
                handlePlayerInventoryDrag(event, player, inventoryHolder);
            }
            isCompassItem(event);
        }
    }

    /**
     * Checks if a specific type of compass (e.g., RECOVERY_COMPASS) was dragged into the player's inventory.
     *
     * @param event The InventoryDragEvent to check.
     */
    private void isCompassItem(InventoryDragEvent event) {
        Map<Integer, ItemStack> newItems = event.getNewItems();
        for (ItemStack item : newItems.values()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null || !itemMeta.hasDisplayName()) continue;

            if (item.getType() == Material.valueOf(String.valueOf(plugin.getVersionManager().getMaterialForVersion("RECOVERY_COMPASS")))) {
                UUID graveUUID = getGraveUUIDFromItemStack(item);
                if (graveUUID != null) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(graveUUID);
                    if (grave != null) {

                        String compassName;
                        if (plugin.getIntegrationManager().hasMiniMessage()) {
                            String compassNameNew = StringUtil.parseString("&f" + plugin
                                    .getConfig("compass.name", grave).getString("compass.name"), grave, plugin);
                            compassName = MiniMessage.parseString(compassNameNew);
                        } else {
                            compassName = StringUtil.parseString("&f" + plugin
                                    .getConfig("compass.name", grave).getString("compass.name"), grave, plugin);
                        }

                        if (itemMeta.getDisplayName().equals(compassName)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles inventory drags when the inventory holder is a Grave.
     *
     * @param event  The InventoryDragEvent.
     * @param player The player dragging items.
     * @param grave  The Grave inventory holder.
     */
    private void handleGraveInventoryDrag(InventoryDragEvent event, Player player, Grave grave) {
        if (plugin.getEntityManager().canOpenGrave(player, grave)) {
            // Schedule a task to update the grave's inventory in the data manager
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    plugin.getDataManager().updateGrave(grave, "inventory",
                            InventoryUtil.inventoryToString(grave.getInventory())), 1L);
        } else {
            event.setCancelled(true);
        }
    }

    /**
     * Handles inventory drags when the player interacts with GraveList or GraveMenu inventories.
     *
     * @param event           The InventoryDragEvent.
     * @param player          The player dragging items.
     * @param inventoryHolder The inventory holder.
     */
    private void handlePlayerInventoryDrag(InventoryDragEvent event, Player player, InventoryHolder inventoryHolder) {
        if (inventoryHolder instanceof GraveList) {
            handleGraveListDrag(event, player, (GraveList) inventoryHolder);
        } else if (inventoryHolder instanceof GraveMenu) {
            handleGraveMenuDrag(event, player, (GraveMenu) inventoryHolder);
        }
    }

    /**
     * Handles inventory drags for GraveList inventories.
     *
     * @param event     The InventoryDragEvent.
     * @param player    The player dragging items.
     * @param graveList The GraveList inventory holder.
     */
    private void handleGraveListDrag(InventoryDragEvent event, Player player, GraveList graveList) {
        // Prevent dragging items in or out of GraveList
        event.setCancelled(true);
    }

    /**
     * Handles inventory drags for GraveMenu inventories.
     *
     * @param event     The InventoryDragEvent.
     * @param player    The player dragging items.
     * @param graveMenu The GraveMenu inventory holder.
     */
    private void handleGraveMenuDrag(InventoryDragEvent event, Player player, GraveMenu graveMenu) {
        Grave grave = graveMenu.getGrave();
        try {
            if (grave != null) {
                // Prevent dragging items into restricted slots in GraveMenu
                event.setCancelled(true);
            }
        } catch (NullPointerException | IllegalArgumentException ignored) {
            // Likely grave doesn't exist. Ignore this.
            event.getWhoClicked().closeInventory();
            event.setCancelled(true);
        }
    }

    /**
     * Retrieves the Grave UUID from the item stack.
     *
     * @param itemStack The item stack to check.
     * @return The UUID of the grave associated with the item stack, or null if not found.
     */
    private UUID getGraveUUIDFromItemStack(ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            if (itemStack.getItemMeta() == null) return null;
            String uuidString = itemStack.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING);
            return uuidString != null ? UUID.fromString(uuidString) : null;
        }
        return null;
    }
}
