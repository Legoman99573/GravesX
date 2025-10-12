package com.ranull.graves.util;

/**
 * Utility class for handling class loading operations.
 */
public class ClassUtil {

    private ClassUtil() {}

    /**
     * Loads the class with the specified name.
     *
     * @param className The fully qualified name of the class to be loaded.
     */
    public static void loadClass(String className) {
        if (className == null || className.isBlank()) return;

        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(className, true, ctx != null ? ctx : ClassLoader.getSystemClassLoader());
            return;
        } catch (ClassNotFoundException ignored) {
        } catch (LinkageError e) {
            e.printStackTrace();
            return;
        }

        try {
            Class.forName(className);
        } catch (ClassNotFoundException | LinkageError e) {
            e.printStackTrace();
        }
    }
}