package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The GraveExplodeEvent class represents an event where a grave explodes.
 * This event is cancellable, meaning it can be prevented from occurring by event listeners.
 */
public class GraveExplodeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Location location;
    private final Entity entity;
    private final Grave grave;
    private boolean cancel;

    /**
     * Constructs a new GraveExplodeEvent.
     *
     * @param location The location of the explosion.
     * @param entity   The entity that caused the explosion, if any.
     * @param grave    The grave that is exploding.
     */
    public GraveExplodeEvent(Location location, @Nullable Entity entity, Grave grave) {
        this.location = location;
        this.entity = entity;
        this.grave = grave;
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

    /**
     * Gets the location of the explosion.
     *
     * @return The location of the explosion.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the grave that is exploding.
     *
     * @return The grave that is exploding.
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the entity that caused the explosion, if any.
     *
     * @return The entity that caused the explosion, or null if there is no entity.
     */
    @Nullable
    public Entity getEntity() {
        return entity;
    }
}