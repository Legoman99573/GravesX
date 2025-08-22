package com.ranull.graves.integration;

import com.ranull.graves.util.pluginsthatgoabandonedandtheirlicenseiscrapsoreflectionitis.ChestSortAPI;
import org.bukkit.inventory.Inventory;

/**
 * @deprecated Unmaintained by Author. Read here: https://www.spigotmc.org/profile-posts/239137/
 * Provides integration with the ChestSort plugin to sort inventories.
 */
@Deprecated
public final class ChestSort {

    /**
     * @deprecated Unmaintained by Author. Read here: https://www.spigotmc.org/profile-posts/239137/
     * Sorts the items in the provided inventory using ChestSortAPI.
     *
     * @param inventory The Inventory object to be sorted.
     */
    @Deprecated
    public void sortInventory(Inventory inventory) {
        ChestSortAPI.sortInventory(inventory);
    }
}