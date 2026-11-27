package com.ranull.graves.util;

import java.util.UUID;

/**
 * Utility class for handling UUID operations.
 */
public class UUIDUtil {

    private UUIDUtil() {}

    /**
     * Converts a string to a UUID.
     *
     * @param input The string to convert to a UUID.
     * @return The UUID if the string is a valid UUID format, otherwise null.
     */
    public static UUID getUUID(String input) {
        String s = input.trim();
        if (s.isEmpty()) {
            return null;
        }

        if (s.contains("-")) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        if (s.length() != 32) {
            return null;
        }

        if (!s.matches("[0-9a-fA-F]{32}")) {
            return null;
        }

        String withDashes =
                s.substring(0, 8) + "-" +
                        s.substring(8, 12) + "-" +
                        s.substring(12, 16) + "-" +
                        s.substring(16, 20) + "-" +
                        s.substring(20);

        try {
            return UUID.fromString(withDashes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}