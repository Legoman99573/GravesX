package com.ranull.graves.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class FurnitureBreakListener implements Listener {
    private final Graves plugin;

    public FurnitureBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig("grave.break", player).getBoolean("grave.break")) return; // No need to cancel anything

        Grave grave = plugin.getEntityDataManager().getGrave(event.getBukkitEntity());

        String furnitureId = event.getNamespacedID();

        if (grave != null) {
            if (plugin.getConfig("itemsadder.furniture.enabled", player).getBoolean("itemsadder.furniture.enabled") && furnitureId.equals(plugin.getConfig("itemsadder.furniture.name", player).getString("itemsadder.furniture.name"))) {
                event.setCancelled(true);
            }
        }
    }
}
