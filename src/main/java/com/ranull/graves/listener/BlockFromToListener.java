package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

/**
 * Listens for BlockFromToEvent to prevent water or lava from flowing over grave blocks.
 */
public class BlockFromToListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockFromToListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockFromToListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockFromToEvent to prevent fluid from flowing into grave blocks.
     *
     * @param event The BlockFromToEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        // Check if the destination block of the fluid is a grave
        if (isGraveBlock(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the destination block of the fluid is a grave block.
     *
     * @param event The BlockFromToEvent to check.
     * @return True if the destination block is a grave block, false otherwise.
     */
    private boolean isGraveBlock(BlockFromToEvent event) {
        return plugin.getBlockManager().getGraveFromBlock(event.getToBlock()) != null || plugin.getBlockManager().getGraveFromBlock(event.getToBlock().getLocation().add(0,1,0).getBlock()) != null;
    }
}