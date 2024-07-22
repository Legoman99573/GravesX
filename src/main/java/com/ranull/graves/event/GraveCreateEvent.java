package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveCreateEvent class represents an event where a grave is created for an entity.
 * This event is cancellable, meaning it can be prevented from occurring by event listeners.
 */
public class GraveCreateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Entity entity;
    private final Grave grave;
    private boolean cancel;

    /**
     * Constructs a new GraveCreateEvent.
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    public GraveCreateEvent(Entity entity, Grave grave) {
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
     * Gets the entity for which the grave is being created.
     *
     * @return The entity.
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Gets the type of the entity for which the grave is being created.
     *
     * @return The entity type.
     */
    public EntityType getEntityType() {
        return entity != null ? entity.getType() : null;
    }

    /**
     * Gets the grave being created.
     *
     * @return The grave.
     */
    public Grave getGrave() {
        return grave;
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