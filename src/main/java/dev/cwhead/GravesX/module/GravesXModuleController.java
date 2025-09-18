package dev.cwhead.GravesX.module;

import java.util.Collection;
import java.util.List;

/**
 * Runtime lifecycle controller for GravesX modules.
 *
 * <p>Implementations are provided by the host (manager/controller). The controller exposed
 * to a module via {@link ModuleContext#getGravesXModules()} can:</p>
 *
 * <ul>
 *   <li>Query enablement state for itself or other modules</li>
 *   <li>Enable/disable itself (current module)</li>
 *   <li>Enable/disable another module by key (module.yml name, simple class name, or FQCN)</li>
 *   <li>Access read-only module descriptors and enumerate all modules</li>
 * </ul>
 *
 * <p>Unless otherwise noted, enable/disable operations are idempotent:
 * invoking them when the target is already in the requested state returns {@code true}
 * (no-op) and does not throw.</p>
 */
public interface GravesXModuleController {
    /**
     * Reports whether <strong>this</strong> (current) module is enabled.
     *
     * @return {@code true} if the current module is enabled; {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Reports whether a target module is enabled.
     *
     * <p>The {@code moduleKey} can be:
     * <ul>
     *   <li>the module.yml {@code name}</li>
     *   <li>the module's simple class name</li>
     *   <li>the module's fully qualified class name (FQCN)</li>
     * </ul>
     * Matching is implementation-defined (typically case-insensitive).</p>
     *
     * @param moduleKey identifier for the target module
     * @return {@code true} if the target module is enabled; {@code false} if disabled or not found
     */
    boolean isEnabled(String moduleKey);

    /**
     * Disables <strong>this</strong> (current) module.
     *
     * <p>Idempotent: if already disabled, no action is taken.</p>
     */
    void disableModule();

    /**
     * Disables a target module identified by key.
     *
     * <p>Idempotent: returns {@code true} if the module becomes or was already disabled.</p>
     *
     * @param moduleKey identifier for the target module (module.yml name, simple class name, or FQCN)
     * @return {@code true} if state changed or the module was already disabled; {@code false} if not found
     */
    boolean disableModule(String moduleKey);

    /**
     * Enables <strong>this</strong> (current) module.
     *
     * <p>Idempotent: if already enabled, no action is taken.</p>
     */
    void enableModule();

    /**
     * Enables a target module identified by key.
     *
     * <p>Idempotent: returns {@code true} if the module becomes or was already enabled.</p>
     *
     * @param moduleKey identifier for the target module (module.yml name, simple class name, or FQCN)
     * @return {@code true} if state changed or the module was already enabled; {@code false} if not found
     */
    boolean enableModule(String moduleKey);

    // ---------- Metadata access ----------

    /**
     * Returns the descriptor for a target module identified by key.
     *
     * <p>The descriptor provides read-only metadata parsed from {@code module.yml}
     * (name, version, authors, dependencies, etc.) plus runtime state.</p>
     *
     * @param moduleKey identifier for the target module (module.yml name, simple class name, or FQCN)
     * @return a descriptor, or {@code null} if the module is unknown
     */
    GravesXModuleDescriptor getModule(String moduleKey);

    /**
     * Returns the descriptor for <strong>this</strong> (current) module.
     *
     * @return a non-null descriptor for the current module
     */
    GravesXModuleDescriptor getThisModule();

    /**
     * Returns descriptors for all discovered modules, regardless of enablement.
     *
     * @return a collection view of all module descriptors (may be empty, never {@code null})
     */
    Collection<GravesXModuleDescriptor> listModules();

    // ---------- Convenience accessors ----------

    /**
     * Convenience accessor for {@code getModule(moduleKey).getName()}.
     *
     * @param moduleKey identifier for the target module
     * @return the module name, or {@code null} if not found
     */
    default String getName(String moduleKey) {
        GravesXModuleDescriptor d = getModule(moduleKey);
        return d != null ? d.getName() : null;
    }

    /**
     * Convenience accessor for {@code getModule(moduleKey).getVersion()}.
     *
     * @param moduleKey identifier for the target module
     * @return the version string, or {@code null} if not found or unspecified
     */
    default String getVersion(String moduleKey) {
        GravesXModuleDescriptor d = getModule(moduleKey);
        return d != null ? d.getVersion() : null;
    }

    /**
     * Convenience accessor for {@code getModule(moduleKey).getWebsite()}.
     *
     * @param moduleKey identifier for the target module
     * @return the website URL, or {@code null} if not found or unspecified
     */
    default String getWebsite(String moduleKey) {
        GravesXModuleDescriptor d = getModule(moduleKey);
        return d != null ? d.getWebsite() : null;
    }

    /**
     * Convenience accessor for {@code getModule(moduleKey).getAuthors()}.
     *
     * @param moduleKey identifier for the target module
     * @return the authors list, {@code List.of()} if not found or unspecified (never {@code null})
     */
    default List<String> getAuthors(String moduleKey) {
        GravesXModuleDescriptor d = getModule(moduleKey);
        return d != null ? d.getAuthors() : List.of();
    }
}