package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveBreakEvent class represents an event where a grave block is broken
 * by a player. This event extends the GraveEvent and includes additional
 * information about the grave and whether items should drop upon breaking the grave block.
 */
public class GraveBreakEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveBreakEvent.
     *
     * @param block  The block being broken.
     * @param player The player breaking the block.
     * @param grave  The grave associated with the block being broken.
     */
    public GraveBreakEvent(Block block, Player player, Grave grave) {
        super(grave, null, block.getLocation(), null, null, null, block, null, player);
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The list of handlers.
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}