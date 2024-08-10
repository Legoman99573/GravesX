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
        Block block = event.getBlock().getRelative(event.getDirection());

        // Check if the block being moved is part of a grave
        if (plugin.getBlockManager().getGraveFromBlock(block) != null) {
            event.setCancelled(true);
        } else {
            // Check for entities within a 12-block radius around the block
            double radius = 12.0;
            for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), radius, radius, radius)) {
                if (plugin.getHologramManager().getGrave(entity) != null) {
                    event.setCancelled(true);
                    break; // No need to continue checking if we've found a grave hologram
                }
            }
        }
    }
}