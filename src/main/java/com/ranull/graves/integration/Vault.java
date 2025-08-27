package com.ranull.graves.integration;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * Provides an integration with Vault's permission system to manage player permissions.
 */
public final class Vault {
    private final Permission permission;

    /**
     * Constructs a new Vault integration instance with the specified Permission instance.
     *
     * @param permission The Permission instance provided by Vault.
     * @deprecated
     */
    @Deprecated
    public Vault(Permission permission) {
        this.permission = permission;
    }

    /**
     * Checks if a player has the specified permission.
     *
     * @param player The player whose permission to check.
     * @param permissionNode The permission node to check.
     * @return {@code true} if the player has the specified permission, otherwise {@code false}.
     */
    public boolean hasPermission(OfflinePlayer player, String permissionNode) {
        return permission.has((CommandSender) player, permissionNode);
    }
}