package com.ranull.graves.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveBlockPlaceEvent class represents an event where a block associated
 * with a grave is placed in the world. This event is cancellable, meaning it
 * can be prevented from occurring by event listeners.
 */
public class GraveBlockPlaceEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveBlockPlaceEvent.
     *
     * @param grave     The grave associated with the event.
     * @param location  The location where the block is being placed.
     * @param blockType The type of the block being placed.
     * @param block     The block being placed.
     */
    public GraveBlockPlaceEvent(Grave grave, Location location, BlockData.BlockType blockType, Block block) {
        super(grave, null, location, null, null, blockType, block, null, null);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}