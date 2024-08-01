package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<ItemStack> itemStackList = event.getDrops();
        List<ItemStack> itemsToRemove = new ArrayList<>();

        for (ItemStack itemStack : itemStackList) {
            if (isCompassToRemove(event, itemStack)) {
                itemsToRemove.add(itemStack);
            }
        }

        itemStackList.removeAll(itemsToRemove);
        cacheRemainingItems(event, itemStackList);
    }

    /**
     * Checks if the given item stack is a compass that should be removed based on the plugin configuration.
     *
     * @param event The PlayerDeathEvent.
     * @param itemStack The item stack to check.
     * @return True if the item stack is a compass that should be removed, false otherwise.
     */
    private boolean isCompassToRemove(PlayerDeathEvent event, ItemStack itemStack) {
        return itemStack != null && itemStack.getType().toString().toLowerCase().contains("compass")
                && plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                && plugin.getConfig("compass.destroy", event.getEntity()).getBoolean("compass.destroy");
    }

    /**
     * Caches the remaining items from the drop list for later reference.
     *
     * @param event The PlayerDeathEvent.
     * @param itemStackList The list of remaining item stacks.
     */
    private void cacheRemainingItems(PlayerDeathEvent event, List<ItemStack> itemStackList) {
        plugin.getCacheManager().getRemovedItemStackMap()
                .put(event.getEntity().getUniqueId(), new ArrayList<>(itemStackList));
    }
}