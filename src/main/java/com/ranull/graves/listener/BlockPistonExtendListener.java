package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GravePistonExtendEvent;
import com.ranull.graves.type.Grave;
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
        List<Block> blocks = event.getBlocks();
        BlockFace[] facesToCheck = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

        // Check all faces around the piston
        for (BlockFace face : facesToCheck) {
            Block adjacentBlock = piston.getRelative(face);
            Grave grave = plugin.getBlockManager().getGraveFromBlock(adjacentBlock);
            if (grave != null) {
                if (plugin.getConfig("drop.piston", grave).getBoolean("drop.piston")) {
                    // Fire the GravePistonExtendEvent
                    GravePistonExtendEvent gravePistonEvent = new GravePistonExtendEvent(grave, piston.getLocation(), piston, direction, blocks);

                    plugin.getServer().getPluginManager().callEvent(gravePistonEvent);

                    if (gravePistonEvent.isCancelled() || gravePistonEvent.isAddon()) {
                        event.setCancelled(true);
                    } else {
                        plugin.getGraveManager().breakGrave(grave.getLocationDeath(), grave);
                        plugin.getGraveManager().closeGrave(grave);
                        plugin.getGraveManager().playEffect("effect.loot", piston.getLocation(), grave);
                        plugin.getEntityManager().runCommands("event.command.pistonextend", piston.getType().name(), piston.getLocation(), grave);
                    }
                } else {
                    event.setCancelled(true);
                }
                return;
            }
        }
    }
}