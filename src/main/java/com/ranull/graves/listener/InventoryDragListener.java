package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import dev.cwhead.GravesX.event.GraveItemTakeEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
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

        if (inventoryHolder == null) return;

        if (inventoryHolder instanceof Grave grave) {
            handleGraveInventoryDrag(event, player, grave);
        } else if (event.getWhoClicked() instanceof Player) {
            handlePlayerInventoryDrag(event, player, inventoryHolder);
        }

        // Guard against dragging the Graves compass into XP-giving inventories
        checkCompassDrag(event);
    }

    /**
     * Checks if a specific type of compass (e.g., RECOVERY_COMPASS) was dragged into the player's inventory.
     */
    private void checkCompassDrag(InventoryDragEvent event) {
        Map<Integer, ItemStack> newItems = event.getNewItems();
        for (ItemStack item : newItems.values()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;

            try {
                if (item.getType() == plugin.getVersionManager().getMaterialForVersion("RECOVERY_COMPASS")) {
                    UUID graveUUID = getGraveUUIDFromItemStack(item);
                    if (graveUUID == null) continue;

                    Grave grave = plugin.getCacheManager().getGraveMap().get(graveUUID);
                    if (grave == null) continue;

                    String configured = plugin.getConfig("compass.name", grave).getString("compass.name");
                    String parsedLegacy = StringUtil.parseString("&f" + configured, grave, plugin);
                    String expectedName = plugin.getIntegrationManager().hasMiniMessage()
                            ? MiniMessage.parseString(parsedLegacy)
                            : parsedLegacy;

                    if (meta.getDisplayName().equals(expectedName)) {
                        InventoryType type = event.getInventory().getType();
                        if (isXpGivingInventory(type)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Be defensive across MC versions/material availability
            }
        }
    }

    /**
     * Handles inventory drags when the inventory holder is a Grave.
     */
    private void handleGraveInventoryDrag(InventoryDragEvent event, Player player, Grave grave) {
        if (!grave.getGravePreview() && plugin.getEntityManager().canOpenGrave(player, grave)) {
            Map<Integer, ItemStack> draggedItems = event.getNewItems();
            InventoryAction action = InventoryAction.MOVE_TO_OTHER_INVENTORY;

            for (ItemStack item : draggedItems.values()) {
                if (item == null) continue;

                GraveItemTakeEvent takeEvent = new GraveItemTakeEvent(grave, player, item, action);
                plugin.getServer().getPluginManager().callEvent(takeEvent);

                if (takeEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }
    }

    /**
     * Handles inventory drags when the player interacts with GraveList or GraveMenu inventories.
     */
    private void handlePlayerInventoryDrag(InventoryDragEvent event, Player player, InventoryHolder holder) {
        if (holder instanceof GraveList list) {
            handleGraveListDrag(event, player, list);
        } else if (holder instanceof GraveMenu menu) {
            handleGraveMenuDrag(event, player, menu);
        }
    }

    /**
     * @return true if the inventory type grants XP (e.g., Furnace, Anvil, Grindstone).
     */
    private boolean isXpGivingInventory(InventoryType type) {
        return switch (type) {
            case FURNACE, BLAST_FURNACE, SMOKER, ANVIL, GRINDSTONE, HOPPER -> true;
            default -> false;
        };
    }

    /**
     * Handles inventory drags for GraveList inventories.
     */
    private void handleGraveListDrag(InventoryDragEvent event, Player player, GraveList graveList) {
        // Prevent dragging items in or out of GraveList
        event.setCancelled(true);
    }

    /**
     * Handles inventory drags for GraveMenu inventories.
     */
    private void handleGraveMenuDrag(InventoryDragEvent event, Player player, GraveMenu graveMenu) {
        try {
            Grave grave = graveMenu.getGrave();
            if (grave != null) {
                // Prevent dragging items into restricted slots in GraveMenu
                event.setCancelled(true);
            }
        } catch (NullPointerException | IllegalArgumentException ignored) {
            // Likely grave doesn't exist. Close to be safe.
            event.getWhoClicked().closeInventory();
            event.setCancelled(true);
        }
    }

    /**
     * Retrieves the Grave UUID from the item stack.
     *
     * @return The UUID of the grave associated with the item stack, or null if not found.
     */
    private UUID getGraveUUIDFromItemStack(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        String uuidString = meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING);
        return uuidString != null ? UUID.fromString(uuidString) : null;
    }
}