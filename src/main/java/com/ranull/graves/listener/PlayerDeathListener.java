package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Listener for handling PlayerDeathEvent to manage items dropped upon player death.
 * Specifically, it handles compass items based on their association with graves.
 */
public class PlayerDeathListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerDeathListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerDeathEvent to remove compass items from the drop list if they are associated with graves.
     *
     * If a compass item is linked to a grave and the configuration setting "compass.destroy" is true,
     * the item is removed from the drop list. The remaining items are then cached for later reference.
     *
     * @param event The PlayerDeathEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        List<ItemStack> itemStackList = event.getDrops();
        List<ItemStack> itemsToRemove = new ArrayList<>(); // To prevent ConcurrentModificationException

        for (ItemStack itemStack : itemStackList) {
            if (itemStack != null && itemStack.getType() == Material.COMPASS) {
                // Check if the item stack is associated with a grave
                if (plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                        && plugin.getConfig("compass.destroy", event.getEntity()).getBoolean("compass.destroy")) {
                    itemsToRemove.add(itemStack);
                }
            }
        }

        // Remove the identified items from the drop list
        itemStackList.removeAll(itemsToRemove);
        // Cache the remaining item stacks
        plugin.getCacheManager().getRemovedItemStackMap()
                .put(event.getEntity().getUniqueId(), new ArrayList<>(itemStackList));
    }
}