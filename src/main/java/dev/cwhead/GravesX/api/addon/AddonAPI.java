package dev.cwhead.GravesX.api.addon;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.addon.GravesXAddon;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Addon helper API.
 *
 * <p>Provides convenience methods for common addon lifecycle actions such as
 * ensuring the addon data folder exists and exporting default configuration files.</p>
 *
 * <p><strong>Usage:</strong> construct once with the owning {@link Graves} plugin
 * and call the helper methods from your addon during startup.</p>
 *
 * @since 4.9
 */
public class AddonAPI {
    private final Graves plugin;

    /**
     * Creates a new {@code AddonAPI} bound to the given Graves plugin.
     *
     * @param plugin the owning Graves plugin (must not be {@code null})
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public AddonAPI(Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Ensures creation of an addon folder.
     *
     * <p>If the folder does not exist, it will be created under the Graves
     * addon root using the addon's plugin name.</p>
     *
     * @param addon the addon plugin requesting the folder (must not be {@code null})
     * @throws NullPointerException if {@code addon} is {@code null}
     */
    public void ensureAddonFolder(Plugin addon) {
        Objects.requireNonNull(addon, "addon");
        GravesXAddon.ensureAddonFolder(plugin, addon.getDescription().getName());
    }

    /**
     * Exports the addon's default configuration files if they do not already exist.
     *
     * <p>Existing files are preserved.</p>
     *
     * @param addon the addon plugin whose configs should be exported (must not be {@code null})
     * @return the number of files exported (0 if none)
     * @throws NullPointerException if {@code addon} is {@code null}
     */
    public int exportAddonConfigs(Plugin addon) {
        Objects.requireNonNull(addon, "addon");
        return GravesXAddon.exportAddonConfigs(plugin, addon.getDescription().getName(), false);
    }

    /**
     * Exports the addon's default configuration files with optional replacement.
     *
     * @param addon the addon plugin whose configs should be exported (must not be {@code null})
     * @param replaceIfExists if {@code true}, existing files will be overwritten
     * @return the number of files exported (0 if none)
     * @throws NullPointerException if {@code addon} is {@code null}
     */
    public int exportAddonConfigs(Plugin addon, boolean replaceIfExists) {
        Objects.requireNonNull(addon, "addon");
        return GravesXAddon.exportAddonConfigs(plugin, addon.getDescription().getName(), replaceIfExists);
    }
}