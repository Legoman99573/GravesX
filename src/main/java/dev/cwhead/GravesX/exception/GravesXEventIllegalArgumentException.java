package dev.cwhead.GravesX.exception;

import java.io.Serial;

public class GravesXEventIllegalArgumentException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
     * is called but that method is performing an illegal argument.
     */
    public GravesXEventIllegalArgumentException(String message) {
        super(message);
    }
}
