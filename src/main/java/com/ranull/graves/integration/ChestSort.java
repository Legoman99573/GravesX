package com.ranull.graves.integration;

import dev.cwhead.GravesX.util.pluginsthatgoabandonedandtheirlicenseiscrapsoreflectionitis.ChestSortAPI;
import org.bukkit.inventory.Inventory;

/**
 * @deprecated Since 4.9.9.1 — Unmaintained by author. Read here: https://www.spigotmc.org/profile-posts/239137/
 * Provides integration with the ChestSort plugin to sort inventories.
 */
@Deprecated(since = "4.9.9.1")
public class ChestSort {

    /**
     * @deprecated Since 4.9.9.1 — Unmaintained by author. Read here: https://www.spigotmc.org/profile-posts/239137/
     * Sorts the items in the provided inventory using ChestSortAPI.
     *
     * @param inventory The Inventory object to be sorted.
     */
    @Deprecated(since = "4.9.9.1")
    public void sortInventory(Inventory inventory) {
        ChestSortAPI.sortInventory(inventory);
    }
}