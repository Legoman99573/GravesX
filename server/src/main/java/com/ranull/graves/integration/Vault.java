package com.ranull.graves.integration;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides an integration with Vault's permission system to manage player permissions.
 */
public class Vault {
    private final Permission permission;

    /**
     * Constructs a new Vault integration instance with the specified Permission instance.
     *
     * @param permission The Permission instance provided by Vault.
     * @deprecated Use a central permissions adapter if available.
     */
    @Deprecated
    public Vault(Permission permission) {
        this.permission = permission;
    }

    /**
     * Checks if an {@link OfflinePlayer} has the specified permission.
     *
     * @param player         The (possibly offline) player to check.
     * @param permissionNode The permission node to check.
     * @return {@code true} if the player has the specified permission, otherwise {@code false}.
     */
    public boolean hasPermission(OfflinePlayer player, String permissionNode) {
        if (player == null || permission == null || permissionNode == null) {
            return false;
        }

        try {
            String world = null;
            Player online = player.getPlayer();
            if (online != null) {
                online.getWorld();
                world = online.getWorld().getName();
            }
            return permission.playerHas(world, player, permissionNode);
        } catch (NoSuchMethodError ignored) {
            Player online = player.getPlayer();
            if (online != null) {
                return permission.has(online, permissionNode);
            }
            return false;
        }
    }
}