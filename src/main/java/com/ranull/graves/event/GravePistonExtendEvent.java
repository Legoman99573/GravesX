package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents an event that occurs when a piston extends into a grave location.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent the piston from extending.
 * </p>
 */
public class GravePistonExtendEvent extends GraveEvent {
    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled = false;
    private final Block pistonBlock;
    private final BlockFace direction;
    private final List<Block> movedBlocks;

    /**
     * Constructs a new {@code GravePistonExtendEvent}.
     *
     * @param grave          The grave associated with the event.
     * @param location       The location of the event.
     * @param pistonBlock    The piston block involved in the event.
     * @param direction      The direction the piston is extending.
     * @param movedBlocks    The list of blocks being moved by the piston.
     */
    public GravePistonExtendEvent(Grave grave, @Nullable Location location,
                                  @NotNull Block pistonBlock, @NotNull BlockFace direction, @NotNull List<Block> movedBlocks) {
        super(grave, null, location, null, null, null, null, null, null);
        this.pistonBlock = pistonBlock;
        this.direction = direction;
        this.movedBlocks = movedBlocks;
    }

    /**
     * Gets the piston block involved in this event.
     *
     * @return The piston block that is extending.
     */
    public Block getPistonBlock() {
        return pistonBlock;
    }

    /**
     * Gets the direction in which the piston is extending.
     *
     * @return The direction of the piston extension.
     */
    public BlockFace getDirection() {
        return direction;
    }

    /**
     * Gets the list of blocks that are being moved by the piston during this extension.
     *
     * @return A list of blocks affected by the piston movement.
     */
    public List<Block> getMovedBlocks() {
        return movedBlocks;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
