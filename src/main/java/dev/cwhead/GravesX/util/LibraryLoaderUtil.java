package dev.cwhead.GravesX.util;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.ranull.graves.Graves;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Utility class for loading external libraries dynamically using BukkitLibraryManager.
 * <p>
 * This class provides methods to load libraries from Maven repositories, with support for
 * relocation and isolation of the loaded libraries.
 * </p>
 */
public class LibraryLoaderUtil {
    private final Graves plugin;

    /**
     * Constructs a new LibraryLoaderUtil instance.
     *
     * @param plugin The plugin instance to associate with the library manager.
     */
    public LibraryLoaderUtil(Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Loads a library using the given group ID, artifact ID, and version.
     * <p>
     * Uses default values: no relocation, no loader ID, non-isolated,
     * no custom repository URL, and resolves transitive dependencies.
     * </p>
     *
     * @param groupID    (Required) Maven group ID of the library.
     * @param artifactID (Required) Maven artifact ID of the library.
     * @param version    (Required) Version of the library.
     */
    public void loadLibrary(String groupID, String artifactID, String version) {
        loadLibrary(groupID, artifactID, version, null, null, null, false, null, true);
    }

    /**
     * Loads a library with the specified group ID, artifact ID, version, and isolation flag.
     * <p>
     * Uses default values: no relocation, no loader ID, no custom repository URL,
     * and resolves transitive dependencies.
     * </p>
     *
     * @param groupID    (Required) Maven group ID of the library.
     * @param artifactID (Required) Maven artifact ID of the library.
     * @param version    (Required) Version of the library.
     * @param isIsolated (Required) Whether the library should be loaded in an isolated class loader.
     */
    public void loadLibrary(String groupID, String artifactID, String version, boolean isIsolated) {
        loadLibrary(groupID, artifactID, version, null, null, null, isIsolated, null, true);
    }

    /**
     * Loads a library with relocation and isolation settings.
     * <p>
     * Uses default values: no loader ID, no custom repository URL,
     * and resolves transitive dependencies.
     * </p>
     *
     * @param groupID                  (Required) Maven group ID of the library.
     * @param artifactID               (Required) Maven artifact ID of the library.
     * @param version                  (Required) Version of the library.
     * @param relocatePattern          (Optional) Original package pattern to relocate (requires relocated pattern).
     * @param relocateRelocatedPattern (Optional) Target package pattern for relocation (requires original pattern).
     * @param isIsolated               (Required) Whether the library should be loaded in an isolated class loader.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, null, true);
    }

    /**
     * Loads a library with relocation, isolation, and transitive dependency settings.
     * <p>
     * Uses default values: no loader ID, no custom repository URL.
     * </p>
     *
     * @param groupID                       (Required) Maven group ID of the library.
     * @param artifactID                    (Required) Maven artifact ID of the library.
     * @param version                       (Required) Version of the library.
     * @param relocatePattern               (Optional) Original package pattern to relocate (requires relocated pattern).
     * @param relocateRelocatedPattern      (Optional) Target package pattern for relocation (requires original pattern).
     * @param isIsolated                    (Required) Whether the library should be loaded in an isolated class loader.
     * @param resolveTransitiveDependencies (Required) Whether to resolve transitive dependencies.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, boolean resolveTransitiveDependencies) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, null, resolveTransitiveDependencies);
    }

    /**
     * Loads a library with relocation, isolation, and custom repository URL.
     * <p>
     * Uses default values: no loader ID, resolves transitive dependencies.
     * </p>
     *
     * @param groupID                  (Required) Maven group ID of the library.
     * @param artifactID               (Required) Maven artifact ID of the library.
     * @param version                  (Required) Version of the library.
     * @param relocatePattern          (Optional) Original package pattern to relocate (requires relocated pattern).
     * @param relocateRelocatedPattern (Optional) Target package pattern for relocation (requires original pattern).
     * @param isIsolated               (Required) Whether the library should be loaded in an isolated class loader.
     * @param libraryURL               (Optional) Custom repository URL; if null, defaults are used.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, String libraryURL) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, libraryURL, true);
    }

    /**
     * Loads a library with relocation, isolation, custom repository URL, and transitive dependency settings.
     * <p>
     * Uses default values: no loader ID.
     * </p>
     *
     * @param groupID                       (Required) Maven group ID of the library.
     * @param artifactID                    (Required) Maven artifact ID of the library.
     * @param version                       (Required) Version of the library.
     * @param relocatePattern               (Optional) Original package pattern to relocate (requires relocated pattern).
     * @param relocateRelocatedPattern      (Optional) Target package pattern for relocation (requires original pattern).
     * @param isIsolated                    (Required) Whether the library should be loaded in an isolated class loader.
     * @param libraryURL                    (Optional) Custom repository URL; if null, defaults are used.
     * @param resolveTransitiveDependencies (Required) Whether to resolve transitive dependencies.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, String libraryURL, boolean resolveTransitiveDependencies) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, libraryURL, resolveTransitiveDependencies);
    }

    /**
     * Loads a library into the runtime using the BukkitLibraryManager.
     *
     * <p><b>Required:</b> {@code groupID}, {@code artifactID}, {@code version}, {@code isIsolated},
     * {@code resolveTransitiveDependencies}.<br>
     * <b>Optional:</b> {@code ID}, {@code relocatePattern} + {@code relocateRelocatedPattern} (must be provided together),
     * {@code libraryURL}.</p>
     *
     * <p>If {@code libraryURL} is null or blank, common repositories (Maven Central, Sonatype, JCenter, JitPack)
     * are registered. If relocation is requested, both patterns must be non-blank. When {@code isIsolated} is true and
     * {@code ID} is blank, a stable loaderId of {@code artifactID + "-isolated"} is used.</p>
     *
     * @param groupID                       (Required) Maven group ID (use "{}" as dot placeholders; replaced with "."
     *                                      in logs/messages only).
     * @param artifactID                    (Required) Maven artifact ID.
     * @param version                       (Required) Library version to resolve.
     * @param ID                            (Optional) Loader ID for namespacing (recommended when isolated).
     * @param relocatePattern               (Optional) Original package pattern to relocate; requires {@code relocateRelocatedPattern}.
     * @param relocateRelocatedPattern      (Optional) Target package pattern for relocation; requires {@code relocatePattern}.
     * @param isIsolated                    (Required) Whether to load the library in an isolated class loader.
     * @param libraryURL                    (Optional) Custom repository URL to resolve from; if blank, defaults are used.
     * @param resolveTransitiveDependencies (Required) Whether to resolve transitive dependencies.
     * @throws IllegalArgumentException if any required parameter is null/blank, or only one relocation pattern is provided.
     */
    public void loadLibrary(String groupID,
                            String artifactID,
                            String version,
                            String ID,
                            String relocatePattern,
                            String relocateRelocatedPattern,
                            boolean isIsolated,
                            String libraryURL,
                            boolean resolveTransitiveDependencies) {

        if (groupID == null || groupID.isBlank()) {
            throw new IllegalArgumentException("groupID is required and cannot be blank.");
        }
        if (artifactID == null || artifactID.isBlank()) {
            throw new IllegalArgumentException("artifactID is required and cannot be blank.");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version is required and cannot be blank.");
        }

        final boolean doRelocate = isDoRelocate(relocatePattern, relocateRelocatedPattern);

        final String groupPretty = groupID.replace("{}", ".");
        final String libLabel = groupPretty + "." + artifactID + ":" + version;

        final boolean hasId = ID != null && !ID.isBlank();
        final String caseKey = (hasId ? "ID" : "PLAIN") + (doRelocate ? "+RELOC" : "");

        LibraryManager libraryManager;
        try {
            libraryManager = getLibraryManager(libraryURL, plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to construct LibraryManager for " + libLabel + ": " + e.getClass().getSimpleName());
            plugin.logStackTrace(e);
            return;
        }
        if (libraryManager == null) {
            plugin.getLogger().severe("LibraryManager was null for " + libLabel + " — aborting this library load.");
            return;
        }

        try {
            Library.Builder builder = Library.builder()
                    .groupId(groupID)
                    .artifactId(artifactID)
                    .version(version)
                    .resolveTransitiveDependencies(resolveTransitiveDependencies);

            switch (caseKey) {
                case "ID+RELOC" -> builder.loaderId(ID).relocate(relocatePattern, relocateRelocatedPattern);
                case "ID"       -> builder.loaderId(ID);
                case "PLAIN+RELOC" -> builder.relocate(relocatePattern, relocateRelocatedPattern);
                default -> { /* PLAIN */ }
            }

            if (isIsolated) {
                if (!hasId) {
                    builder.loaderId(artifactID + "-isolated");
                }
                builder.isolatedLoad(true);
            }

            final Library lib;
            try {
                lib = builder.build();
            } catch (Exception e) {
                plugin.getLogger().severe("Library.builder().build() failed for " + libLabel + ": " + e.getClass().getSimpleName());
                plugin.logStackTrace(e);
                return;
            }

            if (lib == null) {
                plugin.getLogger().severe("Builder returned null Library for " + libLabel + " — skipping.");
                return;
            }

            String loaderId = null;
            try {
                loaderId = lib.getLoaderId();
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not read loaderId for " + libLabel + ": " + t.getClass().getSimpleName());
            }

            plugin.debugMessage(
                    "Loading library " + libLabel +
                            (isIsolated ? " [isolated]" : "") +
                            (doRelocate ? " [relocated]" : "") +
                            ((hasId || isIsolated) ? " (loaderId=" + (loaderId == null ? "null" : loaderId) + ")" : ""),
                    1
            );

            try {
                libraryManager.loadLibrary(lib);
            } catch (NullPointerException npe) {
                plugin.getLogger().severe("NullPointerException while loading library " + libLabel + ". Likely cause: missing repository, malformed library descriptor, or resolver returned null objects.");
                plugin.logStackTrace(npe);
                return;
            } catch (Exception ex) {
                plugin.getLogger().severe("Exception while loading library " + libLabel + ": " + ex.getClass().getSimpleName());
                plugin.logStackTrace(ex);
                return;
            }

            if (isIsolated && doRelocate) {
                try {
                    if (relocateRelocatedPattern == null || relocateRelocatedPattern.isBlank()) {
                        plugin.getLogger().warning("Relocation target pattern is blank for " + libLabel + " — skipping verification.");
                    } else {
                        final String probe = relocateRelocatedPattern.replace("{}", ".");
                        if (probe.indexOf('.') < 0) {
                            plugin.getLogger().warning("Relocation verification probe '" + probe + "' looks invalid for " + libLabel + " — skipping verification.");
                        } else {
                            try {
                                Class.forName(probe, false, Thread.currentThread().getContextClassLoader());
                                plugin.debugMessage("Verified shaded library " + libLabel + ".", 1);
                            } catch (ClassNotFoundException cnfe) {
                                plugin.getLogger().severe("Shaded verification failed for " + libLabel + ": ClassNotFoundException");
                                plugin.logStackTrace(cnfe);
                                plugin.getServer().getPluginManager().disablePlugin(plugin);
                            } catch (Throwable th) {
                                plugin.getLogger().severe("Shaded verification failed for " + libLabel + ": " + th.getClass().getSimpleName());
                                plugin.logStackTrace(th);
                                plugin.getServer().getPluginManager().disablePlugin(plugin);
                            }
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().severe("Unexpected error during shaded verification for " + libLabel + ": " + t.getClass().getSimpleName());
                    plugin.logStackTrace(t);
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
            } else {
                plugin.debugMessage("Loaded library " + libLabel + ".", 1);
            }

        } catch (IllegalArgumentException iae) {
            plugin.getLogger().severe("Invalid arguments while preparing to load " + libLabel + ": " + iae.getMessage());
            plugin.logStackTrace(iae);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load " + libLabel + ": " + e.getClass().getSimpleName());
            plugin.logStackTrace(e);
        }
    }

    private static @NotNull LibraryManager getLibraryManager(String libraryURL, Graves plugin) {
        final LibraryManager libraryManager = new BukkitLibraryManager(plugin);
        if (libraryURL != null && !libraryURL.isBlank()) {
            try {
                libraryManager.addRepository(libraryURL);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to add custom repository '" + libraryURL + "': " + e.getClass().getSimpleName());
            }
        }
        try {
            libraryManager.addMavenCentral();
        } catch (Throwable t) {
            plugin.getLogger().warning("addMavenCentral failed: " + t.getClass().getSimpleName());
        }
        try {
            libraryManager.addSonatype();
        } catch (Throwable t) {
            plugin.getLogger().warning("addSonatype failed: " + t.getClass().getSimpleName());
        }
        try {
            libraryManager.addJCenter();
        } catch (Throwable t) {
            Bukkit.getLogger().warning("addJCenter failed: " + t.getClass().getSimpleName());
        }
        try {
            libraryManager.addJitPack();
        } catch (Throwable t) {
            Bukkit.getLogger().warning("addJitPack failed: " + t.getClass().getSimpleName());
        }
        return libraryManager;
    }

    private static boolean isDoRelocate(String relocatePattern, String relocateRelocatedPattern) {
        final boolean hasRelocatePattern = relocatePattern != null && !relocatePattern.isBlank();
        final boolean hasRelocateTarget  = relocateRelocatedPattern != null && !relocateRelocatedPattern.isBlank();

        if (hasRelocatePattern ^ hasRelocateTarget) {
            throw new IllegalArgumentException("If relocation is used, both relocatePattern and relocateRelocatedPattern must be provided.");
        }
        return hasRelocatePattern;
    }
}