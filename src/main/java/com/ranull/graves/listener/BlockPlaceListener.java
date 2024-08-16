package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();
        // Check if the block being placed is a grave
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        // If the block is a grave or if the item being used is a token, cancel the event
        if (isGraveBlock(grave) || isTokenItem(event)) {
            event.setCancelled(true);
        } else if (isNearGrave(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "You can't place blocks near a grave site.");
        }
    }

    /**
     * Checks if the block is a grave block.
     *
     * @param grave The grave to check.
     * @return True if the block is a grave, false otherwise.
     */
    private boolean isGraveBlock(Grave grave) {
        return grave != null;
    }

    /**
     * Checks if the item being used to place the block is a token.
     *
     * @param event The BlockPlaceEvent.
     * @return True if the item is a token, false otherwise.
     */
    private boolean isTokenItem(BlockPlaceEvent event) {
        return plugin.getRecipeManager() != null && plugin.getRecipeManager().isToken(event.getItemInHand());
    }

    /**
     * Checks if the given location is within 15 blocks of any grave.
     *
     * @param location The location to check.
     * @return True if the location is within 15 blocks of any grave, false otherwise.
     */
    private boolean isNearGrave(Location location) {
        try {
            for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
                Location graveLocation = plugin.getGraveManager().getGraveLocation(location, grave);
                if (graveLocation != null) {
                    double distance = location.distance(graveLocation);
                    if (distance <= plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius")) {
                        return true;
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Assuming grave is in another world
        }
        return false;
    }
}