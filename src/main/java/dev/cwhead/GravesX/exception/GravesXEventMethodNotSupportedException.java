package dev.cwhead.GravesX.exception;

/**
 * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
 * is called but that method is not supported for the specific event type.
 */
public class GravesXEventMethodNotSupportedException extends UnsupportedOperationException {

    /**
     * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
     * is called but that method is not supported for the specific event type.
     */
    public GravesXEventMethodNotSupportedException(String message) {
        super(message);
    }
}