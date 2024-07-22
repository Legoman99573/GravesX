package com.ranull.graves.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for handling player permissions.
 */
public final class PermissionUtil {

    /**
     * Gets the highest integer value associated with a specific permission prefix.
     *
     * @param player     The player whose permissions are being checked.
     * @param permission The permission prefix to search for.
     * @return The highest integer value found for the specified permission prefix. Returns 0 if no such permission is found.
     */
    public static int getHighestInt(Player player, String permission) {
        List<Integer> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains(permission)) {
                try {
                    gravePermissions.add(Integer.parseInt(perm.getPermission().replace(permission, "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions);
        }

        return 0;
    }

    /**
     * Gets the highest double value associated with a specific permission prefix.
     *
     * @param player     The player whose permissions are being checked.
     * @param permission The permission prefix to search for.
     * @return The highest double value found for the specified permission prefix. Returns 0 if no such permission is found.
     */
    public static double getHighestDouble(Player player, String permission) {
        List<Double> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains(permission)) {
                try {
                    gravePermissions.add(Double.parseDouble(perm.getPermission().replace(permission, "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions);
        }

        return 0;
    }
}