package dev.cwhead.GravesX.module;

import dev.cwhead.GravesX.module.util.ModuleInfo;

import java.util.List;

/**
 * Read-only view of a module's metadata (parsed from {@code module.yml})
 * plus its current runtime state.
 *
 * <p>All getters are non-mutating. Optional fields may be {@code null}
 * or an empty list, depending on type.</p>
 *
 * <p>The descriptor also exposes the {@code supportsFolia} flag parsed from
 * {@code module.yml}, which indicates whether the module declares itself safe
 * to run on Folia-based server implementations.</p>
 */
public interface GravesXModuleDescriptor {

    /**
     * Module display name from {@code module.yml:name}.
     *
     * @return module name, never {@code null}
     */
    String getName();

    /**
     * Module version from {@code module.yml:version}.
     *
     * @return version string, or {@code null} if unspecified
     */
    String getVersion();

    /**
     * Human-readable description from {@code module.yml:description}.
     *
     * @return description text, or {@code null} if unspecified
     */
    String getDescription();

    /**
     * Project website or documentation URL from {@code module.yml:website}.
     *
     * @return website URL, or {@code null} if unspecified
     */
    String getWebsite();

    /**
     * Author list from {@code module.yml:authors}.
     *
     * @return immutable list of authors (may be empty)
     */
    List<String> getAuthors();

    /**
     * Fully qualified main class name from {@code module.yml:main}.
     *
     * @return FQCN of the module entrypoint, never {@code null}
     */
    String getMainClass();

    /**
     * Hard module dependencies from {@code module.yml:moduleDepends}.
     * These must be enabled before this module can enable.
     *
     * @return immutable list of required module names (may be empty)
     */
    List<String> getModuleDepends();

    /**
     * Soft module dependencies from {@code module.yml:moduleSoftDepends}.
     * Presence adjusts load order but is not required.
     *
     * @return immutable list of soft-dependency module names (may be empty)
     */
    List<String> getModuleSoftDepends();

    /**
     * Modules that prefer to load after this one from {@code module.yml:moduleLoadBefore}.
     *
     * @return immutable list of module names that should load after this module (may be empty)
     */
    List<String> getModuleLoadBefore();

    /**
     * Required external Bukkit plugins from {@code module.yml:pluginDepends}.
     *
     * @return immutable list of required plugin names (may be empty)
     */
    List<String> getPluginDepends();

    /**
     * Optional external Bukkit plugins from {@code module.yml:pluginSoftDepends}.
     *
     * @return immutable list of soft plugin names (may be empty)
     */
    List<String> getPluginSoftDepends();

    /**
     * Declared load phase from {@code module.yml:load}.
     *
     * <p>This is a declarative hint that tells the host when the module expects
     * to be enabled relative to server/plugin lifecycle.</p>
     *
     * <p>If omitted in {@code module.yml}, the host will default this to
     * {@link GravesXModuleController.LoadPhase#COMPLETED}.</p>
     *
     * @return the declared load phase, never {@code null}
     */
    GravesXModuleController.LoadPhase getLoadPhase();

    /**
     * Libraries declared in {@code module.yml:libraries}.
     *
     * <p>Each entry describes the Maven coordinates plus optional relocation/isolation flags.</p>
     *
     * @return immutable list of library definitions (may be empty)
     */
    List<ModuleInfo.LibraryDef> getLibraries();

    /**
     * Current runtime enablement state.
     *
     * @return {@code true} if this module is enabled, otherwise {@code false}
     */
    boolean isEnabled();

    /**
     * Whether this module declares Folia support via {@code module.yml:supportsFolia}.
     *
     * <p>This is a purely declarative flag: it reflects what the module author
     * configured in {@code module.yml}. The manager may still choose to disable
     * the module at runtime if it detects incompatibilities, but this flag
     * can be used by callers to understand the module's declared intent.</p>
     *
     * <p>If {@code supportsFolia} is omitted in {@code module.yml}, this will
     * typically default to {@code false}.</p>
     *
     * @return {@code true} if the module declares Folia support; {@code false} otherwise
     */
    boolean supportsFolia();
}