package dev.cwhead.GravesX.util;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.ranull.graves.Graves;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;


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
        this.plugin = plugin;
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
        final boolean doRelocate = isDoRelocate(version, relocatePattern, relocateRelocatedPattern);

        final String groupPretty = groupID.replace("{}", ".");
        final String libLabel = groupPretty + "." + artifactID + ":" + version;

        final boolean hasId = ID != null && !ID.isBlank();
        final String caseKey = (hasId ? "ID" : "PLAIN") + (doRelocate ? "+RELOC" : "");

        try {
            final LibraryManager libraryManager = getLibraryManager(libraryURL, plugin);

            Library.Builder builder = Library.builder()
                    .groupId(groupID)
                    .artifactId(artifactID)
                    .version(version)
                    .resolveTransitiveDependencies(resolveTransitiveDependencies);

            switch (caseKey) {
                case "ID+RELOC":
                    builder.loaderId(ID).relocate(relocatePattern, relocateRelocatedPattern);
                    break;
                case "ID":
                    builder.loaderId(ID);
                    break;
                case "PLAIN+RELOC":
                    builder.relocate(relocatePattern, relocateRelocatedPattern);
                    break;
                case "PLAIN":
                default:
                    break;
            }

            if (isIsolated) {
                if (!hasId) {
                    builder.loaderId(artifactID + "-isolated");
                }
                builder.isolatedLoad(true);
            }

            final Library lib = builder.build();
            plugin.debugMessage("Loading library " + libLabel +
                    (isIsolated ? " [isolated]" : "") +
                    (doRelocate ? " [relocated]" : "") +
                    ((hasId || isIsolated) ? " (loaderId=" + lib.getLoaderId() + ")" : ""), 1);

            libraryManager.loadLibrary(lib);

            if (isIsolated && doRelocate) {
                try {
                    final String probe = relocateRelocatedPattern.replace("{}", ".");
                    Class.forName(probe, false, Thread.currentThread().getContextClassLoader());
                    plugin.debugMessage("Verified shaded library " + libLabel + ".", 1);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Shaded verification failed for " + libLabel + ": " +
                            e.getClass().getSimpleName() + " - " + e.getMessage());
                    plugin.logStackTrace(e);
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
            } else {
                plugin.debugMessage("Loaded library " + libLabel + ".", 1);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load " + libLabel + ": " +
                    e.getClass().getSimpleName() + " - " + e.getMessage());
            plugin.logStackTrace(e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private static @NotNull LibraryManager getLibraryManager(String libraryURL, Graves plugin) {
        final LibraryManager libraryManager = new BukkitLibraryManager(plugin);
        if (libraryURL != null && !libraryURL.isBlank()) {
            libraryManager.addRepository(libraryURL);
        }
        libraryManager.addMavenCentral();
        libraryManager.addSonatype();
        libraryManager.addJCenter();
        libraryManager.addJitPack();
        return libraryManager;
    }

    private static boolean isDoRelocate(String version, String relocatePattern, String relocateRelocatedPattern) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version is required and cannot be blank.");
        }

        final boolean hasRelocatePattern = relocatePattern != null && !relocatePattern.isBlank();
        final boolean hasRelocateTarget = relocateRelocatedPattern != null && !relocateRelocatedPattern.isBlank();

        if (hasRelocatePattern ^ hasRelocateTarget) {
            throw new IllegalArgumentException("If relocation is used, both relocatePattern and relocateRelocatedPattern must be provided.");
        }

        return hasRelocatePattern;
    }

}