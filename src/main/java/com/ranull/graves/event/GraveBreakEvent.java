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
    private final Block block;
    private boolean dropItems;

    /**
     * Constructs a new GraveBreakEvent.
     *
     * @param block  The block being broken.
     * @param player The player breaking the block.
     * @param grave  The grave associated with the block being broken.
     */
    public GraveBreakEvent(Block block, Player player, Grave grave) {
        super(grave, null, block.getLocation(), null, null, null, null, player);
        this.block = block;
        this.dropItems = true;
    }

    /**
     * Gets the block being broken.
     *
     * @return The block being broken.
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Gets the experience points associated with breaking the grave.
     *
     * @return The experience points.
     */
    public int getBlockExp() {
        return getGrave().getExperience();
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