package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.StringUtil;
import dev.cwhead.GravesX.event.GraveItemTakeEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Listener for handling InventoryClickEvent to manage grave-related inventory interactions.
 */
public class InventoryClickListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an InventoryClickListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public InventoryClickListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the InventoryClickEvent to perform actions based on the type of inventory holder.
     * Updates grave inventories and handles interactions with GraveList and GraveMenu inventories.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        Inventory top = CompatibilityInventoryView.getTopInventory(event);
        InventoryHolder topHolder = top.getHolder();

        if (clicked.getHolder() instanceof Grave grave) {
            handleGraveInventoryClick(event, player, grave);
        }

        if (topHolder instanceof GraveList graveList) {
            if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
                handleGraveListClick(event, player, graveList, event.getRawSlot());
            }
            event.setCancelled(true);
        } else if (topHolder instanceof GraveMenu graveMenu) {
            if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
                handleGraveMenuClick(event, player, graveMenu, event.getRawSlot());
            }
            event.setCancelled(true);
        }

        ClickType clickType = event.getClick();
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            handleShiftClick(event);
        }
        isCompassItem(event);
    }

    /**
     * Checks if a specific type of compass (e.g., RECOVERY_COMPASS) was clicked in the player's inventory.
     * Also checks if the clicked inventory involves XP-granting inventories like FURNACE, ANVIL, etc.
     *
     * @param event The InventoryClickEvent to check.
     */
    private void isCompassItem(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        ItemStack item = event.getCursor();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) return;

        if (item.getType() == Material.valueOf(String.valueOf(plugin.getVersionManager().getMaterialForVersion("RECOVERY_COMPASS")))) {
            UUID graveUUID = getGraveUUIDFromItemStack(item);
            if (graveUUID != null) {
                Grave grave = plugin.getCacheManager().getGrave(graveUUID);
                if (grave != null) {
                    String compassName = plugin.getIntegrationManager().hasMiniMessage()
                            ? MiniMessage.parseString(StringUtil.parseString("&f" + plugin.getConfigManager().getConfigSection("compass.name", grave).getString("compass.name"), grave, plugin))
                            : StringUtil.parseString("&f" + plugin.getConfigManager().getConfigSection("compass.name", grave).getString("compass.name"), grave, plugin);

                    if (itemMeta.getDisplayName().equals(compassName)) {
                        InventoryType inventoryType = event.getInventory().getType();
                        if (checkIfXPGivingInventory(inventoryType)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle shift-clicking logic, preventing players from shift-clicking the compass into a restricted inventory.
     *
     * @param event The InventoryClickEvent triggered by the player shift-clicking.
     */
    private void handleShiftClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) return;

        if (item.getType() == Material.valueOf(String.valueOf(plugin.getVersionManager().getMaterialForVersion("RECOVERY_COMPASS")))) {
            UUID graveUUID = getGraveUUIDFromItemStack(item);
            if (graveUUID != null) {
                Grave grave = plugin.getCacheManager().getGrave(graveUUID);
                if (grave != null) {
                    String compassName = plugin.getIntegrationManager().hasMiniMessage()
                            ? MiniMessage.parseString(StringUtil.parseString("&f" + plugin.getConfigManager().getConfigSection("compass.name", grave).getString("compass.name"), grave, plugin))
                            : StringUtil.parseString("&f" + plugin.getConfigManager().getConfigSection("compass.name", grave).getString("compass.name"), grave, plugin);

                    if (itemMeta.getDisplayName().equals(compassName)) {
                        InventoryType inventoryType = event.getInventory().getType();
                        if (checkIfXPGivingInventory(inventoryType)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the inventory type is one that grants XP (e.g., Furnace, Anvil, Grindstone).
     *
     * @param inventoryType The type of the inventory.
     * @return true if the inventory grants XP, false otherwise.
     */
    private boolean checkIfXPGivingInventory(InventoryType inventoryType) {
        return switch (inventoryType.name()) {
            case "FURNACE", "BLAST_FURNACE", "SMOKER", "ANVIL", "GRINDSTONE", "HOPPER" -> true;
            default -> false;
        };
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

    /**
     * Handles inventory clicks when the inventory holder is a Grave.
     *
     * @param event  The InventoryClickEvent.
     * @param player The Player clicking.
     * @param grave  The Grave inventory holder.
     */
    private void handleGraveInventoryClick(InventoryClickEvent event, Player player, Grave grave) {
        if (grave.getGravePreview()) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getEntityManager().canOpenGrave(player, grave)) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        InventoryAction action = event.getAction();
        Inventory topInventory = CompatibilityInventoryView.getTopInventory(event); // grave inv
        Inventory bottomInventory = CompatibilityInventoryView.getBottomInventory(event); // player inv
        Grave.StorageMode storageMode = plugin.getGraveManager().getStorageMode(plugin.getConfigManager().getConfigSection("storage.mode", grave).getString("storage.mode"));

        if (clickedInventory == null) return;

        if (storageMode == Grave.StorageMode.EXACT) {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory.equals(bottomInventory)) {
                event.setCancelled(true);
                return;
            }

            if (clickedInventory.equals(grave.getInventory())) {
                if (action == InventoryAction.PLACE_ALL
                        || action == InventoryAction.PLACE_SOME
                        || action == InventoryAction.PLACE_ONE
                        || action == InventoryAction.SWAP_WITH_CURSOR) {
                    event.setCancelled(true);
                    return;
                }

                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    GraveItemTakeEvent takeEvent = new GraveItemTakeEvent(grave, player, clickedItem, action);
                    plugin.getServer().getPluginManager().callEvent(takeEvent);

                    if (takeEvent.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }

                    plugin.getSchedulerManager().runTaskLater(() ->
                            plugin.getDataManager().updateGrave(grave, "inventory",
                                    InventoryUtil.inventoryToString(grave.getInventory(), plugin)), 1L);
                }
            }
        }
    }

    /**
     * Handles inventory clicks for GraveList inventories.
     *
     * @param event     The InventoryClickEvent.
     * @param player    The player interacting with the inventory.
     * @param graveList The GraveList inventory holder.
     */
    private void handleGraveListClick(InventoryClickEvent event, Player player, GraveList graveList, int slot) {
        Grave grave = graveList.getGrave(slot);
        if (grave != null) {
            if (event.getClick() == ClickType.SHIFT_LEFT) {
                event.setCancelled(true);
                return;
            }

            plugin.getEntityManager().runFunction(player, plugin.getConfigManager().getConfigSection("gui.menu.list.function", grave).getString("gui.menu.list.function", "menu"), grave);
            plugin.getGUIManager().setGraveListItems(graveList.getInventory(), graveList.getUUID());
        }

        event.setCancelled(true);
    }

    /**
     * Handles inventory clicks for GraveMenu inventories.
     *
     * @param event     The InventoryClickEvent.
     * @param player    The player interacting with the inventory.
     * @param graveMenu The GraveMenu inventory holder.
     */
    private void handleGraveMenuClick(InventoryClickEvent event, Player player, GraveMenu graveMenu, int slot) {
        Grave grave = graveMenu.getGrave();
        if (grave == null) {
            event.getWhoClicked().closeInventory();
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfigManager()
                .getConfigSection("gui.menu.grave.slot." + slot + ".enabled", grave)
                .getBoolean("gui.menu.grave.slot." + slot + ".enabled")) {

            plugin.getEntityManager().runFunction(
                    player,
                    plugin.getConfigManager()
                            .getConfigSection("gui.menu.grave.slot." + slot + ".function", grave)
                            .getString("gui.menu.grave.slot." + slot + ".function", "none"),
                    grave
            );

            plugin.getGUIManager().setGraveMenuItems(graveMenu.getInventory(), grave);
        }

        event.setCancelled(true);
    }
}
