package com.ranull.graves.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveBlockPlaceEvent class represents an event where a block associated
 * with a grave is placed in the world. This event is cancellable, meaning it
 * can be prevented from occurring by event listeners.
 */
public class GraveBlockPlaceEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private Location location;
    private BlockData.BlockType blockType;
    private boolean cancel;

    /**
     * Constructs a new GraveBlockPlaceEvent.
     *
     * @param grave     The grave associated with the event.
     * @param location  The location where the block is being placed.
     * @param blockType The type of the block being placed.
     */
    public GraveBlockPlaceEvent(Grave grave, Location location, BlockData.BlockType blockType) {
        this.location = location;
        this.grave = grave;
        this.blockType = blockType;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The list of handlers.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the location where the block is being placed.
     *
     * @return The location where the block is being placed.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location where the block is being placed.
     *
     * @param location The new location where the block is being placed.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the block at the location where the block is being placed.
     *
     * @return The block at the location.
     */
    public Block getBlock() {
        return location.getBlock();
    }

    /**
     * Gets the grave associated with the event.
     *
     * @return The grave associated with the event.
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the type of the block being placed.
     *
     * @return The type of the block being placed.
     */
    public BlockData.BlockType getBlockType() {
        return blockType;
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

    /**
     * Checks whether the event is cancelled.
     *
     * @return True if the event is cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        return cancel;
    }

    /**
     * Sets whether the event should be cancelled.
     *
     * @param cancel True to cancel the event, false otherwise.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}