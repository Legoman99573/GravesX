package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;

/**
 * Handles integration with the CoreProtect plugin.
 *
 * This class provides methods to obtain and interact with the CoreProtect API.
 * It checks the CoreProtect plugin's availability and API version to ensure compatibility.
 *
 */
public class CoreProtectIntegration {
    private final Graves plugin;
    private CoreProtectAPI coreProtectAPI;

    /**
     * Constructs a new {@code CoreProtectIntegration}.
     *
     * @param plugin the {@link Graves} plugin instance
     */
    public CoreProtectIntegration(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Retrieves the CoreProtect API instance.
     *
     * If the API instance is not already cached, it attempts to obtain it by checking if the CoreProtect plugin is
     * available, enabled, and if the API version is at least 9. If the CoreProtect plugin is not available or
     * the API version is insufficient, this method will log an error and return {@code null}.
     *
     * @return the {@link CoreProtectAPI} instance, or {@code null} if CoreProtect is unavailable or incompatible
     */
    public CoreProtectAPI getCoreProtectAPI() {
        if (coreProtectAPI != null) {
            return coreProtectAPI;
        }

        Plugin coreProtectPlugin = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
        if (coreProtectPlugin != null && coreProtectPlugin instanceof CoreProtect) {
            CoreProtect coreProtect = (CoreProtect) coreProtectPlugin;
            if (coreProtect.isEnabled() && coreProtect.getAPI().APIVersion() >= 9) {
                coreProtectAPI = coreProtect.getAPI();
            } else {
                plugin.getLogger().severe("CoreProtect is using API version " + coreProtect.getAPI().APIVersion() + ". Graves will not be logged.");
            }
        }
        return coreProtectAPI;
    }
}