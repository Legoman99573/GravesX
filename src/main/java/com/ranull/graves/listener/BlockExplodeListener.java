package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveExplodeEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Listens for BlockExplodeEvent to handle interactions with grave blocks when they are affected by block explosions.
 */
public class BlockExplodeListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockExplodeListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockExplodeListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockExplodeEvent to manage grave interactions when blocks are exploded by other blocks.
     *
     * @param event The BlockExplodeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> affectedBlocks = event.blockList();
        boolean cancelEvent = false;

        // Temporary list to store blocks that need to be removed
        List<Block> blocksToRemove = new ArrayList<>();

        // Check if any affected blocks are within the protection radius
        for (Block block : affectedBlocks) {
            Location blockLocation = block.getLocation();
            Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

            if (grave != null) {
                Location graveLocation = plugin.getGraveManager().getGraveLocation(blockLocation, grave);
                if (graveLocation != null) {
                    double distance = blockLocation.distance(graveLocation);
                    int protectionRadius = plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius");
                    if (protectionRadius != 0 && distance <= protectionRadius) {
                        blocksToRemove.add(block); // Add blocks within protection radius to the list
                    }
                }
            }
        }

        if (!blocksToRemove.isEmpty()) {
            cancelEvent = true;
            affectedBlocks.removeAll(blocksToRemove); // Remove protected blocks from the affected blocks list
        }

        if (cancelEvent) {
            event.setCancelled(true);
        } else {
            // Handle blocks that are not within the protection radius
            Iterator<Block> iterator = affectedBlocks.iterator();
            while (iterator.hasNext()) {
                Block block = iterator.next();
                Location blockLocation = block.getLocation();

                Grave grave = plugin.getBlockManager().getGraveFromBlock(block);
                if (grave != null) {
                    if (shouldExplode(grave)) {
                        handleGraveExplosion(event, iterator, block, grave, blockLocation);
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * Checks if the grave should explode based on the configuration.
     *
     * @param grave The grave to check.
     * @return True if the grave should explode, false otherwise.
     */
    private boolean shouldExplode(Grave grave) {
        return plugin.getConfig("grave.explode", grave).getBoolean("grave.explode");
    }

    /**
     * Handles the explosion of a grave.
     *
     * @param event     The BlockExplodeEvent.
     * @param iterator  The iterator for the blocks in the explosion.
     * @param block     The block that exploded.
     * @param grave     The grave associated with the block.
     * @param location  The location of the grave.
     */
    private void handleGraveExplosion(BlockExplodeEvent event, Iterator<Block> iterator, Block block, Grave grave, Location location) {
        GraveExplodeEvent graveExplodeEvent = new GraveExplodeEvent(location, null, grave);
        plugin.getServer().getPluginManager().callEvent(graveExplodeEvent);

        if (!graveExplodeEvent.isCancelled()) {
            if (plugin.getConfig("drop.explode", grave).getBoolean("drop.explode")) {
                plugin.getGraveManager().breakGrave(location, grave);
            } else {
                plugin.getGraveManager().removeGrave(grave);
            }

            plugin.getGraveManager().closeGrave(grave);
            plugin.getGraveManager().playEffect("effect.loot", location, grave);
            plugin.getEntityManager().runCommands("event.command.explode", block.getType().name(), location, grave);

            // Assuming you have a similar zombie spawning mechanism for BlockExplodeEvent if needed
            if (plugin.getConfig("zombie.explode", grave).getBoolean("zombie.explode")) {
                plugin.getEntityManager().spawnZombie(location, grave);
            }
        } else {
            iterator.remove();
        }
    }
}