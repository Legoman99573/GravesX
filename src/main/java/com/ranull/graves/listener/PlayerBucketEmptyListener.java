package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Listener for handling PlayerBucketEmptyEvent to prevent buckets from being emptied
 * on graves.
 */
public class PlayerBucketEmptyListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerBucketEmptyListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerBucketEmptyListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerBucketEmptyEvent to prevent emptying buckets on grave blocks.
     * If the block being interacted with is part of a grave, the event is cancelled
     * and the block's state is updated to reflect the prevention of the bucket emptying.
     *
     * @param event The PlayerBucketEmptyEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        // Check if the block is part of a grave
        if (plugin.getBlockManager().getGraveFromBlock(block) != null) {
            block.getState().update();
            // Cancel the bucket emptying event
            event.setCancelled(true);
        }
    }
}