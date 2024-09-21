package com.ranull.graves.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import org.bukkit.block.Block;
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

        Grave grave = plugin.getEntityDataManager().getGrave(event.getBukkitEntity());

        String furnitureId = event.getNamespacedID();

        if (grave != null) {
            if (plugin.getConfig("itemsadder.furniture.enabled", player).getBoolean("itemsadder.furniture.enabled") && furnitureId.equals(plugin.getConfig("itemsadder.furniture.name", player).getString("itemsadder.furniture.name"))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakIA(CustomBlockBreakEvent event) {
        Player player = event.getPlayer();

        Block block = event.getBlock();

        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        String blockId = event.getNamespacedID();

        if (grave != null) {
            if (plugin.getConfig("itemsadder.block.enabled", player).getBoolean("itemsadder.block.enabled") && blockId.equals(plugin.getConfig("itemsadder.block.name", player).getString("itemsadder.block.name"))) {
                event.setCancelled(true);
            }
        }
    }
}
