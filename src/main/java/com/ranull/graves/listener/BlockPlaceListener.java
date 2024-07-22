package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listens for BlockPlaceEvent to prevent placing blocks in certain conditions.
 */
public class BlockPlaceListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockPlaceListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockPlaceListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockPlaceEvent to prevent placing blocks if they are graves or if the item being used is a token.
     *
     * @param event The BlockPlaceEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Get the block being placed
        Block block = event.getBlock();
        // Check if the block being placed is a grave
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        // If the block is a grave or if the item being used is a token, cancel the event
        if (grave != null || (plugin.getRecipeManager() != null
                && plugin.getRecipeManager().isToken(event.getItemInHand()))) {
            event.setCancelled(true);
        }
    }
}