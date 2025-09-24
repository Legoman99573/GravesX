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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> toRemove = new ArrayList<>();

        for (ItemStack stack : drops) {
            if (isCompassToRemove(event, stack)) {
                toRemove.add(stack);
            }
        }

        drops.removeAll(toRemove);
        cacheRemainingItems(event, drops);
    }

    /**
     * Checks if the given item stack is a compass that should be removed based on the plugin configuration.
     *
     * @param event     The PlayerDeathEvent.
     * @param itemStack The item stack to check.
     * @return True if the item stack is a compass that should be removed, false otherwise.
     */
    private boolean isCompassToRemove(PlayerDeathEvent event, ItemStack itemStack) {
        if (itemStack == null) return false;

        // Support across MC versions: COMPASS (1.8+) and RECOVERY_COMPASS (1.19+)
        String typeName = itemStack.getType().name(); // already upper-case
        boolean isAnyCompass = typeName.contains("COMPASS");

        return isAnyCompass
                && plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                && plugin.getConfig("compass.destroy", event.getEntity()).getBoolean("compass.destroy");
    }

    /**
     * Caches the remaining items from the drop list for later reference.
     *
     * @param event The PlayerDeathEvent.
     * @param items The list of remaining item stacks.
     */
    private void cacheRemainingItems(PlayerDeathEvent event, List<ItemStack> items) {
        plugin.getCacheManager()
                .getRemovedItemStackMap()
                .put(event.getEntity().getUniqueId(), new ArrayList<>(items));
    }
}