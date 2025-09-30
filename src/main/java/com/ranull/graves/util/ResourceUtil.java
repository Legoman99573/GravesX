package com.ranull.graves.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for copying resources from a plugin's JAR file to the file system.
 */
public final class ResourceUtil {

    private ResourceUtil() {}

    /**
     * Copies resources from the plugin's JAR file to the specified output path.
     *
     * @param inputPath  The path inside the JAR file to copy from.
     * @param outputPath The path on the file system to copy to.
     * @param plugin     The plugin instance.
     */
    public static void copyResources(String inputPath, String outputPath, JavaPlugin plugin) {
        copyResources(inputPath, outputPath, true, plugin);
    }

    /**
     * Copies resources from the plugin's JAR file to the specified output path.
     *
     * @param inputPath  The path inside the JAR file to copy from.
     * @param outputPath The path on the file system to copy to.
     * @param overwrite  Whether to overwrite existing files.
     * @param plugin     The plugin instance.
     */
    public static void copyResources(String inputPath, String outputPath, boolean overwrite, JavaPlugin plugin) {
        inputPath = formatString(inputPath);
        outputPath = formatString(outputPath);

        saveResources(getResources(inputPath, plugin), inputPath, outputPath, overwrite);
    }

    /**
     * Retrieves resources from the plugin's JAR file at the specified path.
     *
     * @param path   The path inside the JAR file.
     * @param plugin The plugin instance.
     * @return A map of resource paths to their input streams.
     */
    private static Map<String, InputStream> getResources(String path, JavaPlugin plugin) {
        Map<String, InputStream> inputStreamMap = new HashMap<>();
        URL url = plugin.getClass().getClassLoader().getResource(path);

        if (url != null) {
            try {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                try (JarFile jarFile = connection.getJarFile()) {
                    Path basePath = Paths.get(path).normalize();

                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        if (jarEntry.isDirectory()) continue;

                        Path entryPath = Paths.get(jarEntry.getName()).normalize();

                        if (!entryPath.startsWith(basePath)) continue;

                        InputStream is = plugin.getResource(jarEntry.getName());
                        if (is != null) {
                            inputStreamMap.put(jarEntry.getName(), is);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return inputStreamMap;
    }

    /**
     * Saves the retrieved resources to the specified output path.
     *
     * @param inputStreamMap A map of resource paths to their input streams.
     * @param inputPath      The path inside the JAR file.
     * @param outputPath     The path on the file system to copy to.
     * @param overwrite      Whether to overwrite existing files.
     */
    private static void saveResources(Map<String, InputStream> inputStreamMap, String inputPath, String outputPath,
                                      boolean overwrite) {
        for (Map.Entry<String, InputStream> entry : inputStreamMap.entrySet()) {
            String path = entry.getKey();
            InputStream inputStream = entry.getValue();
            File outputFile = new File(outputPath + File.separator + path.replaceFirst(inputPath, ""));

            if (!outputFile.exists() || overwrite) {
                if (createDirectories(outputFile)) {
                    try (InputStream in = inputStream; OutputStream out = Files.newOutputStream(outputFile.toPath())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    /**
     * Creates the necessary directories for the specified file.
     *
     * @param file The file for which to create directories.
     * @return True if the directories were created successfully, false otherwise.
     */
    private static boolean createDirectories(File file) {
        File parentFile = file.getParentFile();
        return parentFile != null && (parentFile.exists() || parentFile.mkdirs());
    }

    /**
     * Formats a file path string to use the system's file separator.
     *
     * @param string The string to format.
     * @return The formatted string.
     */
    private static String formatString(String string) {
        return string.replace("/", File.separator);
    }
}