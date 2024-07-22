package com.ranull.graves.util;

/**
 * Utility class for handling class loading operations.
 */
public final class ClassUtil {

    /**
     * Loads the class with the specified name.
     *
     * @param className The fully qualified name of the class to be loaded.
     */
    public static void loadClass(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}