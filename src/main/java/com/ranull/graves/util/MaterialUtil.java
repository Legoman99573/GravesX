package com.ranull.graves.util;

import org.bukkit.Material;

/**
 * Utility class for handling various material-related operations.
 */
public final class MaterialUtil {

    /**
     * Checks if the given material is an air block.
     *
     * @param material The material to check.
     * @return True if the material is air, false otherwise.
     */
    public static boolean isAir(Material material) {
        return isAir(material.name());
    }

    /**
     * Checks if the given string represents an air block.
     *
     * @param string The string to check.
     * @return True if the string represents air, false otherwise.
     */
    public static boolean isAir(String string) {
        switch (string) {
            case "AIR":
            case "CAVE_AIR":
            case "VOID_AIR":
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the given material is lava.
     *
     * @param material The material to check.
     * @return True if the material is lava, false otherwise.
     */
    public static boolean isLava(Material material) {
        return isLava(material.name());
    }

    /**
     * Checks if the given string represents lava.
     *
     * @param string The string to check.
     * @return True if the string represents lava, false otherwise.
     */
    public static boolean isLava(String string) {
        switch (string) {
            case "LAVA":
            case "STATIONARY_LAVA":
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the given material is not solid and is safe (i.e., not lava).
     *
     * @param material The material to check.
     * @return True if the material is not solid and safe, false otherwise.
     */
    public static boolean isSafeNotSolid(Material material) {
        return !isSolid(material) && !isLava(material);
    }

    /**
     * Checks if the given material is solid and safe (i.e., not lava).
     *
     * @param material The material to check.
     * @return True if the material is solid and safe, false otherwise.
     */
    public static boolean isSafeSolid(Material material) {
        return isSolid(material) && !isLava(material);
    }

    /**
     * Checks if the given material is solid.
     *
     * @param material The material to check.
     * @return True if the material is solid, false otherwise.
     */
    private static boolean isSolid(Material material) {
        return material.isSolid() || isSafe(material);
    }

    /**
     * Checks if the given material is considered safe.
     *
     * @param material The material to check.
     * @return True if the material is safe, false otherwise.
     */
    private static boolean isSafe(Material material) {
        return isSafe(material.name());
    }

    /**
     * Checks if the given string represents a safe material.
     *
     * @param string The string to check.
     * @return True if the string represents a safe material, false otherwise.
     */
    private static boolean isSafe(String string) {
        switch (string) {
            case "SCAFFOLDING":
            case "POWDER_SNOW":
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the given material is water.
     *
     * @param material The material to check.
     * @return True if the material is water, false otherwise.
     */
    public static boolean isWater(Material material) {
        return isWater(material.name());
    }

    /**
     * Checks if the given string represents water.
     *
     * @param string The string to check.
     * @return True if the string represents water, false otherwise.
     */
    public static boolean isWater(String string) {
        switch (string) {
            case "WATER":
            case "STATIONARY_WATER":
                return true;
            default:
                return false;
        }
    }

    /**
     * @deprecated
     * <p>
     * This method is deprecated and will be removed in a future version.
     * Use {@link #isPlayerHead(String)} instead.
     * </p>
     *
     * Checks if the given material is a player head.
     *
     * @param material The material to check.
     * @return True if the material is a player head, false otherwise.
     */
    @Deprecated
    public static boolean isPlayerHead(Material material) {
        return isPlayerHead(material.name());
    }

    /**
     * Checks if the given string represents a player head.
     *
     * @param string The string to check.
     * @return True if the string represents a player head, false otherwise.
     */
    public static boolean isPlayerHead(String string) {
        switch (string) {
            case "PLAYER_HEAD":
            case "SKULL":
                return true;
            default:
                return false;
        }
    }
}