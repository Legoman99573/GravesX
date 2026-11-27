package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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

    private final @NotNull Block pistonBlock;
    private final @NotNull BlockFace direction;
    private final @NotNull List<Block> movedBlocks;

    /**
     * Constructs a new {@code GravePistonExtendEvent}.
     *
     * @param grave       The grave associated with the event.
     * @param location    The location of the event (nullable).
     * @param pistonBlock The piston block involved in the event.
     * @param direction   The direction the piston is extending.
     * @param movedBlocks The list of blocks being moved by the piston.
     */
    public GravePistonExtendEvent(@NotNull Grave grave, @Nullable Location location, @NotNull Block pistonBlock, @NotNull BlockFace direction, @NotNull List<Block> movedBlocks) {
        super(Objects.requireNonNull(grave, "grave"), location, null, null);
        this.pistonBlock = Objects.requireNonNull(pistonBlock, "pistonBlock");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.movedBlocks = List.copyOf(Objects.requireNonNull(movedBlocks, "movedBlocks"));
    }

    /**
     * Gets the piston block involved in this event.
     *
     * @return the piston block.
     */
    public @NotNull Block getPistonBlock() {
        return pistonBlock;
    }

    /**
     * Gets the direction in which the piston is extending.
     *
     * @return the direction.
     */
    public @NotNull BlockFace getDirection() {
        return direction;
    }

    /**
     * Gets the list of blocks that are being moved by the piston during this extension.
     *
     * @return moved blocks.
     */
    public @NotNull List<Block> getMovedBlocks() {
        return movedBlocks;
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