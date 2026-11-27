package dev.cwhead.GravesX.cache;

/**
 * Defines the available cache storage types.
 */
public enum CacheType {

    /**
     * Stores cache data in memory.
     */
    NORMAL,

    /**
     * Stores cache data on disk.
     */
    DISK,

    /**
     * Disables caching and reads authoritative database rows directly.
     */
    DATABASE;

    /**
     * Gets a cache type from a string.
     */
    public static CacheType fromString(String value) {
        if (value == null || value.isBlank()) return NORMAL;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }
}