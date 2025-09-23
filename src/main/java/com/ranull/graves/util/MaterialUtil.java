package com.ranull.graves.util;

import org.bukkit.Material;

import java.util.Set;

/**
 * Utility class for handling various material-related operations across MC 1.8–1.21.x.
 * <p>
 * Uses string-name checks so it can be compiled against any one API version while
 * recognizing legacy and modern material names at runtime.
 */
public final class MaterialUtil {

    private MaterialUtil() {
    }

    private static final Set<String> AIR_NAMES = Set.of(
            "AIR",
            "CAVE_AIR",
            "VOID_AIR",
            "LEGACY_AIR",
            "LEGACY_CAVE_AIR",
            "LEGACY_VOID_AIR"
    );

    private static final Set<String> WATER_NAMES = Set.of(
            "WATER",
            "STATIONARY_WATER",
            "LEGACY_WATER",
            "LEGACY_STATIONARY_WATER"
    );

    private static final Set<String> LAVA_NAMES = Set.of(
            "LAVA",
            "STATIONARY_LAVA",
            "LEGACY_LAVA",
            "LEGACY_STATIONARY_LAVA"
    );

    private static final Set<String> PLAYER_HEAD_NAMES = Set.of(
            "PLAYER_HEAD",
            "PLAYER_WALL_HEAD",
            "SKULL",
            "SKULL_ITEM",
            "LEGACY_PLAYER_HEAD",
            "LEGACY_PLAYER_WALL_HEAD",
            "LEGACY_SKULL",
            "LEGACY_SKULL_ITEM"
    );

    private static final Set<String> SAFE_SUPPORTS = Set.of(
            "SCAFFOLDING",
            "POWDER_SNOW",
            "WEB",
            "COBWEB",
            "SWEET_BERRY_BUSH",
            "HONEY_BLOCK",
            "TRIPWIRE",
            "TRIPWIRE_HOOK",
            "LEGACY_WEB",
            "LEGACY_COBWEB",
            "LEGACY_SWEET_BERRY_BUSH",
            "LEGACY_HONEY_BLOCK",
            "LEGACY_TRIPWIRE",
            "LEGACY_TRIPWIRE_HOOK"
    );

    /**
     * Checks if the given material is an air block.
     */
    public static boolean isAir(Material material) {
        return isAir(material.name());
    }

    /**
     * Checks if the given string represents an air block.
     */
    public static boolean isAir(String name) {
        return AIR_NAMES.contains(name);
    }

    /**
     * Checks if the given material is lava.
     */
    public static boolean isLava(Material material) {
        return isLava(material.name());
    }

    /**
     * Checks if the given string represents lava.
     */
    public static boolean isLava(String name) {
        return LAVA_NAMES.contains(name);
    }

    /**
     * Checks if the given material is not solid and is safe (i.e., not lava).
     */
    public static boolean isSafeNotSolid(Material material) {
        return !isSolid(material) && !isLava(material);
    }

    /**
     * Checks if the given material is solid and safe (i.e., not lava).
     */
    public static boolean isSafeSolid(Material material) {
        return isSolid(material) && !isLava(material);
    }

    /**
     * Treats certain non-solid “supports” as solid for our purposes.
     */
    private static boolean isSolid(Material material) {
        return material.isSolid() || isSafeSupport(material);
    }

    private static boolean isSafeSupport(Material material) {
        return SAFE_SUPPORTS.contains(material.name());
    }

    /**
     * Checks if the given material is water.
     */
    public static boolean isWater(Material material) {
        return isWater(material.name());
    }

    /**
     * Checks if the given string represents water.
     */
    public static boolean isWater(String name) {
        return WATER_NAMES.contains(name);
    }

    /**
     * Checks if the given material is a player head (block or item), across versions.
     */
    public static boolean isPlayerHead(Material material) {
        return isPlayerHead(material.name());
    }

    /**
     * Checks if the given string represents a player head (block or item), across versions.
     */
    public static boolean isPlayerHead(String name) {
        return PLAYER_HEAD_NAMES.contains(name);
    }
}