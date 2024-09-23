package com.ranull.graves.util;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.ranull.graves.Graves;
import org.bukkit.Bukkit;


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
     * Loads a library with the specified group ID, artifact ID, and version.
     * <p>
     * Uses default settings for relocation, ID, and isolation.
     * </p>
     *
     * @param groupID    The group ID of the library.
     * @param artifactID The artifact ID of the library.
     * @param version    The version of the library.
     */
    public void loadLibrary(String groupID, String artifactID, String version) {
        loadLibrary(groupID, artifactID, version, null, null, null, false, null, true);
    }

    /**
     * Loads a library with the specified group ID, artifact ID, version, and isolation setting.
     * <p>
     * Uses default settings for relocation and ID.
     * </p>
     *
     * @param groupID    The group ID of the library.
     * @param artifactID The artifact ID of the library.
     * @param version    The version of the library.
     * @param isIsolated Whether to load the library in an isolated class loader.
     */
    public void loadLibrary(String groupID, String artifactID, String version, boolean isIsolated) {
        loadLibrary(groupID, artifactID, version, null, null, null, isIsolated, null, true);
    }

    /**
     * Loads a library with the specified group ID, artifact ID, version, relocation patterns, and isolation setting.
     * <p>
     * Uses default settings for ID.
     * </p>
     *
     * @param groupID                   The group ID of the library.
     * @param artifactID                The artifact ID of the library.
     * @param version                   The version of the library.
     * @param relocatePattern           The package pattern to relocate.
     * @param relocateRelocatedPattern  The relocated package pattern.
     * @param isIsolated                Whether to load the library in an isolated class loader.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, null, true);
    }

    /**
     * Loads a library with the specified group ID, artifact ID, version, relocation patterns, and isolation setting.
     * <p>
     * Uses default settings for ID.
     * </p>
     *
     * @param groupID                       The group ID of the library.
     * @param artifactID                    The artifact ID of the library.
     * @param version                       The version of the library.
     * @param relocatePattern               The package pattern to relocate.
     * @param relocateRelocatedPattern      The relocated package pattern.
     * @param isIsolated                    Whether to load the library in an isolated class loader.
     * @param resolveTransitiveDependencies Determines whether to resolve Transitive Dependencies.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, boolean resolveTransitiveDependencies) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, null, resolveTransitiveDependencies);
    }

    /**
     * Loads a library with the specified group ID, artifact ID, version, relocation patterns, and isolation setting.
     * <p>
     * Uses default settings for ID.
     * </p>
     *
     * @param groupID                   The group ID of the library.
     * @param artifactID                The artifact ID of the library.
     * @param version                   The version of the library.
     * @param relocatePattern           The package pattern to relocate.
     * @param relocateRelocatedPattern  The relocated package pattern.
     * @param isIsolated                Whether to load the library in an isolated class loader.
     * @param libraryURL                Points to an external library URL to a repository.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, String libraryURL) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, libraryURL, true);
    }

    /**
     * Loads a library with the specified group ID, artifact ID, version, relocation patterns, and isolation setting.
     * <p>
     * Uses default settings for ID.
     * </p>
     *
     * @param groupID                       The group ID of the library.
     * @param artifactID                    The artifact ID of the library.
     * @param version                       The version of the library.
     * @param relocatePattern               The package pattern to relocate.
     * @param relocateRelocatedPattern      The relocated package pattern.
     * @param isIsolated                    Whether to load the library in an isolated class loader.
     * @param libraryURL                    Points to an external library URL to a repository.
     * @param resolveTransitiveDependencies Determines whether to resolve Transitive Dependencies.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, String libraryURL, boolean resolveTransitiveDependencies) {
        loadLibrary(groupID, artifactID, version, null, relocatePattern, relocateRelocatedPattern, isIsolated, libraryURL, resolveTransitiveDependencies);
    }


    /**
     * Loads a library with the specified group ID, artifact ID, version, ID, relocation patterns, and isolation setting.
     * <p>
     * Configures the library with optional ID and relocation settings, and loads it using the BukkitLibraryManager.
     * </p>
     *
     * @param groupID                       The group ID of the library.
     * @param artifactID                    The artifact ID of the library.
     * @param version                       The version of the library.
     * @param ID                            Optional ID for the library.
     * @param relocatePattern               Optional package pattern to relocate.
     * @param relocateRelocatedPattern      Optional relocated package pattern.
     * @param isIsolated                    Whether to load the library in an isolated class loader.
     * @param libraryURL                    Points to an external library URL to a repository.
     * @param resolveTransitiveDependencies Determines whether to resolve Transitive Dependencies.
     */
    public void loadLibrary(String groupID, String artifactID, String version, String ID, String relocatePattern, String relocateRelocatedPattern, boolean isIsolated, String libraryURL, boolean resolveTransitiveDependencies) {
        try {
            LibraryManager libraryManager = new BukkitLibraryManager(plugin);
            if (libraryURL != null) {
                libraryManager.addRepository(libraryURL);
            } else {
                libraryManager.addMavenCentral();
                libraryManager.addSonatype();
                libraryManager.addJCenter();
                libraryManager.addJitPack();
            }
            libraryManager.getRepositories();
            if (ID != null) {
                plugin.getLogger().info("Loading library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " with ID " + ID + ".");
                if (relocatePattern != null) {
                    libraryManager.loadLibrary(Library.builder()
                            .groupId(groupID)
                            .artifactId(artifactID)
                            .version(version)
                            .loaderId(ID)
                            .relocate(relocatePattern, relocateRelocatedPattern)
                            .isolatedLoad(isIsolated)
                            .resolveTransitiveDependencies(resolveTransitiveDependencies)
                            .build()
                    );
                    if (isIsolated) {
                        try {
                            Class<?> clazz = Class.forName(relocateRelocatedPattern.replace("{}", "."));
                            clazz.getClassLoader();
                            plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " and shaded successfully with ID " + ID + ".");
                        } catch (ClassNotFoundException e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                            e.printStackTrace();
                        }
                    } else {
                        plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " and shaded successfully with ID " + ID + ".");
                    }
                } else {
                    libraryManager.loadLibrary(Library.builder()
                            .groupId(groupID)
                            .artifactId(artifactID)
                            .version(version)
                            .loaderId(ID)
                            .isolatedLoad(isIsolated)
                            .resolveTransitiveDependencies(resolveTransitiveDependencies)
                            .build()
                    );
                    if (isIsolated) {
                        try {
                            Class<?> clazz = Class.forName(relocateRelocatedPattern.replace("{}", "."));
                            clazz.getClassLoader();
                            plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " successfully with ID " + ID + ".");
                        } catch (ClassNotFoundException e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                            e.printStackTrace();
                        }
                    } else {
                        plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " successfully with ID " + ID + ".");
                    }
                }
            } else {
                plugin.getLogger().info("Loading library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + ".");
                if (relocatePattern != null) {
                    libraryManager.loadLibrary(Library.builder()
                            .groupId(groupID)
                            .artifactId(artifactID)
                            .version(version)
                            .relocate(relocatePattern, relocateRelocatedPattern)
                            .isolatedLoad(isIsolated)
                            .resolveTransitiveDependencies(resolveTransitiveDependencies)
                            .build()
                    );
                    if (isIsolated) {
                        try {
                            Class<?> clazz = Class.forName(relocateRelocatedPattern.replace("{}", "."));
                            clazz.getClassLoader();
                            plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " and shaded successfully.");
                        } catch (ClassNotFoundException e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                            e.printStackTrace();
                        }
                    } else {
                        plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " and shaded successfully.");
                    }
                } else {
                    libraryManager.loadLibrary(Library.builder()
                            .groupId(groupID)
                            .artifactId(artifactID)
                            .version(version)
                            .isolatedLoad(isIsolated)
                            .resolveTransitiveDependencies(resolveTransitiveDependencies)
                            .build()
                    );
                    if (isIsolated) {
                        try {
                            Class<?> clazz = Class.forName(relocateRelocatedPattern.replace("{}", "."));
                            clazz.getClassLoader();
                            plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " successfully.");
                        } catch (ClassNotFoundException e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Shaded library could not be loaded.");
                            e.printStackTrace();
                        }
                    } else {
                        plugin.getLogger().info("Loaded library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + " successfully.");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to download or load library " + groupID.replace("{}", ".") + "." + artifactID + " version " + version + ". Cause: " + e.getCause());
            plugin.logStackTrace(e);
        }
    }
}