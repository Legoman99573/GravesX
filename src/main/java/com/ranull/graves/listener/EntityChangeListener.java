package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Listener for handling EntityChangeBlockEvent to prevent any entities from interacting with blocks
 * in specific areas, such as picking up or placing blocks near graves.
 */
public class EntityChangeListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new EntityChangeListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public EntityChangeListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the EntityChangeBlockEvent to prevent any entity from picking up or placing blocks in a grave area.
     *
     * @param event The EntityChangeBlockEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();

        // Check if the block is within the radius of a grave
        if (isNearGrave(block.getLocation(), block)) {
            event.setCancelled(true); // Prevent the entity from changing the block
        }
    }

    /**
     * Checks if the given location is within 15 blocks of any grave.
     *
     * @param location The location to check.
     * @return True if the location is within 15 blocks of any grave, false otherwise.
     */
    private boolean isNearGrave(Location location, Block block) {
        try {
            for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
                Location graveLocation = plugin.getGraveManager().getGraveLocation(block.getLocation(), grave);
                if (graveLocation != null) {
                    double distance = location.distance(graveLocation);
                    if (plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius") != 0 && distance <= plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius")) {
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