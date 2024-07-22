package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides integration with ProtectionLib to check if a player can build at a specific location.
 */
public final class ProtectionLib {
    private final Graves plugin;
    private final Plugin protectionLibPlugin;

    /**
     * Constructs a new ProtectionLib instance with the specified Graves plugin and ProtectionLib plugin.
     *
     * @param plugin The main Graves plugin instance.
     * @param protectionLibPlugin The ProtectionLib plugin instance.
     */
    public ProtectionLib(Graves plugin, Plugin protectionLibPlugin) {
        this.plugin = plugin;
        this.protectionLibPlugin = protectionLibPlugin;
    }

    /**
     * Checks if a player is allowed to build at the specified location.
     * Uses FurnitureLib integration if available; otherwise, reflects ProtectionLib's method.
     *
     * @param location The location to check.
     * @param player   The player attempting to build.
     * @return True if the player can build at the location, false otherwise.
     */
    public boolean canBuild(Location location, Player player) {
        if (plugin.getIntegrationManager().hasFurnitureLib()) {
            return plugin.getIntegrationManager().getFurnitureLib().canBuild(location, player);
        } else {
            try {
                // Reflectively access ProtectionLib's canBuild method
                Object protectionLib = Class.forName("de.Ste3et_C0st.ProtectionLib.main.ProtectionLib")
                        .cast(protectionLibPlugin);
                Method canBuild = protectionLib.getClass().getMethod("canBuild", location.getClass(),
                        Class.forName("org.bukkit.entity.Player"));

                canBuild.setAccessible(true);

                return (boolean) canBuild.invoke(protectionLib, location, player);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                     | ClassNotFoundException ignored) {
            }
        }

        return true;
    }
}