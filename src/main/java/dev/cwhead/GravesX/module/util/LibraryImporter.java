package dev.cwhead.GravesX.module.util;

import dev.cwhead.GravesX.module.ModuleContext;

/**
 * Strategy interface for loading external libraries for a module.
 */
@FunctionalInterface
public interface LibraryImporter {

    /**
     * Imports one or more libraries described by coordinates for the given module.
     *
     * @param ctx Calling module's context.
     * @param coordinates One or more Maven-style coordinates, e.g.
     *                    {@code group:artifact:version[?key=value&...]}.
     */
    void importLibrary(ModuleContext ctx, String... coordinates);
}