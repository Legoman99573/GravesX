package dev.cwhead.GravesX;

import com.mojang.authlib.GameProfile;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.data.LocationData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.util.PluginDownloadUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API for managing graves in the GravesX plugin. The GravesXAPI provides methods to create graves for entities
 * and manage grave creation events.
 * <p>
 * Graves are created with various configurations, including equipment, items, experience, protection, and more.
 * The API also handles event triggering when graves are created and ensures data is stored correctly.
 * </p>
 *
 * @deprecated Since 4.9.9.1, this class is replaced by the modular APIs under
 * {@link dev.cwhead.GravesX.api.GravesXAPI}. Use:
 * <ul>
 *     <li>{@code GravesXApi#gravesCreate} for grave creation</li>
 *     <li>{@code GravesXApi#gravesManage} for management actions</li>
 *     <li>{@code GravesXApi#world} for location/block-face helpers</li>
 *     <li>{@code GravesXApi#inventory} for inventory helpers</li>
 *     <li>{@code GravesXApi#skin} for textures and profiles</li>
 *     <li>{@code GravesXApi#addon} for addon folder/config export</li>
 *     <li>{@code GravesXApi#util} for utilities (permissions, XP, colors, files, YAML, paste, etc.)</li>
 * </ul>
 */
@Deprecated(forRemoval = true, since = "4.9.9.1")
public final class GravesXAPI {

    private final Graves plugin;
    private final dev.cwhead.GravesX.api.GravesXAPI api;

    /**
     * Constructor for initializing the GravesXAPI with the main plugin instance.
     *
     * @param plugin The main Graves plugin instance.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.GravesXAPI#GravesXAPI(Graves)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public GravesXAPI(Graves plugin) {
        this.plugin = plugin;
        this.api = new dev.cwhead.GravesX.api.GravesXAPI(plugin);
    }

    /* ----------------------------------------
     * Grave Creation (original overloads)
     * ---------------------------------------- */

    /**
     * Creates a grave for an entity with the basic parameters.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI#createGrave(Entity, EntityType, long)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType, long timeAliveRemaining) {
        api.gravesCreate.createGrave(victim, killerEntityType, timeAliveRemaining);
    }

    /**
     * Creates a grave for an entity with the basic parameters.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI#createGrave(Entity, EntityType, int, long)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType, int experience, long timeAliveRemaining) {
        api.gravesCreate.createGrave(victim, killerEntityType, experience, timeAliveRemaining);
    }

    /**
     * Creates a grave for an entity with the basic parameters.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param equipmentMap       The equipment the victim had at the time of death.
     * @param itemStackList      The list of items the victim had at the time of death.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList, int experience, long timeAliveRemaining) {
        api.gravesCreate.createGrave(victim, killerEntityType, equipmentMap, itemStackList, experience, timeAliveRemaining);
    }

    /**
     * Creates a grave with additional protection settings.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            boolean graveProtection, long graveProtectionTime) {
        api.gravesCreate.createGrave(victim, killerEntityType, equipmentMap, itemStackList, experience, timeAliveRemaining, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity with a specific storage type.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param equipmentMap       The equipment the victim had at the time of death.
     * @param itemStackList      The list of items the victim had at the time of death.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @param damageCause        Damage Caused (nullable).
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause) {
        api.gravesCreate.createGrave(victim, killerEntityType, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause);
    }

    /**
     * Creates a grave for an entity with a specific storage type and additional protection settings.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param damageCause         Damage Caused (nullable).
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause,
                            boolean graveProtection, long graveProtectionTime) {
        api.gravesCreate.createGrave(victim, killerEntityType, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity at a specific location where the victim died.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param locationDeath      The location where the victim died (nullable).
     * @param equipmentMap       The equipment the victim had at the time of death.
     * @param itemStackList      The list of items the victim had at the time of death.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @param damageCause        Damage Caused (nullable).
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause) {
        api.gravesCreate.createGrave(victim, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause);
    }

    /**
     * Creates a grave for an entity at a specific location with protection settings.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param damageCause         Damage Caused (nullable).
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause,
                            boolean graveProtection, long graveProtectionTime) {
        api.gravesCreate.createGrave(victim, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity at a specific location without a killer and no storage type.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param locationDeath      The location where the victim died (nullable).
     * @param equipmentMap       The equipment the victim had at the time of death.
     * @param itemStackList      The list of items the victim had at the time of death.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining) {
        api.gravesCreate.createGrave(victim, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining);
    }

    /**
     * Creates a grave for an entity killed by another entity.
     *
     * @param victim             The entity that died.
     * @param killer             The entity that killed the victim (nullable).
     * @param killerEntityType   The entity type of the killer.
     * @param locationDeath      The location where the victim died (nullable).
     * @param equipmentMap       The equipment the victim had at the time of death.
     * @param itemStackList      The list of items the victim had at the time of death.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining) {
        api.gravesCreate.createGrave(victim, killer, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining);
    }

    /**
     * Creates a grave for an entity killed by another entity with protection settings.
     *
     * @param victim              The entity that died.
     * @param killer              The entity that killed the victim (nullable).
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     * @deprecated Since 4.9.9.1. Use the equivalent in {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            boolean graveProtection, long graveProtectionTime) {
        // Call once with protection settings (the original implementation did two calls).
        api.gravesCreate.createGrave(
                victim, killer, killerEntityType, locationDeath,
                equipmentMap, itemStackList, experience, timeAliveRemaining,
                null, graveProtection, graveProtectionTime
        );
    }

    /**
     * Main method to create a grave with all available parameters.
     *
     * @param victim              The entity that died.
     * @param killer              The entity that killed the victim (nullable).
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param damageCause         Damage Caused (nullable).
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveCreationAPI#createGrave(Entity, Entity, EntityType, Location, Map, List, int, long, EntityDamageEvent.DamageCause, boolean, long)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath, @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList, int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause, boolean graveProtection, long graveProtectionTime) {
        api.gravesCreate.createGrave(victim, killer, killerEntityType, locationDeath, equipmentMap, itemStackList,
                experience, timeAliveRemaining, damageCause, graveProtection, graveProtectionTime);
    }

    /* ----------------------------------------
     * Management
     * ---------------------------------------- */

    /**
     * Removes the specified grave from the grave manager.
     *
     * @param grave the grave to be removed
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#removeGrave(Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void removeGrave(@NotNull Grave grave) {
        api.gravesManage.removeGrave(grave);
    }

    /**
     * Breaks the specified grave, triggering its removal and handling any related events.
     *
     * @param grave the grave to be broken
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#breakGrave(Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void breakGrave(@NotNull Grave grave) {
        api.gravesManage.breakGrave(grave);
    }

    /**
     * Breaks the specified grave at a given location.
     *
     * @param location the location where the grave is located
     * @param grave    the grave to be broken
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#breakGrave(Location, Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void breakGrave(@NotNull Location location, @NotNull Grave grave) {
        api.gravesManage.breakGrave(location, grave);
    }

    /**
     * Automatically loots the specified grave for the given entity at the given location.
     *
     * @param entity   the entity that will loot the grave
     * @param location the location of the grave
     * @param grave    the grave to be looted
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#autoLootGrave(Entity, Location, Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void autoLootGrave(@NotNull Entity entity, @NotNull Location location, @NotNull Grave grave) {
        api.gravesManage.autoLootGrave(entity, location, grave);
    }

    /**
     * Marks the specified grave as abandoned, preventing further interaction.
     *
     * @param grave the grave to be abandoned
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#abandonGrave(Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void abandonGrave(@NotNull Grave grave) {
        api.gravesManage.abandonGrave(grave);
    }

    /**
     * Drops the items stored in the specified grave at the given location.
     *
     * @param location the location where the items will be dropped
     * @param grave    the grave whose items are to be dropped
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#dropGraveItems(Location, Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void dropGraveItems(@NotNull Location location, @NotNull Grave grave) {
        api.gravesManage.dropGraveItems(location, grave);
    }

    /**
     * Removes the oldest grave associated with the specified living entity.
     *
     * @param livingEntity the entity whose oldest grave will be removed
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#removeOldestGrave(LivingEntity)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void removeOldestGrave(@NotNull LivingEntity livingEntity) {
        api.gravesManage.removeOldestGrave(livingEntity);
    }

    /**
     * Determines if the specified location is near a grave.
     * <p>
     * This method serves as an overload to allow optional parameters such as a player
     * or a block to be included in the proximity check.
     *
     * @param location the location to check for nearby graves (required).
     * @param player   the player to consider in the proximity check (optional; nullable).
     * @param block    the block to consider in the proximity check (optional; nullable).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#isNearGrave(Location, Player, org.bukkit.block.Block)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isNearGrave(@NotNull Location location, @Nullable Player player, @Nullable Block block) {
        return api.gravesManage.isNearGrave(location, player, block);
    }

    /**
     * Determines if the specified location is near a grave.
     * <p>
     * This variant of the method omits the player and block parameters.
     *
     * @param location the location to check for nearby graves (required).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#isNearGrave(Location)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isNearGrave(@NotNull Location location) {
        return api.gravesManage.isNearGrave(location);
    }

    /**
     * Determines if the specified location is near a grave, considering a specific player.
     * <p>
     * This variant of the method includes the player parameter but omits the block parameter.
     *
     * @param location the location to check for nearby graves (required).
     * @param player   the player to consider in the proximity check (required).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#isNearGrave(Location, Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isNearGrave(@NotNull Location location, @NotNull Player player) {
        return api.gravesManage.isNearGrave(location, player);
    }

    /**
     * Determines if the specified location is near a grave, considering a specific block.
     * <p>
     * This variant of the method includes the block parameter but omits the player parameter.
     *
     * @param location the location to check for nearby graves (required).
     * @param block    the block to consider in the proximity check (required).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#isNearGrave(Location, org.bukkit.block.Block)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isNearGrave(@NotNull Location location, @NotNull Block block) {
        return api.gravesManage.isNearGrave(location, block);
    }

    /**
     * Gets the grave type
     *
     * @param uuid the uuid of the grave
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#getGrave(UUID)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Grave getGrave(@NotNull UUID uuid) {
        return api.gravesManage.getGrave(uuid);
    }

    /**
     * Retrieves the BlockData associated with a grave at a given location.
     *
     * @param location        The location of the grave.
     * @param graveUUID       The unique identifier of the grave.
     * @param replaceMaterial The material to replace in the BlockData.
     * @param replaceData     Additional data to apply to the BlockData.
     * @return A BlockData instance representing the grave at the specified location.
     * @deprecated Since 4.9.9.1. Access data classes directly as needed; no replacement API.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public BlockData getBlockData(@NotNull Location location, @NotNull UUID graveUUID,
                                  @NotNull String replaceMaterial, @NotNull String replaceData) {
        return new BlockData(location, graveUUID, replaceMaterial, replaceData);
    }

    /**
     * Retrieves the ChunkData for the chunk containing the specified location.
     *
     * @param location The location for which to retrieve the chunk data.
     * @return A ChunkData instance representing the chunk at the specified location.
     * @deprecated Since 4.9.9.1. Access data classes directly as needed; no replacement API.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public ChunkData getChunkData(@NotNull Location location) {
        return new ChunkData(location);
    }

    /**
     * Retrieves the EntityData for an entity associated with a grave.
     *
     * @param location   The location of the entity.
     * @param uuidEntity The unique identifier of the entity.
     * @param uuidGrave  The unique identifier of the grave.
     * @param type       The type of the entity.
     * @return An EntityData instance representing the entity associated with the grave.
     * @deprecated Since 4.9.9.1. Access data classes directly as needed; no replacement API.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public EntityData getEntityData(@NotNull Location location, @NotNull UUID uuidEntity,
                                    @NotNull UUID uuidGrave, @NotNull EntityData.Type type) {
        return new EntityData(location, uuidEntity, uuidGrave, type);
    }

    /**
     * Retrieves the HologramData for a hologram associated with a grave.
     *
     * @param location   The location of the hologram.
     * @param uuidEntity The unique identifier of the entity associated with the hologram.
     * @param uuidGrave  The unique identifier of the grave.
     * @param line       The line number of the hologram to retrieve.
     * @return A HologramData instance representing the hologram associated with the grave.
     * @deprecated Since 4.9.9.1. Access data classes directly as needed; no replacement API.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public HologramData getHologramData(@NotNull Location location, @NotNull UUID uuidEntity,
                                        @NotNull UUID uuidGrave, int line) {
        return new HologramData(location, uuidEntity, uuidGrave, line);
    }

    /**
     * Retrieves the LocationData for a given location.
     *
     * @param location The location for which to retrieve data.
     * @return A LocationData instance representing the specified location.
     * @deprecated Since 4.9.9.1. Access data classes directly as needed; no replacement API.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public LocationData getLocationData(@NotNull Location location) {
        return new LocationData(location);
    }

    /**
     * Simplifies a given BlockFace to one of the four cardinal directions (NORTH, EAST, SOUTH, WEST).
     *
     * @param face The BlockFace to simplify.
     * @return The simplified BlockFace.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#simplifyBlockFace(BlockFace)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public BlockFace simplifyBlockFace(@NotNull BlockFace face) {
        return api.world.simplifyBlockFace(face);
    }

    /**
     * Retrieves the Rotation corresponding to a given BlockFace.
     *
     * @param face The BlockFace for which to retrieve the rotation.
     * @return The corresponding Rotation for the specified BlockFace.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#getRotationFromBlockFace(BlockFace)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Rotation getRotationFromBlockFace(@NotNull BlockFace face) {
        return api.world.getRotationFromBlockFace(face);
    }

    /**
     * Encodes an object to a Base64 string using Base64Util.
     *
     * @param object The object to encode.
     * @return The Base64 encoded string, or null if encoding fails.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#objectToBase64(Object)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String encodeObjectToBase64(@NotNull Object object) {
        return api.util.objectToBase64(object);
    }

    /**
     * Decodes a Base64 string to an object using Base64Util.
     *
     * @param base64String The Base64 string to decode.
     * @return The decoded object, or null if decoding fails.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#base64ToObject(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Object decodeBase64ToObject(@NotNull String base64String) {
        return api.util.base64ToObject(base64String);
    }

    /**
     * Loads a class with the specified name using ClassUtil.
     *
     * @param className The fully qualified name of the class to be loaded.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#loadClass(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void loadClass(@NotNull String className) {
        api.util.loadClass(className);
    }

    /**
     * Gets the Color corresponding to the given color name using ColorUtil.
     *
     * @param colorName The name of the color as a string.
     * @return The Color corresponding to the given name, or null if no match is found.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#getColor(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Color getColor(@NotNull String colorName) {
        return api.util.getColor(colorName);
    }

    /**
     * Parses a hex color code to a Color using ColorUtil.
     *
     * @param hex The hex color code as a string (e.g., "#FF5733").
     * @return The Color corresponding to the hex color code, or null if the code is invalid.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#getColorFromHex(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Color getColorFromHex(@NotNull String hex) {
        return api.util.getColorFromHex(hex);
    }

    /**
     * Creates a Particle.DustOptions object using a hex color code.
     *
     * @param hexColor The hex color code as a string (e.g., "#FF5733").
     * @param size     The size of the dust particle.
     * @return A Particle.DustOptions object with the specified color and size, or null if the color code is invalid.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#dustFromHex(String, float)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Particle.DustOptions createDustOptionsFromHex(@NotNull String hexColor, float size) {
        return api.util.dustFromHex(hexColor, size);
    }

    /**
     * Checks if the specified entity has the given permission using EntityUtil.
     *
     * @param entity     The entity to check.
     * @param permission The permission to check for.
     * @return {@code true} if the entity has the specified permission,
     * {@code true} if the method is not found,
     * or {@code false} if an exception occurs.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#hasPermission(Entity, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean hasPermission(@NotNull Entity entity, @NotNull String permission) {
        return api.util.hasPermission(entity, permission);
    }

    /**
     * Gets the total experience of the specified player using ExperienceUtil.
     *
     * @param player The player to get the experience from.
     * @return The total experience of the player.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#playerTotalXp(Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int getPlayerExperience(@NotNull Player player) {
        return api.util.playerTotalXp(player);
    }

    /**
     * Gets the experience required to reach a specific level using ExperienceUtil.
     *
     * @param level The level to get the experience for.
     * @return The experience required to reach the specified level.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#xpAtLevel(int)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int getExperienceAtLevel(int level) {
        return api.util.xpAtLevel(level);
    }

    /**
     * Calculates the level from a given amount of experience using ExperienceUtil.
     *
     * @param experience The experience to calculate the level from.
     * @return The level corresponding to the given experience.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#levelFromXp(long)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public long getLevelFromExperience(long experience) {
        return api.util.levelFromXp(experience);
    }

    /**
     * Calculates the drop percentage of experience using ExperienceUtil.
     *
     * @param experience The total experience.
     * @param percent    The percentage to drop.
     * @return The experience drop amount.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#dropPercent(int, float)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int getDropPercent(int experience, float percent) {
        return api.util.dropPercent(experience, percent);
    }

    /**
     * Gets the amount of experience a player will drop upon death based on a percentage.
     *
     * @param player          The player to get the drop experience from.
     * @param expStorePercent The percentage of experience to drop.
     * @return The amount of experience to drop.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#playerDropXp(Player, float)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int getPlayerDropExperience(@NotNull Player player, float expStorePercent) {
        return api.util.playerDropXp(player, expStorePercent);
    }

    /**
     * Moves a file to a new location with a new name using FileUtil.
     *
     * @param file The file to be moved.
     * @param name The new name for the file.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#moveFile(File, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void moveFile(@NotNull File file, @NotNull String name) {
        api.util.moveFile(file, name);
    }

    /**
     * Copies a file to a new location with a new name using FileUtil.
     *
     * @param file The file to be copied.
     * @param name The new name for the copied file.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#copyFile(File, String)} (deprecated).
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void copyFile(@NotNull File file, @NotNull String name) {
        api.util.copyFile(file, name);
    }

    /**
     * Gets the appropriate inventory size based on the given size.
     *
     * @param size The size to be used for determining the inventory size.
     * @return The appropriate inventory size.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.inventory.InventoryAPI#getInventorySize(int)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int getInventorySize(int size) {
        return api.inventory.getInventorySize(size);
    }

    /**
     * Equips the player's armor from the given inventory.
     *
     * @param inventory The inventory containing the armor items.
     * @param player    The player to be equipped with armor.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.inventory.InventoryAPI#equipArmor(Inventory, Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void equipArmor(@NotNull Inventory inventory, @NotNull Player player) {
        api.inventory.equipArmor(inventory, player);
    }

    /**
     * Equips the player's inventory items from the given inventory.
     *
     * @param inventory The inventory containing the items.
     * @param player    The player to be equipped with items.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.inventory.InventoryAPI#equipItems(Inventory, Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void equipItems(@NotNull Inventory inventory, @NotNull Player player) {
        api.inventory.equipItems(inventory, player);
    }

    /**
     * Converts the given inventory to a string representation.
     *
     * @param inventory The inventory to be converted.
     * @return The string representation of the inventory.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.inventory.InventoryAPI#inventoryToString(Inventory)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String inventoryToString(@NotNull Inventory inventory) {
        return api.inventory.inventoryToString(inventory);
    }

    /**
     * Converts a string representation of an inventory to an Inventory object.
     *
     * @param inventoryHolder The inventory holder.
     * @param string          The string representation of the inventory.
     * @param title           The title of the inventory.
     * @return The Inventory object.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.inventory.InventoryAPI#stringToInventory(InventoryHolder, String, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Inventory stringToInventory(@NotNull InventoryHolder inventoryHolder, @NotNull String string, @NotNull String title) {
        return api.inventory.stringToInventory(inventoryHolder, string, title);
    }

    /**
     * Rounds the given location's coordinates to the nearest whole numbers.
     *
     * @param location The location to be rounded.
     * @return A new location with rounded coordinates.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#roundLocation(Location)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Location roundLocation(@NotNull Location location) {
        return api.world.roundLocation(location);
    }

    /**
     * Converts a Location object to a string representation.
     *
     * @param location The location to be converted.
     * @return A string representation of the location in the format "world|x|y|z".
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#locationToString(Location)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String locationToString(@NotNull Location location) {
        return api.world.locationToString(location);
    }

    /**
     * Converts a chunk's location to a string representation.
     *
     * @param location The location within the chunk.
     * @return A string representation of the chunk in the format "world|chunkX|chunkZ".
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#chunkToString(Location)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String chunkToString(@NotNull Location location) {
        return api.world.chunkToString(location);
    }

    /**
     * Converts a chunk string representation back to a Location object.
     *
     * @param string The string representation of the chunk in the format "world|chunkX|chunkZ".
     * @return A Location object representing the chunk.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#chunkStringToLocation(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Location chunkStringToLocation(@NotNull String string) {
        return api.world.chunkStringToLocation(string);
    }

    /**
     * Converts a string representation of a location back to a Location object.
     *
     * @param string The string representation of the location in the format "world|x|y|z".
     * @return A Location object.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#stringToLocation(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Location stringToLocation(@NotNull String string) {
        return api.world.stringToLocation(string);
    }

    /**
     * Finds the closest location to a given base location from a list of locations.
     *
     * @param locationBase The base location to compare against.
     * @param locationList The list of locations to search through.
     * @return The closest location to the base location, or null if the list is empty.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.world.LocationAPI#getClosestLocation(Location, List)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Location getClosestLocation(@NotNull Location locationBase, @NotNull List<Location> locationList) {
        return api.world.getClosestLocation(locationBase, locationList);
    }

    /**
     * Checks if the given material is an air block.
     *
     * @param material The material to check.
     * @return True if the material is air, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isAir(Material)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isAir(@NotNull Material material) {
        return api.util.isAir(material);
    }

    /**
     * Checks if the given material is lava.
     *
     * @param material The material to check.
     * @return True if the material is lava, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isLava(Material)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isLava(@NotNull Material material) {
        return api.util.isLava(material);
    }

    /**
     * Checks if the given material is not solid and is safe (i.e., not lava).
     *
     * @param material The material to check.
     * @return True if the material is not solid and safe, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isSafeNotSolid(Material)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isSafeNotSolid(@NotNull Material material) {
        return api.util.isSafeNotSolid(material);
    }

    /**
     * Checks if the given material is solid and safe (i.e., not lava).
     *
     * @param material The material to check.
     * @return True if the material is solid and safe, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isSafeSolid(Material)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isSafeSolid(@NotNull Material material) {
        return api.util.isSafeSolid(material);
    }

    /**
     * Checks if the given material is water.
     *
     * @param material The material to check.
     * @return True if the material is water, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isWater(Material)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isWater(@NotNull Material material) {
        return api.util.isWater(material);
    }

    /**
     * Checks if the given material is a player head.
     *
     * @param material The material to check.
     * @return True if the material is a player head, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isPlayerHead(Material)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isPlayerHead(@NotNull Material material) {
        return api.util.isPlayerHead(material);
    }

    /**
     * Checks if the given material is a player head.
     *
     * @param material The material to check via string.
     * @return True if the material is a player head, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isPlayerHead(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isPlayerHead(@NotNull String material) {
        return api.util.isPlayerHead(material);
    }

    /**
     * Posts the given log content to mclo.gs and returns the URL of the posted log.
     *
     * @param content The log content to be posted.
     * @return The URL of the posted log, or null if the post was unsuccessful.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#postLog(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String postLog(@NotNull String content) {
        return api.util.postLog(content);
    }

    /**
     * Gets the highest integer value associated with a specific permission prefix for the player.
     *
     * @param player     The player whose permissions are being checked.
     * @param permission The permission prefix to search for.
     * @return The highest integer value found for the specified permission prefix. Returns 0 if no such permission is found.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#highestInt(Player, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int getHighestInt(@NotNull Player player, @Nullable String permission) {
        return api.util.highestInt(player, permission);
    }

    /**
     * Gets the highest double value associated with a specific permission prefix for the player.
     *
     * @param player     The player whose permissions are being checked.
     * @param permission The permission prefix to search for.
     * @return The highest double value found for the specified permission prefix. Returns 0 if no such permission is found.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#highestDouble(Player, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public double getHighestDouble(@NotNull Player player, String permission) {
        return api.util.highestDouble(player, permission);
    }

    /**
     * Triggers the main hand swing animation for the specified player.
     *
     * @param player The player whose main hand swing animation is to be triggered.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#swingMainHand(Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void swingMainHand(@NotNull Player player) {
        api.util.swingMainHand(player);
    }

    /**
     * Copies resources from the plugin's JAR file to the specified output path.
     *
     * @param inputPath  The path inside the JAR file to copy from.
     * @param outputPath The path on the file system to copy to.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#copyResources(String, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void copyResources(@NotNull String inputPath, @NotNull String outputPath) {
        api.util.copyResources(inputPath, outputPath);
    }

    /**
     * Copies resources from the plugin's JAR file to the specified output path, with an option to overwrite existing files.
     *
     * @param inputPath  The path inside the JAR file to copy from.
     * @param outputPath The path on the file system to copy to.
     * @param overwrite  Whether to overwrite existing files.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#copyResources(String, String, boolean)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void copyResources(@NotNull String inputPath, @NotNull String outputPath, boolean overwrite) {
        api.util.copyResources(inputPath, outputPath, overwrite);
    }

    /**
     * Gets the skin signature of the specified entity if it is a player.
     *
     * @param entity The entity whose skin signature is to be retrieved.
     * @return The skin signature of the player, or null if the entity is not a player or the signature could not be retrieved.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.skin.SkinAPI#getSkinSignature(Entity)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String getSkinSignature(@NotNull Entity entity) {
        return api.skin.getSkinSignature(entity);
    }

    /**
     * Sets the texture of a Skull block.
     *
     * @param skull  The Skull block.
     * @param name   The name associated with the texture.
     * @param base64 The Base64 encoded texture.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.skin.SkinAPI#setSkullTexture(Skull, String, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void setSkullTexture(@NotNull Skull skull, @NotNull String name, @NotNull String base64) {
        api.skin.setSkullTexture(skull, name, base64);
    }

    /**
     * Sets the texture of a Skull item stack.
     *
     * @param skullMeta The SkullMeta item meta.
     * @param name      The name associated with the texture.
     * @param base64    The Base64 encoded texture.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.skin.SkinAPI#setSkullTexture(SkullMeta, String, String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void setSkullTexture(@NotNull SkullMeta skullMeta, @NotNull String name, @NotNull String base64) {
        api.skin.setSkullTexture(skullMeta, name, base64);
    }

    /**
     * Retrieves the texture of the specified entity.
     *
     * @param entity The entity from which to get the texture.
     * @return The Base64 encoded texture string, or null if not found.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.skin.SkinAPI#getTexture(Entity)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String getTexture(@NotNull Entity entity) {
        return api.skin.getTexture(entity);
    }

    /**
     * Retrieves the GameProfile of the specified player.
     *
     * @param player The player from which to get the GameProfile.
     * @return The GameProfile of the player, or null if not found.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.skin.SkinAPI#getPlayerGameProfile(Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public GameProfile getPlayerGameProfile(@NotNull Player player) {
        return api.skin.getPlayerGameProfile(player);
    }

    /**
     * Converts a string to a UUID.
     *
     * @param string The string to convert to a UUID.
     * @return The UUID if the string is a valid UUID format, otherwise null.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#uuidOf(String)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public UUID getUUID(@NotNull String string) {
        return api.util.uuidOf(string);
    }

    /**
     * Gets the latest version of a resource from SpigotMC.
     *
     * @param resourceId The ID of the resource on SpigotMC.
     * @return The latest version of the resource as a String, or null if an error occurs.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#latestSpigotVersion(int)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public String getLatestVersion(int resourceId) {
        return api.util.latestSpigotVersion(resourceId);
    }

    /**
     * Checks if a given file is a valid YAML file.
     *
     * @param file The file to check.
     * @return True if the file is a valid YAML file, otherwise false.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.util.UtilAPI#isValidYaml(File)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isValidYAML(@NotNull File file) {
        return api.util.isValidYaml(file);
    }

    /**
     * This code is added for debugging purposes.
     * Checks if the specified location is a grave's location.
     *
     * @param grave the grave to check. This always returns true for the provided grave's death location.
     *              For more precise checking, use {@link #isGrave(Grave, Location)} with a specific location.
     * @return true if the location matches the grave's death location, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#isGrave(Grave)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isGrave(@NotNull Grave grave) {
        return api.gravesManage.isGrave(grave);
    }

    /**
     * Checks if a given location matches the death location of a specific grave.
     *
     * @param grave    the grave to check
     * @param location the location to compare with the grave's death location
     * @return true if the location matches the grave's death location, false otherwise.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#isGrave(Grave, Location)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public boolean isGrave(@NotNull Grave grave, @NotNull Location location) {
        return api.gravesManage.isGrave(grave, location);
    }

    /**
     * Returns the total number of graves for all players.
     * <p>
     * This method calls {@link #getGraveAmount(Player)} with a {@code null} argument
     * to count graves without filtering by any specific player.
     *
     * @return the total count of graves for all players.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#getGraveAmount()}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public long getGraveAmount() {
        return api.gravesManage.getGraveAmount();
    }

    /**
     * Returns the number of graves associated with a specified player.
     * <p>
     * If {@code targetPlayer} is provided, only graves owned by this player will be counted.
     * If {@code targetPlayer} is {@code null}, all graves are counted.
     *
     * @param targetPlayer the player whose graves should be counted; if {@code null},
     *                     counts graves for all players.
     * @return the number of graves associated with {@code targetPlayer}, or the total
     * count of all graves if {@code targetPlayer} is {@code null}.
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.grave.GraveManagementAPI#getGraveAmount(Player)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public long getGraveAmount(@Nullable Player targetPlayer) {
        return api.gravesManage.getGraveAmount(targetPlayer);
    }

    /**
     * Downloads a plugin from Spiget and saves it to the plugins folder, replacing it if it exists.
     *
     * @param pluginId      The Spigot resource ID of the plugin.
     * @param pluginName    The name of the plugin file (without the ".jar" extension).
     * @param pluginsFolder The path to the plugins' folder.
     * @param commandSender The sender to message about progress/result.
     * @throws IOException If the download or file operations fail.
     * @deprecated Since 4.9.9.1. Call {@link PluginDownloadUtil} directly from your code.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public static void downloadAndReplacePlugin(long pluginId, String pluginName, String pluginsFolder, CommandSender commandSender) throws IOException {
        PluginDownloadUtil.downloadAndReplacePlugin(pluginId, pluginName, pluginsFolder, commandSender);
    }

    /**
     * Downloads a plugin from Spiget and saves it to the plugins folder, replacing it if it exists.
     *
     * @param pluginId      The Spigot resource ID of the plugin.
     * @param pluginName    The name of the plugin file (without the ".jar" extension).
     * @param pluginsFolder The path to the plugins' folder.
     * @param commandSender The sender to message about progress/result.
     * @throws IOException If the download or file operations fail.
     * @deprecated Since 4.9.9.1. Call {@link PluginDownloadUtil} directly from your code.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public static void downloadAndReplacePlugin(String pluginId, String pluginName, String pluginsFolder, CommandSender commandSender) throws IOException {
        PluginDownloadUtil.downloadAndReplacePlugin(pluginId, pluginName, pluginsFolder, commandSender);
    }

    /**
     * Ensures creation of an addon folder.
     *
     * @param addon The addon to register
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.addon.AddonAPI#ensureAddonFolder(Plugin)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void ensureGravesXAddonFolder(Plugin addon) {
        api.addon.ensureAddonFolder(addon);
    }

    /**
     * Exports addon configs
     *
     * @param addon the addon to get configs from
     * @return the addons exported
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.addon.AddonAPI#exportAddonConfigs(Plugin)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int exportAddonConfigs(Plugin addon) {
        return api.addon.exportAddonConfigs(addon);
    }

    /**
     * Exports addon configs
     *
     * @param addon           the addon to get configs from
     * @param replaceIfExists replace configs even if they exist
     * @return the addons exported
     * @deprecated Since 4.9.9.1. Use {@link dev.cwhead.GravesX.api.addon.AddonAPI#exportAddonConfigs(Plugin, boolean)}.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public int exportAddonConfigs(Plugin addon, boolean replaceIfExists) {
        return api.addon.exportAddonConfigs(addon, replaceIfExists);
    }

    /**
     * Retrieves the instance of the {@link Graves} class.
     *
     * <p><strong>Warning:</strong> Using this method can have undesirable results. Unless you know what you are doing, we recommend using other methods.</p>
     *
     * @return the {@link Graves} instance.
     * @deprecated Since 4.9.9.1. Prefer using the typed sub-APIs from {@link dev.cwhead.GravesX.api.GravesXAPI}.
     */
    @ApiStatus.Experimental
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public Graves getGravesX() {
        return plugin;
    }

    /**
     * Gets the instance of the GravesXAPI.
     *
     * @return The instance of the API.
     * @deprecated Since 4.9.9.1. This self-reference is obsolete; hold {@link dev.cwhead.GravesX.api.GravesXAPI} instead.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public GravesXAPI getInstance() {
        return this;
    }

    /**
     * Registers the API as an event listener in the plugin manager.
     *
     * @deprecated Since 4.9.9.1. Register your own listeners where needed; this API class is being removed.
     */
    @Deprecated(forRemoval = true, since = "4.9.9.1")
    public void register() {
        plugin.getLogger().warning("[GravesX] GravesXAPI#register() is deprecated and now a no-op.");
    }
}