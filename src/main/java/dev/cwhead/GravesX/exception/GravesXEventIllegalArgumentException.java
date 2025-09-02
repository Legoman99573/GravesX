package dev.cwhead.GravesX.exception;

public class GravesXEventIllegalArgumentException extends IllegalArgumentException {

    /**
     * Thrown when a method on a {@link dev.cwhead.GravesX.event.graveevent.GraveEvent}
     * is called but that method is performing an illegal argument.
     */
    public GravesXEventIllegalArgumentException(String message) {
        super(message);
    }
}
