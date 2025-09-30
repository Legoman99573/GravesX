package dev.cwhead.GravesX.addon;

import com.ranull.graves.Graves;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Objects;
import java.util.logging.Level;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Handles Addon Creation
 */
public class GravesXAddon {

    /**
     * Ensures plugins/'PluginName'/addon exists.
     */
    public static void ensureAddonRoot(Graves plugin) {
        File data = plugin.getDataFolder();
        File addonRoot = new File(data, "addon");
        try {
            Files.createDirectories(addonRoot.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to create addon folder: " + addonRoot.getAbsolutePath(), e);
            throw new IllegalStateException("Could not create addon root: " + addonRoot, e);
        }
    }

    /**
     * Ensures plugins/'PluginName'/addon/'addonName' exists and returns it.
     */
    public static File ensureAddonFolder(Graves plugin, String addonName) {
        Objects.requireNonNull(plugin, "plugin");
        if (addonName == null || addonName.trim().isEmpty()) {
            throw new IllegalArgumentException("addonName cannot be null or blank");
        }

        File data = plugin.getDataFolder();
        File addonRoot = new File(data, "addon");
        File addonDir = new File(addonRoot, sanitize(addonName));

        try {
            Files.createDirectories(addonDir.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to create addon folder: " + addonDir.getAbsolutePath(), e);
            throw new IllegalStateException("Could not create addon folder: " + addonDir, e);
        }
        return addonDir;
    }

    /**
     * Copies all *.yml files found in resources at:
     *   addon/'addonName'/...
     * into:
     *   plugins/GravesX/addon/'addonName'/'file'.yml
     *
     * Skips files that already exist (set replaceIfExists=true to overwrite).
     *
     * @return number of files written
     */
    public static int exportAddonConfigs(Graves plugin, String addonName, boolean replaceIfExists) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(addonName, "addonName");

        File destDir = ensureAddonFolder(plugin, addonName);
        Path destAbs = destDir.toPath().toAbsolutePath().normalize(); // base path
        final String resourcePrefix = "addon/" + addonName + "/";
        int written = 0;

        try {
            URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            File codeFile = new File(codeSource.toURI());

            if (codeFile.isFile() && codeFile.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(codeFile)) {
                    for (Enumeration<JarEntry> en = jar.entries(); en.hasMoreElements(); ) {
                        JarEntry entry = en.nextElement();
                        String name = entry.getName();
                        if (entry.isDirectory()) continue;
                        if (!name.startsWith(resourcePrefix)) continue;
                        if (!name.endsWith(".yml")) continue;

                        String fileName = name.substring(resourcePrefix.length());
                        if (fileName.contains("/")) continue;

                        Path target = destAbs.resolve(fileName).normalize();
                        if (!target.startsWith(destAbs)) {
                            plugin.getLogger().warning("Skipping suspicious path in JAR: " + name);
                            continue;
                        }

                        if (Files.exists(target) && !replaceIfExists) continue;

                        Path parent = target.getParent();
                        if (parent != null) Files.createDirectories(parent);

                        try (InputStream in = jar.getInputStream(entry)) {
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }

                        if (Files.isSymbolicLink(target)) {
                            Files.delete(target);
                            plugin.getLogger().warning("Skipped symlink file: " + name);
                            continue;
                        }

                        written++;
                    }
                }
            } else if (codeFile.isDirectory()) {
                File resFolder = new File(codeFile, resourcePrefix);
                if (resFolder.isDirectory()) {
                    File[] files = resFolder.listFiles((dir, n) -> n.endsWith(".yml"));
                    if (files != null) {
                        for (File f : files) {
                            Path target = destAbs.resolve(f.getName()).normalize();
                            if (!target.startsWith(destAbs)) {
                                plugin.getLogger().warning("Skipping suspicious dev file: " + f.getPath());
                                continue;
                            }
                            if (Files.exists(target) && !replaceIfExists) continue;

                            Path parent = target.getParent();
                            if (parent != null) Files.createDirectories(parent);
                            Files.copy(f.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                            written++;
                        }
                    }
                } else {
                    plugin.getLogger().warning("Resource path not found in dev environment: " + resFolder.getPath());
                }
            } else {
                plugin.getLogger().warning("Unknown plugin code source type: " + codeFile);
            }
        } catch (URISyntaxException | IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed exporting addon configs for " + addonName, ex);
            throw new IllegalStateException("Export failed for addon: " + addonName, ex);
        }

        if (written > 0) {
            plugin.debugMessage("Exported " + written + " config file(s) for addon '" + addonName + "'.", 1);
        } else {
            plugin.debugMessage("No new configs exported for addon '" + addonName + "'.", 1);
        }
        return written;
    }

    /**
     * Helper: copy a single resource from the JAR to a target file if needed.
     */
    private static boolean copyResourceIfNeeded(Graves plugin, String resourcePath, File target, boolean replace)
            throws IOException {
        if (target.exists() && !replace) return false;

        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Missing bundled resource: " + resourcePath);
                return false;
            }
            Files.createDirectories(target.getParentFile().toPath());
            Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }

    /** Allow letters, numbers, dot, underscore, dash; replace others with '_' */
    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

