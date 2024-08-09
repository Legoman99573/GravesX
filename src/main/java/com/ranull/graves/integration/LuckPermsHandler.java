package com.ranull.graves.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Provides an integration with LuckPerms to manage player permissions.
 */
public class LuckPermsHandler {
    private final LuckPerms luckPerms;

    /**
     * Constructs a new LuckPerms integration instance by registering the LuckPerms service.
     * If LuckPerms is not available, an IllegalStateException is thrown.
     */
    public LuckPermsHandler() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            throw new IllegalStateException("LuckPerms is not available on this server.");
        }
    }

    /**
     * Checks if a player has the specified permission.
     *
     * @param offlinePlayer The offline player whose permission to check.
     * @param permissionNode The permission node to check.
     * @return {@code true} if the player has the specified permission, otherwise {@code false}.
     */
    public boolean hasPermission(OfflinePlayer offlinePlayer, String permissionNode) {
        User user = luckPerms.getUserManager().getUser(offlinePlayer.getUniqueId());
        return user != null && user.getCachedData().getPermissionData().checkPermission(permissionNode).asBoolean();
    }

    /**
     * Checks if a player has the specified permission.
     *
     * @param player The player whose permission to check.
     * @param permissionNode The permission node to check.
     * @return {@code true} if the player has the specified permission, otherwise {@code false}.
     */
    public boolean hasPermission(Player player, String permissionNode) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user != null && user.getCachedData().getPermissionData().checkPermission(permissionNode).asBoolean();
    }

    /**
     * Grants the specified permission to a player.
     *
     * @param player The player to whom the permission will be granted.
     * @param permissionNode The permission node to grant.
     * @return {@code true} if the permission was successfully granted, otherwise {@code false}.
     */
    public boolean grantPermission(OfflinePlayer player, String permissionNode) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            Node node = Node.builder(permissionNode).build();
            user.data().add(node);
            luckPerms.getUserManager().saveUser(user);
            return true;
        }
        return false;
    }

    /**
     * Revokes the specified permission from a player.
     *
     * @param player The player from whom the permission will be revoked.
     * @param permissionNode The permission node to revoke.
     * @return {@code true} if the permission was successfully revoked, otherwise {@code false}.
     */
    public boolean revokePermission(OfflinePlayer player, String permissionNode) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            Node node = Node.builder(permissionNode).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user);
            return true;
        }
        return false;
    }

    /**
     * Checks if a player is in the specified group.
     *
     * @param player The player whose group membership to check.
     * @param groupName The name of the group to check.
     * @return {@code true} if the player is in the specified group, otherwise {@code false}.
     */
    public boolean isInGroup(OfflinePlayer player, String groupName) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user != null && user.getPrimaryGroup().equalsIgnoreCase(groupName);
    }
}