package com.ranull.graves.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveBreakEvent} instead. Will be removed in 4.9.10.1.
 * The GraveBreakEvent class represents an event where a grave block is broken
 * by a player. This event extends the GraveEvent and includes additional
 * information about the grave and whether items should drop upon breaking the grave block.
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
public class GraveBreakEvent extends dev.cwhead.GravesX.event.GraveBreakEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveBreakEvent} instead.
     * Constructs a new GraveBreakEvent.
     *
     * @param block   The block being broken.
     * @param player  The player breaking the block.
     * @param grave   The grave associated with the block being broken.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveBreakEvent(@NotNull Block block, @NotNull Player player, @NotNull Grave grave) {
        super(block, player, grave);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveBreakEvent} instead.
     *
     * @param block     The block being broken.
     * @param player    The player breaking the block.
     * @param grave     The grave associated with the block being broken.
     * @param blockType The block type if already known (nullable). If null, it will be resolved from the block.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveBreakEvent(@NotNull Block block, @NotNull Player player, @NotNull Grave grave, @Nullable BlockData.BlockType blockType) {
        super(block, player, grave, blockType);
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