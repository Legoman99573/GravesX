package dev.cwhead.GravesX.module.util;

import dev.cwhead.GravesX.module.ModuleContext;

import java.util.Objects;

/**
 * Imports external libraries for a module.
 */
public interface LibraryImporter {

    /**
     * Imports one or more libraries by coordinate strings.
     *
     * @param ctx module context
     * @param coordinates Maven coordinates (group:artifact:version[key=value...])
     */
    void importLibrary(ModuleContext ctx, String... coordinates);

    /**
     * Imports a library described in {@code module.yml:libraries}.
     *
     * <p>Default behavior imports by {@link ModuleInfo.LibraryDef#coordinates()} only.
     * Importers that support extra flags should override this method.</p>
     *
     * @param ctx module context
     * @param def library definition
     */
    default void importLibrary(ModuleContext ctx, ModuleInfo.LibraryDef def) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(def, "def");
        String coords = def.coordinates();
        if (coords == null || coords.isBlank()) return;
        importLibrary(ctx, coords);
    }
}
