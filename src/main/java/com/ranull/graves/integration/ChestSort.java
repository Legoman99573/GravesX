package com.ranull.graves.integration;

import de.jeff_media.chestsort.api.ChestSortAPI;
import org.bukkit.inventory.Inventory;

/**
 * Provides integration with the ChestSort plugin to sort inventories.
 */
public final class ChestSort {

    /**
     * Sorts the items in the provided inventory using ChestSortAPI.
     *
     * @param inventory The Inventory object to be sorted.
     */
    public void sortInventory(Inventory inventory) {
        ChestSortAPI.sortInventory(inventory);
    }
}