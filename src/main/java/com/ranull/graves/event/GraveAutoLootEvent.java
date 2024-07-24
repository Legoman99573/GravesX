package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class GraveAutoLootEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private Location location;
    private final Entity entity;
    private boolean cancel;

    /**
     * Constructs a new GraveAutoLootEvent.
     *
     * @param entity    Gets the entity
     * @param location  Gets the grave location
     * @param grave     The grave associated with the event.
     */
    public GraveAutoLootEvent(Entity entity, Location location, Grave grave) {
        this.entity = entity;
        this.location = location;
        this.grave = grave;
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
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the grave associated with the event.
     *
     * @return The grave associated with the event.
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the location of the grave.
     *
     * @return The location of the grave.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of the grave.
     *
     * @param location The location of the grave.
     */
    public void setLocation(Location location) {
        this.location = location;
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