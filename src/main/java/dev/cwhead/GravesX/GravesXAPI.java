package dev.cwhead.GravesX;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.manager.CacheManager;
import com.ranull.graves.manager.DataManager;
import com.ranull.graves.manager.GraveManager;
import com.ranull.graves.type.Grave;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * API for managing graves in the GravesX plugin. The GravesXAPI provides methods to create graves for entities
 * and manage grave creation events.
 * <p>
 * Graves are created with various configurations, including equipment, items, experience, protection, and more.
 * The API also handles event triggering when graves are created and ensures data is stored correctly.
 */
public class GravesXAPI {
    private final GravesXAPI instance;

    private final Graves plugin;

    /**
     * Constructor for initializing the GravesXAPI with the main plugin instance.
     *
     * @param plugin The main Graves plugin instance.
     */
    public GravesXAPI(Graves plugin) {
        this.plugin = plugin;
        instance = this;
    }

    //TODO Implement API methods
    /**
     * Creates a grave for an entity with the basic parameters.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining) {
        createGrave(victim, null, killerEntityType,  null, equipmentMap, itemStackList, experience, timeAliveRemaining, null, false, 0);
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
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType,  null, equipmentMap, itemStackList, experience, timeAliveRemaining, null, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity with a specific storage type.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param storageType         The type of storage used for the grave (nullable).
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, @Nullable StorageType storageType) {
        createGrave(victim, null, killerEntityType,  null, equipmentMap, itemStackList, experience, timeAliveRemaining, storageType, false, 0);
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
     * @param storageType         The type of storage used for the grave (nullable).
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, @Nullable StorageType storageType, boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType,  null, equipmentMap, itemStackList, experience, timeAliveRemaining, storageType, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity at a specific location where the victim died.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, @Nullable StorageType storageType) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, storageType, false, 0);
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
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, @Nullable StorageType storageType, boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, storageType, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity at a specific location without a killer and no storage type.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, null, false, 0);
    }

    /**
     * Creates a grave for an entity at a specific location with grave protection, no killer, and no storage type.
     *
     * @param victim              The entity that died.
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     */
    public void createGrave(@NotNull Entity victim, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, null, graveProtection, graveProtectionTime);
    }

    /**
     * Creates a grave for an entity killed by another entity.
     *
     * @param victim              The entity that died.
     * @param killer              The entity that killed the victim (nullable).
     * @param killerEntityType    The entity type of the killer.
     * @param locationDeath       The location where the victim died (nullable).
     * @param equipmentMap        The equipment the victim had at the time of death.
     * @param itemStackList       The list of items the victim had at the time of death.
     * @param experience          The experience the victim had.
     * @param timeAliveRemaining  The remaining time the grave will stay alive.
     */
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining) {
        createGrave(victim, killer, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, null, false, 0);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, killer, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, null, graveProtection, graveProtectionTime);
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
     * @param storageType         The type of storage used for the grave (nullable).
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     */
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @NotNull Map<EquipmentSlot, ItemStack> equipmentMap, @NotNull List<ItemStack> itemStackList, int experience, long timeAliveRemaining, @Nullable StorageType storageType, boolean graveProtection, long graveProtectionTime) {
        GraveManager graveManager = plugin.getGraveManager();
        DataManager dataManager = plugin.getDataManager();
        CacheManager cacheManager = plugin.getCacheManager();
        Grave grave = graveManager.createGrave(victim, itemStackList);
        Map<Location, BlockData.BlockType> locationMap = new HashMap<>();

        grave.setOwnerType(victim.getType());
        grave.setOwnerUUID(victim.getUniqueId());
        grave.setOwnerName(victim.getName());
        grave.setOwnerDisplayName(victim instanceof Player ? ((Player) victim).getDisplayName()
                : victim.getCustomName());
        if (killer != null) {
            grave.setKillerName(killer instanceof Player ? ((Player) killer).getDisplayName()
                    : killer.getCustomName());
        }
        grave.setKillerType(killerEntityType);
        grave.setTimeCreation(System.currentTimeMillis());
        grave.setTimeAlive(timeAliveRemaining);
        grave.setTimeAliveRemaining(timeAliveRemaining);
        grave.setProtection(graveProtection);
        grave.setTimeProtection(graveProtectionTime);
        grave.setExperience(experience);
        grave.setEquipmentMap(equipmentMap);
        Location finalLocationDeath;

        if (locationDeath != null) {
            finalLocationDeath = locationDeath;
        } else {
            finalLocationDeath = victim.getLocation();
        }

        try {
            GraveCreateEvent createGrave = new GraveCreateEvent(victim, grave);
            Bukkit.getPluginManager().callEvent(createGrave);
            if (!createGrave.isCancelled()) {
                locationMap.put(finalLocationDeath, BlockData.BlockType.DEATH);

                grave.setLocationDeath(finalLocationDeath);
                grave.setInventory(plugin.getGraveManager().getGraveInventory(grave, (LivingEntity) victim, itemStackList, getRemovedItemStacks((LivingEntity) victim), null));
                grave.setEquipmentMap(!plugin.getVersionManager().is_v1_7() ? plugin.getEntityManager().getEquipmentMap((LivingEntity) victim, grave) : new HashMap<>());
                graveManager.placeGrave(finalLocationDeath, grave);
                dataManager.addGrave(grave);

                cacheManager.getGraveMap().put(grave.getUUID(), grave);

                ChunkData chunkData = new ChunkData(finalLocationDeath);

                String chunkKey = generateChunkKey(finalLocationDeath);
                cacheManager.getChunkMap().put(chunkKey, chunkData);

                plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + victim + " through the GravesX API", 1);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while creating grave " + grave.getUUID() + " for entity " + victim + " through the GravesX API. Cause: " + e.getCause());
            plugin.getLogger().severe("Exception Message: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    private String generateChunkKey(Location location) {
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();
        return chunkX + "_" + chunkZ;
    }

    /**
     * Retrieves the list of removed item stacks for the specified entity.
     *
     * @param livingEntity The entity whose removed item stacks are to be retrieved.
     * @return The list of removed item stacks.
     */
    private List<ItemStack> getRemovedItemStacks(LivingEntity livingEntity) {
        List<ItemStack> removedItemStackList = new ArrayList<>();
        if (plugin.getCacheManager().getRemovedItemStackMap().containsKey(livingEntity.getUniqueId())) {
            removedItemStackList.addAll(plugin.getCacheManager().getRemovedItemStackMap().get(livingEntity.getUniqueId()));
            plugin.getCacheManager().getRemovedItemStackMap().remove(livingEntity.getUniqueId());
        }
        return removedItemStackList;
    }

    /**
     * Gets the instance of the GravesXAPI.
     *
     * @return The instance of the API.
     */
    public GravesXAPI getInstance() {
        return instance;
    }

    /**
     * Registers the API as an event listener in the plugin manager.
     */
    public void register() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents((Listener) this, plugin);
    }

    /**
     * Enumeration representing different types of storage modes for the grave.
     */
    public enum StorageType {
        COMPACT,
        EXACT,
        CHESTSORT
    }
}