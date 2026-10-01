package dev.cwhead.GravesX.api.cache;

/**
 * Public storage modes. Use capabilities on CacheAPI rather than switching on modes when possible.
 */
public enum CacheMode {
    /**
     * Live cache values in memory.
     */
    NORMAL,

    /**
     * Cache values loaded from disk on demand.
     */
    DISK,

    /**
     * Persistent cache disabled; source rows and temporary interaction state use SQL.
     */
    DATABASE
}