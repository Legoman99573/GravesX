package dev.cwhead.GravesX.api;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.api.addon.AddonAPI;
import dev.cwhead.GravesX.api.grave.GraveCreationAPI;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.api.inventory.InventoryAPI;
import dev.cwhead.GravesX.api.permission.PermissionAPI;
import dev.cwhead.GravesX.api.skin.SkinAPI;
import dev.cwhead.GravesX.api.util.UtilAPI;
import dev.cwhead.GravesX.api.world.LocationAPI;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Facade exposing the modular GravesX APIs.
 * Obtain from your plugin on enable and keep a reference.
 */
public class GravesXAPI {
    private final Graves plugin;

    /** Grave creation operations. */
    public final GraveCreationAPI gravesCreate;

    /** Grave management operations. */
    public final GraveManagementAPI gravesManage;

    /** World/location helpers. */
    public final LocationAPI world;

    /** Inventory helpers. */
    public final InventoryAPI inventory;

    /** Skin/texture helpers. */
    public final SkinAPI skin;

    /** Addon helpers. */
    public final AddonAPI addon;

    /** Permission helpers. */
    public final PermissionAPI permission;

    /** Utilities (permissions, XP, colors, files, YAML, paste, etc.). */
    public final UtilAPI util;

    /**
     * Initializes the GravesXAPI facade with all modular API components.
     * <p>
     * This constructor sets up and wires together the modular API classes:
     * </p>
     * <ul>
     *   <li>{@link LocationAPI} – helpers for world and location utilities</li>
     *   <li>{@link UtilAPI} – general-purpose helpers (Base64, colors, XP, permissions, etc.)</li>
     *   <li>{@link InventoryAPI} – helpers for inventories, conversions, and equipping players</li>
     *   <li>{@link SkinAPI} – helpers for skins, skulls, and player textures</li>
     *   <li>{@link AddonAPI} – helpers for managing addons and their configuration</li>
     *   <li>{@link PermissionAPI} – helpers for permission checks via the active permissions provider</li>
     *   <li>{@link GraveManagementAPI} – operations for managing existing graves</li>
     *   <li>{@link GraveCreationAPI} – operations for creating new graves</li>
     * </ul>
     *
     * <p>
     * Each sub-API is created and linked to the given {@link Graves} plugin instance.
     * Consumers should obtain this facade once (e.g., in {@code onEnable()})
     * and re-use it for accessing all modular APIs.
     * </p>
     *
     * @param plugin The active {@link Graves} plugin instance used to initialize APIs
     */
    public GravesXAPI(@NotNull Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");

        this.world = new LocationAPI(plugin);
        this.util = new UtilAPI(plugin, world);
        this.inventory = new InventoryAPI(plugin, util);
        this.skin = new SkinAPI();
        this.addon = new AddonAPI(plugin);
        this.permission = new PermissionAPI(plugin);
        this.gravesManage = new GraveManagementAPI(plugin);
        this.gravesCreate = new GraveCreationAPI(plugin, world, util, gravesManage);
    }

    /**
     * Provides access to world and location utility methods.
     * <p>
     * This includes helpers for converting between {@link org.bukkit.Location}
     * and string representations, rounding coordinates, working with chunks,
     * and simplifying {@link org.bukkit.block.BlockFace} values.
     * </p>
     *
     * @return the {@link LocationAPI} instance
     */
    public @NotNull LocationAPI getLocationAPI() {
        return world;
    }

    /**
     * Provides access to general-purpose utility methods.
     * <p>
     * This includes helpers for Base64 encoding/decoding, colors, particles,
     * permissions, experience calculations, materials, resources, files,
     * YAML validation, reflection, and version updates.
     * </p>
     *
     * @return the {@link UtilAPI} instance
     */
    public @NotNull UtilAPI getUtilAPI() {
        return util;
    }

    /**
     * Provides access to inventory utility methods.
     * <p>
     * This includes helpers for converting inventories to and from strings,
     * equipping players with armor or items, and determining valid inventory sizes.
     * </p>
     *
     * @return the {@link InventoryAPI} instance
     */
    public @NotNull InventoryAPI getInventoryAPI() {
        return inventory;
    }

    /**
     * Provides access to skin and texture utility methods.
     * <p>
     * This includes helpers for retrieving and applying player skin textures,
     * setting skull textures, and accessing {@link com.mojang.authlib.GameProfile}
     * information for players.
     * </p>
     *
     * @return the {@link SkinAPI} instance
     */
    public @NotNull SkinAPI getSkinAPI() {
        return skin;
    }

    /**
     * Provides access to addon management utility methods.
     * <p>
     * This includes ensuring addon folders exist and exporting
     * addon configuration files from packaged resources.
     * </p>
     *
     * @return the {@link AddonAPI} instance
     */
    public @NotNull AddonAPI getAddonAPI() {
        return addon;
    }

    /**
     * Provides access to permission checks via {@link PermissionAPI}.
     *
     * @return the {@link PermissionAPI} instance
     */
    public @NotNull PermissionAPI getPermissionAPI() {
        return permission;
    }

    /**
     * Provides access to grave management utility methods.
     * <p>
     * This includes removing, breaking, looting, abandoning,
     * and counting graves, as well as proximity checks.
     * </p>
     *
     * @return the {@link GraveManagementAPI} instance
     */
    public @NotNull GraveManagementAPI getGravesManagementAPI() {
        return gravesManage;
    }

    /**
     * Provides access to grave creation utility methods.
     * <p>
     * This includes creating new graves with various options such as
     * equipment, items, experience, locations, protection, and custom causes.
     * </p>
     *
     * @return the {@link GraveCreationAPI} instance
     */
    public @NotNull GraveCreationAPI getGravesCreationAPI() {
        return gravesCreate;
    }

    /**
     * Returns the underlying {@link Graves} plugin instance.
     *
     * <p>This is intended for advanced use-cases where direct access to internal managers
     * or server state is necessary. Most consumers should prefer the modular sub-APIs
     * exposed by this facade.</p>
     *
     * @return the active Graves plugin instance
     */
    @ApiStatus.Experimental
    public @NotNull Graves plugin() {
        return plugin;
    }
}