package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GravePistonExtendEvent} instead. Will be removed in 2027.4.9.1.
 * Represents an event that occurs when a piston extends into a grave location.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and is cancellable, allowing event listeners
 * to prevent the piston from extending.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
public class GravePistonExtendEvent extends dev.cwhead.GravesX.event.GravePistonExtendEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GravePistonExtendEvent} instead.
     * Constructs a new {@code GravePistonExtendEvent}.
     *
     * @param grave       The grave associated with the event.
     * @param location    The location of the event (nullable).
     * @param pistonBlock The piston block involved in the event.
     * @param direction   The direction the piston is extending.
     * @param movedBlocks The list of blocks being moved by the piston.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GravePistonExtendEvent(@NotNull Grave grave, @Nullable Location location, @NotNull Block pistonBlock, @NotNull BlockFace direction, @NotNull List<Block> movedBlocks) {
        super(grave, location, pistonBlock, direction, movedBlocks);
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

    /**
     * Gets the static list of handlers for this event.
     *
     * @return The static handler list for this event.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}