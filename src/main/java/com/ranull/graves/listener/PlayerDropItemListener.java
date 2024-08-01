package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for handling PlayerDropItemEvent to prevent dropping items associated with graves.
 */
public class PlayerDropItemListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerDropItemListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerDropItemListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerDropItemEvent to remove items from the world if they are associated with graves.
     *
     * If the dropped item is linked to a grave (i.e., it has a grave UUID associated with it),
     * the item drop is cancelled, and the item is removed from the world.
     *
     * @param event The PlayerDropItemEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (isGraveItem(itemStack)) {
            event.getItemDrop().remove();
        }
    }

    /**
     * Checks if the item stack is associated with a grave.
     *
     * @param itemStack The item stack to check.
     * @return True if the item stack is associated with a grave, false otherwise.
     */
    private boolean isGraveItem(ItemStack itemStack) {
        return plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null;
    }
}