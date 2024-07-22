package com.ranull.graves.util;

import java.util.UUID;

/**
 * Utility class for handling UUID operations.
 */
public final class UUIDUtil {

    /**
     * Converts a string to a UUID.
     *
     * @param string The string to convert to a UUID.
     * @return The UUID if the string is a valid UUID format, otherwise null.
     */
    public static UUID getUUID(String string) {
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}