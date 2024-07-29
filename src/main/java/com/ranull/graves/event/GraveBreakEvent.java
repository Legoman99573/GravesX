package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveBreakEvent class represents an event where a grave block is broken
 * by a player. This event extends the BlockBreakEvent and includes additional
 * information about the grave and whether items should drop upon breaking the grave block.
 */
public class GraveBreakEvent extends BlockBreakEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private boolean dropItems;
    private boolean isCancelled;

    /**
     * Constructs a new GraveBreakEvent.
     *
     * @param block  The block being broken.
     * @param player The player breaking the block.
     * @param grave  The grave associated with the block being broken.
     */
    public GraveBreakEvent(Block block, Player player, Grave grave) {
        super(block, player);
        this.dropItems = true;
        this.grave = grave;
        this.isCancelled = false;
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
     * Gets the grave associated with the event.
     *
     * @return The grave associated with the event.
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the experience points associated with breaking the grave.
     *
     * @return The experience points.
     */
    public int getBlockExp() {
        return grave.getExperience();
    }

    /**
     * Checks whether items should drop upon breaking the grave block.
     *
     * @return True if items should drop, false otherwise.
     */
    public boolean isDropItems() {
        return this.dropItems;
    }

    /**
     * Sets whether items should drop upon breaking the grave block.
     *
     * @param dropItems True if items should drop, false otherwise.
     */
    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins.
     *
     * @return True if this event is cancelled.
     */
    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins.
     *
     * @param cancel True if you wish to cancel this event.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
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
}