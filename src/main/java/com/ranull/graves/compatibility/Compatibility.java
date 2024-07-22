package com.ranull.graves.compatibility;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * An interface to handle compatibility between different server versions and plugins.
 */
public interface Compatibility {

    /**
     * Sets the block data for a given location and material, associating it with a grave.
     *
     * @param location The location where the block data should be set.
     * @param material The material of the block to set.
     * @param grave    The grave associated with the block.
     * @param plugin   The Graves plugin instance.
     * @return The BlockData representing the set block data.
     */
    BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin);

    /**
     * Checks if a player can build at a given location.
     *
     * @param player   The player to check.
     * @param location The location to check.
     * @param plugin   The Graves plugin instance.
     * @return True if the player can build at the location, false otherwise.
     */
    boolean canBuild(Player player, Location location, Graves plugin);

    /**
     * Checks if a block has title data.
     *
     * @param block The block to check.
     * @return True if the block has title data, false otherwise.
     */
    boolean hasTitleData(Block block);

    /**
     * Gets the skull item stack for a given grave.
     *
     * @param grave  The grave associated with the skull.
     * @param plugin The Graves plugin instance.
     * @return The ItemStack representing the skull.
     */
    ItemStack getSkullItemStack(Grave grave, Graves plugin);

    /**
     * Gets the texture of a skull item stack.
     *
     * @param itemStack The item stack representing the skull.
     * @return The texture of the skull as a string.
     */
    String getSkullTexture(ItemStack itemStack);
}
