package dev.cwhead.GravesX.exception;

import org.bukkit.event.Event;

import java.io.Serial;

public class GravesXNullPointerException extends NullPointerException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
     * is called but that method is null for the specific event type.
     * @param event The event that fired the error
     * @param cause The cause of the error to occur
     */
    public GravesXNullPointerException(Event event, String cause) {
        super(event.getEventName() + " tried to find " + cause + ", but returned null.");
    }

    /**
     * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
     * is called but that method is null for the specific event type.
     */
    public GravesXNullPointerException(String message) {
        super(message);
    }
}