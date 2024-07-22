package com.ranull.graves.util;

import org.bukkit.entity.Entity;

/**
 * Utility class for handling entity-related operations.
 */
public final class EntityUtil {

    /**
     * Checks if an entity has a specific permission.
     *
     * @param entity     The entity to check.
     * @param permission The permission to check for.
     * @return {@code true} if the entity has the specified permission, {@code true} if the method is not found, or {@code false} if an exception occurs.
     */
    public static boolean hasPermission(Entity entity, String permission) {
        try {
            return entity.hasPermission(permission);
        } catch (NoSuchMethodError ignored) {
        }

        return true;
    }
}