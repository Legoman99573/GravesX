package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

/**
 * Listens for BlockPistonExtendEvent to prevent pistons from moving blocks that are graves or are near holograms of graves.
 */
public class BlockPistonExtendListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockPistonExtendListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockPistonExtendListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockPistonExtendEvent to prevent pistons from extending if they are moving a grave block or a block near a grave hologram.
     *
     * @param event The BlockPistonExtendEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        // Get the block that will be moved by the piston
        Block block = event.getBlock().getRelative(event.getDirection());

        // Check if the block being moved is a grave
        if (isGraveBlock(block)) {
            event.setCancelled(true);
        } else {
            // Check if any nearby entity is a hologram of a grave
            if (isNearGraveHologram(block)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Checks if the block is a grave block.
     *
     * @param block The block to check.
     * @return True if the block is a grave block, false otherwise.
     */
    private boolean isGraveBlock(Block block) {
        return plugin.getBlockManager().getGraveFromBlock(block) != null;
    }

    /**
     * Checks if the block is near a grave hologram.
     *
     * @param block The block to check.
     * @return True if the block is near a grave hologram, false otherwise.
     */
    private boolean isNearGraveHologram(Block block) {
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 0.5, 0.5, 0.5)) {
            if (plugin.getHologramManager().getGrave(entity) != null) {
                return true;
            }
        }
        return false;
    }
}