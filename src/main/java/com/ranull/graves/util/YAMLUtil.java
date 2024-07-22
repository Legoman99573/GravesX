package com.ranull.graves.util;

import java.io.File;

/**
 * Utility class for handling YAML file operations.
 */
public final class YAMLUtil {

    /**
     * Checks if a given file is a valid YAML file.
     *
     * @param file The file to check.
     * @return True if the file is a valid YAML file (i.e., does not start with a dot and ends with ".yml"), otherwise false.
     */
    public static boolean isValidYAML(File file) {
        return !file.getName().startsWith(".") && file.getName().endsWith(".yml");
    }
}