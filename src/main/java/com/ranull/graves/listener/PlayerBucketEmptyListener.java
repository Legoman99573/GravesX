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
        Block block = getTargetBlock(event);

        if (isGraveBlock(block)) {
            preventBucketEmpty(event, block);
        }
    }

    /**
     * Gets the block that the player is interacting with when emptying the bucket.
     *
     * @param event The PlayerBucketEmptyEvent.
     * @return The block being interacted with.
     */
    private Block getTargetBlock(PlayerBucketEmptyEvent event) {
        return event.getBlockClicked().getRelative(event.getBlockFace());
    }

    /**
     * Checks if the block is part of a grave.
     *
     * @param block The block to check.
     * @return True if the block is part of a grave, false otherwise.
     */
    private boolean isGraveBlock(Block block) {
        return plugin.getBlockManager().getGraveFromBlock(block) != null;
    }

    /**
     * Prevents the bucket from being emptied on the grave block by cancelling the event
     * and updating the block's state.
     *
     * @param event The PlayerBucketEmptyEvent.
     * @param block The block being interacted with.
     */
    private void preventBucketEmpty(PlayerBucketEmptyEvent event, Block block) {
        block.getState().update();
        event.setCancelled(true);
    }
}