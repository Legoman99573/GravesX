package com.ranull.graves.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class CustomBlockBreakListener implements Listener {

    private final Graves plugin;

    public CustomBlockBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakIA(CustomBlockBreakEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig("grave.break", player).getBoolean("grave.break")) return; // No need to cancel anything

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
