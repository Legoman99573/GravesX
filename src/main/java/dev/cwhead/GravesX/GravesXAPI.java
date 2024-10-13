package dev.cwhead.GravesX;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.event.GraveBlockPlaceEvent;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.event.GraveProtectionCreateEvent;
import com.ranull.graves.manager.*;
import com.ranull.graves.type.Grave;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @NotNull EntityType killerEntityType, @Nullable Location locationDeath, @Nullable Map<EquipmentSlot, ItemStack> equipmentMap, @Nullable List<ItemStack> itemStackList, int experience, long timeAliveRemaining, @Nullable StorageType storageType, boolean graveProtection, long graveProtectionTime) {
        GraveManager graveManager = plugin.getGraveManager();
        DataManager dataManager = plugin.getDataManager();
        IntegrationManager integrationManager = plugin.getIntegrationManager();
        VersionManager versionManager = plugin.getVersionManager();
        LocationManager locationManager = plugin.getLocationManager();
        EntityManager entityManager = plugin.getEntityManager();

        Map<Location, BlockData.BlockType> locationMap = new HashMap<>();
        Grave grave = graveManager.createGrave(victim, itemStackList);

        grave.setOwnerType(victim.getType());
        grave.setOwnerName(victim.getName());
        grave.setOwnerNameDisplay(victim instanceof Player ? ((Player) victim).getDisplayName() : grave.getOwnerName());
        grave.setOwnerUUID(victim.getUniqueId());
        grave.setPermissionList(null);
        grave.setYaw(victim.getLocation().getYaw());
        grave.setPitch(victim.getLocation().getPitch());
        Location finalLocationDeath = locationDeath != null ? locationManager.getSafeGraveLocation((LivingEntity) victim, locationDeath, grave) : locationManager.getSafeGraveLocation((LivingEntity) victim, victim.getLocation(), grave);
        if (killer != null) {
            grave.setKillerType(EntityType.PLAYER);
            grave.setKillerName(killer.getName());
            grave.setKillerNameDisplay(killer.getCustomName());
            grave.setKillerUUID(killer.getUniqueId());
        } else {
            grave.setKillerUUID(null);
            grave.setKillerType(null);
            grave.setKillerName(graveManager.getDamageReason(victim.getLastDamageCause() != null ? victim.getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.valueOf("KILLED"), grave));
            grave.setKillerNameDisplay(grave.getKillerName());
        }

        if (graveProtection && plugin.getConfig("protection.enabled", grave).getBoolean("protection.enabled")) {
            GraveProtectionCreateEvent graveProtectionCreateEvent = new GraveProtectionCreateEvent(victim, grave);
            plugin.getServer().getPluginManager().callEvent(graveProtectionCreateEvent);
            if (!graveProtectionCreateEvent.isCancelled()) {
                grave.setProtection(true);
                grave.setTimeProtection(graveProtectionTime != 0 ? graveProtectionTime : plugin.getConfig("protection.time", grave).getInt("protection.time") * 1000L);
            }
        }

        try {
            GraveCreateEvent createGrave = new GraveCreateEvent(victim, grave);
            Bukkit.getPluginManager().callEvent(createGrave);
            if (!createGrave.isCancelled()) {
                locationMap.put(finalLocationDeath, BlockData.BlockType.DEATH);

                grave.setLocationDeath(finalLocationDeath);
                grave.setInventory(graveManager.getGraveInventory(grave, (LivingEntity) victim, itemStackList, getRemovedItemStacks((LivingEntity) victim), null));
                if (equipmentMap != null) {
                    grave.setEquipmentMap(equipmentMap);
                } else {
                    grave.setEquipmentMap(!versionManager.is_v1_7() ? entityManager.getEquipmentMap((LivingEntity) victim, grave) : new HashMap<>());
                }
                dataManager.addGrave(grave);
                if (integrationManager.hasMultiPaper()) {
                    integrationManager.getMultiPaper().notifyGraveCreation(grave);
                }
                placeGraveBlocks(grave, locationMap, (LivingEntity) victim);

                plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + victim + " through the GravesX API", 1);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while creating grave " + grave.getUUID() + " for entity " + victim + " through the GravesX API. Cause: " + e.getCause());
            plugin.getLogger().severe("Exception Message: " + e.getMessage());
            plugin.logStackTrace(e);
        }
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

    private void placeGraveBlocks(Grave grave, Map<Location, BlockData.BlockType> locationMap, LivingEntity livingEntity) {
        for (Map.Entry<Location, BlockData.BlockType> entry : locationMap.entrySet()) {
            Location location = entry.getKey().clone();
            int offsetX = 0;
            int offsetY = 0;
            int offsetZ = 0;
            switch (entry.getValue()) {
                case DEATH:
                    break;
                case NORMAL:
                    offsetX = plugin.getConfig("placement.offset.x", grave).getInt("placement.offset.x");
                    offsetY = plugin.getConfig("placement.offset.y", grave).getInt("placement.offset.y");
                    offsetZ = plugin.getConfig("placement.offset.z", grave).getInt("placement.offset.z");
                    break;
                case GRAVEYARD:
                    offsetX = plugin.getConfig().getInt("settings.graveyard.offset.x");
                    offsetY = plugin.getConfig().getInt("settings.graveyard.offset.y");
                    offsetZ = plugin.getConfig().getInt("settings.graveyard.offset.z");
                    break;
            }
            location.add(offsetX, offsetY, offsetZ);
            GraveBlockPlaceEvent graveBlockPlaceEvent = new GraveBlockPlaceEvent(grave, location, entry.getValue(), entry.getKey().getBlock(), livingEntity);
            plugin.getServer().getPluginManager().callEvent(graveBlockPlaceEvent);
            if (!graveBlockPlaceEvent.isCancelled()) {
                plugin.getGraveManager().placeGrave(graveBlockPlaceEvent.getLocation(), grave);
                plugin.getEntityManager().sendMessage("message.block", livingEntity, location, grave);
                plugin.getEntityManager().runCommands("event.command.block", livingEntity, graveBlockPlaceEvent.getLocation(), grave);
            }
        }
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