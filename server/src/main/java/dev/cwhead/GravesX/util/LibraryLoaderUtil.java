package dev.cwhead.GravesX.util;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.LibraryManager;
import com.ranull.graves.Graves;
import dev.cwhead.GravesXCommon.libby.LibraryLoader;
import dev.cwhead.GravesXCommon.libby.LibraryLog;
import dev.cwhead.GravesXCommon.libby.LibraryRequest;
import dev.cwhead.GravesXCommon.libby.LoadResult;

import java.util.Objects;

/**
 * Utility class for loading external libraries dynamically using BukkitLibraryManager.
 * <p>
 * The loading itself lives in the shared {@link LibraryLoader} (module {@code gravesx-common}), which the proxy
 * plugins use as well; this class supplies the Bukkit {@link LibraryManager} and logging.
 * </p>
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

        // throws IllegalArgumentException for blank coordinates or a half-specified relocation
        final LibraryRequest request = LibraryRequest.builder()
                .coordinates(groupID, artifactID, version)
                .loaderId(ID)
                .relocate(relocatePattern, relocateRelocatedPattern)
                .isolated(isIsolated)
                .transitive(resolveTransitiveDependencies)
                .build();

        final LibraryLoader loader;
        try {
            loader = new LibraryLoader(new BukkitLibraryManager(plugin), new LibraryLog() {
                @Override
                public void debug(String message) {
                    plugin.debugMessage(message, 1);
                }

                @Override
                public void warn(String message) {
                    plugin.getLogger().warning(message);
                }

                @Override
                public void error(String message, Throwable error) {
                    plugin.getLogger().severe(message);
                    plugin.logStackTrace(error);
                }
            }).useDefaultRepositories(libraryURL);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to construct LibraryManager for " + request.label() + ": " + e.getClass().getSimpleName());
            plugin.logStackTrace(e);
            return;
        }

        if (loader.load(request) == LoadResult.VERIFICATION_FAILED) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }
}
