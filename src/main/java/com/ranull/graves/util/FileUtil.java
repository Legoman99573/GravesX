package com.ranull.graves.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility class for file operations.
 */
public final class FileUtil {

    /**
     * Moves a file to a new location with a new name.
     *
     * @param file The file to be moved.
     * @param name The new name for the file.
     */
    public static void moveFile(File file, String name) {
        try {
            Files.move(file.toPath(), file.toPath().resolveSibling(name));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * @deprecated
     * <p>
     * This method is deprecated and will be removed in a future version.
     * Use {@link #moveFile(File, String)} instead.
     *</p>
     *
     * Copies a file to a new location with a new name.
     *
     * @param file The file to be copied.
     * @param name The new name for the copied.
     */
    @Deprecated
    public static void copyFile(File file, String name) {
        try {
            Files.copy(file.toPath(), file.toPath().resolveSibling(name));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}