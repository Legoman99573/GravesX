package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveZombieSpawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private Location location;
    private final LivingEntity targetEntity;
    private boolean cancel;

    /**
     * Constructs a new GraveAutoLootEvent.
     *
     * @param targetEntity    Gets the entity
     * @param location        Gets the grave location
     * @param grave           The grave associated with the event.
     */
    public GraveZombieSpawnEvent(Location location, LivingEntity targetEntity, Grave grave) {
        this.targetEntity = targetEntity;
        this.location = location;
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
        return targetEntity;
    }

    /**
     * Gets the type of the entity for which the grave is being created.
     *
     * @return The entity type.
     */
    public EntityType getEntityType() {
        return targetEntity != null ? targetEntity.getType() : null;
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
