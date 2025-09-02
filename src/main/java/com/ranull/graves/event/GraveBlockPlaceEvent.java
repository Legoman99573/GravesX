package com.ranull.graves.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveBlockPlaceEvent} instead.
 * Represents an event that occurs when a grave is placed by a {@link org.bukkit.entity.LivingEntity} whether that be a {@link org.bukkit.entity.Player} or {@link org.bukkit.entity.Entity}.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and provides information about the grave
 * and the {@link org.bukkit.entity.LivingEntity} ({@link org.bukkit.entity.Player} or {@link org.bukkit.entity.Entity}) involved when the grave is placed.
 * </p>
 */
@Deprecated (since = "4.9.9.1", forRemoval = true)
public class GraveBlockPlaceEvent extends dev.cwhead.GravesX.event.GraveBlockPlaceEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveBlockPlaceEvent} instead.
     * Constructs a new GraveBlockPlaceEvent.
     *
     * @param grave        The grave associated with the event.
     * @param location     The location where the block is being placed.
     * @param blockType    The type of the block being placed.
     * @param block        The block being placed.
     * @param livingEntity The Killer
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveBlockPlaceEvent(@NotNull Grave grave, @NotNull Location location, @NotNull BlockData.BlockType blockType, Block block, LivingEntity livingEntity) {
        super(grave, location, blockType, block, livingEntity);
    }

    /**
     * @deprecated Use {@link GraveBlockPlaceEvent(Grave, Location, BlockData.BlockType, Block, LivingEntity)} instead.
     * Constructs a new GraveBlockPlaceEvent.
     *
     * @param grave     The grave associated with the event.
     * @param location  The location where the block is being placed.
     * @param blockType The type of the block being placed.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveBlockPlaceEvent(@NotNull Grave grave, @NotNull Location location, @NotNull BlockData.BlockType blockType) {
        super(grave, location, blockType, null, null);
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
