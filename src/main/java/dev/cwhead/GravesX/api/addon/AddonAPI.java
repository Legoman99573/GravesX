package dev.cwhead.GravesX.api.addon;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.addon.GravesXAddon;
import org.bukkit.plugin.Plugin;

/**
 * Addon helper API.
 */
public final class AddonAPI {
    private final Graves plugin;

    public AddonAPI(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Ensures creation of an addon folder.
     * @param addon The addon to register
     */
    public void ensureAddonFolder(Plugin addon) {
        GravesXAddon.ensureAddonFolder(plugin, addon.getDescription().getName());
    }

    /**
     * exports addon configs
     * @param addon the addon to get configs from
     * @return the addons exported
     */
    public int exportAddonConfigs(Plugin addon) {
        return GravesXAddon.exportAddonConfigs(plugin, addon.getDescription().getName(), false);
    }

    /**
     * exports addon configs
     * @param addon the addon to get configs from
     * @param replaceIfExists replace configs even if they exist
     * @return the addons exported
     */
    public int exportAddonConfigs(Plugin addon, boolean replaceIfExists) {
        return GravesXAddon.exportAddonConfigs(plugin, addon.getDescription().getName(), replaceIfExists);
    }
}
