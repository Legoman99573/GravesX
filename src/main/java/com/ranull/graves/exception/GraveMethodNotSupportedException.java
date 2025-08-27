package com.ranull.graves.exception;

/**
 * Thrown when a method on a {@link com.ranull.graves.event.GraveEvent}
 * is called but that method is not supported for the specific event type.
 */
public class GraveMethodNotSupportedException extends UnsupportedOperationException {
    public GraveMethodNotSupportedException(String message) {
        super(message);
    }
}