package dev.cwhead.GravesX.exception;

import java.io.Serial;

/**
 * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
 * is called but that method is not supported for the specific event type.
 */
public class GravesXEventMethodNotSupportedException extends UnsupportedOperationException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
     * is called but that method is not supported for the specific event type.
     */
    public GravesXEventMethodNotSupportedException(String message) {
        super(message);
    }
}
