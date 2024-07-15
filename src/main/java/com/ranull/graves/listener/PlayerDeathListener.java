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

public class PlayerDeathListener implements Listener {
    private final Graves plugin;

    public PlayerDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    //remove ", ignoreCancelled = true" if 3rd party plugins support breaks
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        List<ItemStack> itemStackList = event.getDrops();
        List<ItemStack> itemsToRemove = new ArrayList<>(); // To prevent ConcurrentModificationException from occuring

        for (ItemStack itemStack : itemStackList) {
            ItemStack first = itemStackList.get(0);
            if (first != null && first.getType().toString().toLowerCase().contains("compass")) {
                if (plugin.getEntityManager().getGraveUUIDFromItemStack(first) != null
                        && plugin.getConfig("compass.destroy", event.getEntity()).getBoolean("compass.destroy")) {
                    itemsToRemove.add(first);
                }
            } else {
                if (itemStack != null && itemStack.getType().toString().toLowerCase().contains("compass")) {
                    if (plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                            && plugin.getConfig("compass.destroy", event.getEntity()).getBoolean("compass.destroy")) {
                        itemsToRemove.add(itemStack);
                    }
                }
            }
        }

        itemStackList.removeAll(itemsToRemove);
        plugin.getCacheManager().getRemovedItemStackMap()
                .put(event.getEntity().getUniqueId(), new ArrayList<>(itemStackList));
    }
}