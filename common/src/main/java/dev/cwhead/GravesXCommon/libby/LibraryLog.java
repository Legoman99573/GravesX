package dev.cwhead.GravesXCommon.libby;

/**
 * Where the shared library loader reports to. Each platform adapts this to its own logger.
 */
public interface LibraryLog {
    /**
     * Verbose progress, normally only shown when debugging.
     */
    void debug(String message);

    void warn(String message);

    void error(String message, Throwable error);
}
