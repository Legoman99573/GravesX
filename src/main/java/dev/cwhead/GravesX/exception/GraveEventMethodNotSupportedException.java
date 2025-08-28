package dev.cwhead.GravesX.exception;

import dev.cwhead.GravesX.event.GraveEvent;

/**
 * Thrown when a method on a {@link GraveEvent}
 * is called but that method is not supported for the specific event type.
 */
public class GraveEventMethodNotSupportedException extends UnsupportedOperationException {

    /**
     * Thrown when a method on a {@link GraveEvent}
     * is called but that method is not supported for the specific event type.
     */
    public GraveEventMethodNotSupportedException(String message) {
        super(message);
    }
}