package dev.cwhead.GravesX.module.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * Class loader for a single module JAR using parent-first delegation.
 */
public final class ModuleClassLoader extends URLClassLoader {

    /**
     * Creates a class loader for the given module JAR.
     *
     * @param jarUrl URL of the module JAR.
     * @param parent Parent class loader to delegate to.
     */
    public ModuleClassLoader(URL jarUrl, ClassLoader parent) {
        super(new URL[] { Objects.requireNonNull(jarUrl, "jarUrl") }, Objects.requireNonNull(parent, "parent"));
    }

    /**
     * Closes the class loader and releases resources. Any errors are ignored.
     */
    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception ignored) {
            // ignored
        }
    }
}