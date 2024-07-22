package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveTimeoutEvent class represents an event that occurs when a grave times out.
 * This event is cancellable, meaning it can be prevented from occurring by event listeners.
 */
public class GraveTimeoutEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private Location location;
    private boolean cancel;

    /**
     * Constructs a new GraveTimeoutEvent.
     *
     * @param grave The grave that is timing out.
     */
    public GraveTimeoutEvent(Grave grave) {
        this.grave = grave;
        this.location = grave.getLocationDeath();
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
     * Gets the grave that is timing out.
     *
     * @return The grave that is timing out.
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the location of the grave that is timing out.
     *
     * @return The location of the grave.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of the grave that is timing out.
     *
     * @param location The new location of the grave.
     */
    public void setLocation(Location location) {
        this.location = location;
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