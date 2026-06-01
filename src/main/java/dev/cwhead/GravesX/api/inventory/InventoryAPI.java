package dev.cwhead.GravesX.api.inventory;

import com.ranull.graves.Graves;
import com.ranull.graves.util.InventoryUtil;
import dev.cwhead.GravesX.api.util.UtilAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Inventory helper API.
 */
public class InventoryAPI {
    private final Graves plugin;
    private final UtilAPI util;

    public InventoryAPI(Graves plugin, UtilAPI util) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.util = Objects.requireNonNull(util, "util");
    }

    /**
     * Gets the appropriate inventory size based on the given size.
     *
     * @param size The size to be used for determining the inventory size.
     * @return The appropriate inventory size.
     */
    public int getInventorySize(int size) {
        return InventoryUtil.getInventorySize(size);
    }

    /**
     * Equips the player's armor from the given inventory.
     *
     * @param inventory The inventory containing the armor items.
     * @param player    The player to be equipped with armor.
     */
    public void equipArmor(@NotNull Inventory inventory, @NotNull Player player) {
        InventoryUtil.equipArmor(inventory, player);
    }

    /**
     * Equips the player's inventory items from the given inventory.
     *
     * @param inventory The inventory containing the items.
     * @param player    The player to be equipped with items.
     */
    public void equipItems(@NotNull Inventory inventory, @NotNull Player player) {
        InventoryUtil.equipItems(inventory, player);
    }

    /**
     * Converts the given inventory to a string representation.
     *
     * @param inventory The inventory to be converted.
     * @return The string representation of the inventory.
     */
    public String inventoryToString(@NotNull Inventory inventory) {
        return InventoryUtil.inventoryToString(inventory, plugin);
    }

    /**
     * Converts a string representation of an inventory to an Inventory object.
     *
     * @param inventoryHolder The inventory holder.
     * @param string          The string representation of the inventory.
     * @param title           The title of the inventory.
     * @return The Inventory object.
     */
    public Inventory stringToInventory(@NotNull InventoryHolder inventoryHolder, @NotNull String string, @NotNull String title) {
        return InventoryUtil.stringToInventory(inventoryHolder, string, title, plugin);
    }
}
