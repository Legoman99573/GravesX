package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCreateEvent} instead. Will be removed in 4.9.15.1.
 * Represents an event that occurs when a grave is created for an entity.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEntityEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
public class GraveCreateEvent extends dev.cwhead.GravesX.event.GraveCreateEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCreateEvent} instead.
     * Constructs a new {@code GraveCreateEvent}.
     * <p>
     * This constructor does not provide ignored items/blocks information and simply
     * delegates to the newer event with {@code null} collections.
     * </p>
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveCreateEvent(@NotNull Entity entity, @NotNull Grave grave) {
        super(entity, grave);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCreateEvent} instead.
     * Constructs a new {@code GraveCreateEvent} with ignored items and blocks.
     *
     * @param entity        The entity for which the grave is being created.
     * @param grave         The grave being created.
     * @param ignoredItems  Items that were not stored in the grave (may be {@code null} or empty).
     * @param ignoredBlocks Blocks that were ignored for this grave (may be {@code null} or empty).
     */
    @Deprecated(since = "4.9.14.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveCreateEvent(@NotNull Entity entity,
                            @NotNull Grave grave,
                            @Nullable Collection<ItemStack> ignoredItems,
                            @Nullable Collection<Block> ignoredBlocks) {
        super(entity, grave, ignoredItems, ignoredBlocks);
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