package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated Unmaintained greedware plugin.
 * Handles integration with the CoreProtect plugin.
 * This class provides methods to obtain and interact with the CoreProtect API.
 * It checks the CoreProtect plugin's availability and API version to ensure compatibility.
 *
 */
public class CoreProtectIntegration {
    private final Graves plugin;
    private CoreProtectAPI coreProtectAPI;

    /**
     * @deprecated Unmaintained greedware plugin.
     *
     * Constructs a new {@code CoreProtectIntegration}.
     *
     * @param plugin the {@link Graves} plugin instance
     */
    @Deprecated
    public CoreProtectIntegration(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     *
     * Retrieves the CoreProtect API instance.
     *
     * If the API instance is not already cached, it attempts to obtain it by checking if the CoreProtect plugin is
     * available, enabled, and if the API version is at least 9. If the CoreProtect plugin is not available or
     * the API version is insufficient, this method will log an error and return {@code null}.
     *
     * @return the {@link CoreProtectAPI} instance, or {@code null} if CoreProtect is unavailable or incompatible
     */
    @Deprecated
    public CoreProtectAPI getCoreProtectAPI() {
        if (coreProtectAPI != null) {
            return coreProtectAPI;
        }

        Plugin coreProtectPlugin = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
        if (coreProtectPlugin instanceof CoreProtect coreProtect) {
            CoreProtectAPI api = coreProtect.getAPI();
            if (coreProtect.isEnabled() && api.APIVersion() >= 9) {
                coreProtectAPI = api;
            } else {
                plugin.getLogger().severe(
                        "CoreProtect is using API version " + api.APIVersion() + ". Graves will not be logged."
                );
            }
        }
        return coreProtectAPI;
    }
}