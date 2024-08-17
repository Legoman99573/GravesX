package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

import java.util.List;

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
        BlockFace direction = event.getDirection();

        Block piston = event.getBlock();
        // Get all blocks being moved by the piston
        List<Block> blocks = event.getBlocks();

        BlockFace[] facesToCheck = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

        // Check all faces around the piston
        for (BlockFace face : facesToCheck) {
            Block adjacentBlock = piston.getRelative(face);

            // Check if the adjacent block is part of a grave
            if (plugin.getBlockManager().getGraveFromBlock(adjacentBlock) != null) {
                event.setCancelled(true);
                return;
            }
        }

        // Check each block to see if it's within 12 blocks of a grave or grave hologram
        for (Block block : blocks) {
            Block relativeBlock = block.getRelative(direction);

            // Check if the block being moved is part of a grave
            if (plugin.getBlockManager().getGraveFromBlock(relativeBlock) != null) {
                event.setCancelled(true);
                return;
            }

            // Check for entities (such as holograms) within a 12-block radius around the block
            // double radius = 12.0;
            // for (Entity entity : relativeBlock.getWorld().getNearbyEntities(relativeBlock.getLocation(), radius, radius, radius)) {
            //     if (plugin.getHologramManager().getGrave(entity) != null) {
            //         event.setCancelled(true);
            //         return;
            //     }
            // }
        }
    }
}