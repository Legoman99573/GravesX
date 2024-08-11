package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;

public class CoreProtectIntegration {
    private final Graves plugin;
    private CoreProtectAPI coreProtectAPI;

    public CoreProtectIntegration(Graves plugin) {
        this.plugin = plugin;
    }

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