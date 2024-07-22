package com.ranull.graves.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

/**
 * Utility class for checking the latest version of a resource from SpigotMC.
 */
public final class UpdateUtil {

    /**
     * Gets the latest version of a resource from SpigotMC.
     *
     * @param resourceId The ID of the resource on SpigotMC.
     * @return The latest version of the resource as a String, or null if an error occurs.
     */
    public static String getLatestVersion(int resourceId) {
        try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource="
                + resourceId).openStream();
             Scanner scanner = new Scanner(inputStream)) {

            if (scanner.hasNext()) {
                return scanner.next();
            }
        } catch (IOException ignored) {
        }

        return null;
    }
}