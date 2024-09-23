package com.ranull.graves.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * A listener for handling ItemsAdder's {@link CustomBlockBreakEvent}.
 * This class is responsible for cancelling the block break event for graves,
 * depending on the configuration settings in the Graves plugin.
 */
public class CustomBlockBreakListener implements Listener {

    private final Graves plugin;

    /**
     * Constructs a new {@code CustomBlockBreakListener}.
     *
     * @param plugin the instance of the {@link Graves} plugin
     */
    public CustomBlockBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the {@link CustomBlockBreakEvent} for ItemsAdder custom blocks.
     * Cancels the event if the block is part of a grave and the configuration
     * for preventing block breaking is enabled.
     *
     * @param event the ItemsAdder {@code CustomBlockBreakEvent} triggered when
     *              a custom block is broken
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakIA(CustomBlockBreakEvent event) {
        Player player = event.getPlayer();

        // If the player has permission to break graves, allow it
        if (plugin.getConfig("grave.break", player).getBoolean("grave.break")) return;

        Block block = event.getBlock();

        // Retrieve the grave associated with the block
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        String blockId = event.getNamespacedID();

        // If the block is part of a grave and matches the configured ItemsAdder block, cancel the event
        if (grave != null) {
            if (plugin.getConfig("itemsadder.block.enabled", player).getBoolean("itemsadder.block.enabled") &&
                    blockId.equals(plugin.getConfig("itemsadder.block.name", player).getString("itemsadder.block.name"))) {
                event.setCancelled(true);
            }
        }
    }
}