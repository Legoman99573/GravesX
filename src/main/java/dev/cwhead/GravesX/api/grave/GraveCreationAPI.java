package dev.cwhead.GravesX.api.grave;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.manager.*;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.util.UtilAPI;
import dev.cwhead.GravesX.api.world.LocationAPI;
import dev.cwhead.GravesX.event.GraveBlockPlaceEvent;
import dev.cwhead.GravesX.event.GraveCreateEvent;
import dev.cwhead.GravesX.event.GraveProtectionCreateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * API for creating graves and firing appropriate events.
 */
public final class GraveCreationAPI {
    private final Graves plugin;
    private final LocationAPI world;
    private final UtilAPI util;
    private final GraveManagementAPI manage;

    public GraveCreationAPI(Graves plugin, LocationAPI world, UtilAPI util, GraveManagementAPI manage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.util = Objects.requireNonNull(util, "util");
        this.manage = Objects.requireNonNull(manage, "manage");
    }

    /**
     * Creates a grave for an entity with the basic parameters.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType, long timeAliveRemaining) {
        createGrave(victim, null, killerEntityType, null, null, null, 0, timeAliveRemaining, null, false, 0);
    }

    /**
     * Creates a grave for an entity with the basic parameters.
     *
     * @param victim             The entity that died.
     * @param killerEntityType   The entity type of the killer.
     * @param experience         The experience the victim had.
     * @param timeAliveRemaining The remaining time the grave will stay alive.
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType, int experience, long timeAliveRemaining) {
        createGrave(victim, null, killerEntityType, null, null, null, experience, timeAliveRemaining, null, false, 0);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining) {
        createGrave(victim, null, killerEntityType, null, equipmentMap, itemStackList, experience, timeAliveRemaining, null, false, 0);
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
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType, null, equipmentMap, itemStackList, experience, timeAliveRemaining, null, graveProtection, graveProtectionTime);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause) {
        createGrave(victim, null, killerEntityType, null, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, false, 0);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause,
                            boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType, null, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, graveProtection, graveProtectionTime);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, false, 0);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause,
                            boolean graveProtection, long graveProtectionTime) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, graveProtection, graveProtectionTime);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining) {
        createGrave(victim, null, killerEntityType, locationDeath, equipmentMap, itemStackList, experience, timeAliveRemaining, null, false, 0);
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
     */
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining) {
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
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath,
                            @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList,
                            int experience, long timeAliveRemaining,
                            boolean graveProtection, long graveProtectionTime) {
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
     * @param damageCause         Damage Caused (nullable).
     * @param graveProtection     Whether the grave is protected.
     * @param graveProtectionTime The time for which the grave remains protected.
     */
    public void createGrave(@NotNull Entity victim, @Nullable Entity killer, @Nullable EntityType killerEntityType,
                            @Nullable Location locationDeath, @Nullable Map<EquipmentSlot, ItemStack> equipmentMap,
                            @Nullable List<ItemStack> itemStackList, int experience, long timeAliveRemaining,
                            @Nullable EntityDamageEvent.DamageCause damageCause, boolean graveProtection, long graveProtectionTime) {
        GraveManager graveManager = plugin.getGraveManager();
        DataManager dataManager = plugin.getDataManager();
        IntegrationManager integrationManager = plugin.getIntegrationManager();
        VersionManager versionManager = plugin.getVersionManager();
        LocationManager locationManager = plugin.getLocationManager();
        EntityManager entityManager = plugin.getEntityManager();
        CacheManager cacheManager = plugin.getCacheManager();

        Map<Location, BlockData.BlockType> locationMap = new HashMap<>();
        Grave grave = graveManager.createGrave(victim, itemStackList);

        grave.setOwnerType(victim.getType());
        grave.setOwnerName(victim.getName());
        grave.setOwnerNameDisplay(victim instanceof Player ? ((Player) victim).getDisplayName() : grave.getOwnerName());
        grave.setOwnerUUID(victim.getUniqueId());
        grave.setOwnerTexture(util.skinTexture(victim));
        grave.setOwnerTextureSignature(util.skinSignature(victim));
        grave.setPermissionList(null);
        grave.setYaw(victim.getLocation().getYaw());
        grave.setPitch(victim.getLocation().getPitch());
        grave.setExperience(experience);
        grave.setTimeCreation(System.currentTimeMillis());
        long truetimeAliveRemaining = timeAliveRemaining > 0 ? timeAliveRemaining : plugin.getConfig("grave.time", grave).getLong("grave.time");
        grave.setTimeAlive(truetimeAliveRemaining);
        grave.setTimeAliveRemaining(truetimeAliveRemaining);
        Location finalLocationDeath = locationDeath != null ? locationDeath : locationManager.getSafeGraveLocation((LivingEntity) victim, victim.getLocation(), grave);

        if (killer != null) {
            grave.setKillerType(killerEntityType != null ? killerEntityType : EntityType.PLAYER);
            grave.setKillerName(killer.getName());
            grave.setKillerNameDisplay(killer.getCustomName());
            grave.setKillerUUID(killer.getUniqueId());
        } else {
            grave.setKillerUUID(victim.getUniqueId());
            grave.setKillerType(EntityType.PLAYER);
            EntityDamageEvent.DamageCause finalDamageCause = damageCause != null ? damageCause : EntityDamageEvent.DamageCause.valueOf("KILL");
            grave.setKillerName(graveManager.getDamageReason(
                    victim.getLastDamageCause() != null ? victim.getLastDamageCause().getCause() : finalDamageCause, grave));
            grave.setKillerNameDisplay(grave.getKillerName());
        }

        if (graveProtection && plugin.getConfig("protection.enabled", grave).getBoolean("protection.enabled")) {
            GraveProtectionCreateEvent gp = new GraveProtectionCreateEvent(victim, grave);
            plugin.getServer().getPluginManager().callEvent(gp);
            grave.setProtection(true);
            grave.setTimeProtection(graveProtectionTime > 0 ? graveProtectionTime : plugin.getConfig("protection.time", grave).getInt("protection.time") * 1000L);
        }

        try {
            GraveCreateEvent createGrave = new GraveCreateEvent(victim, grave);
            Bukkit.getPluginManager().callEvent(createGrave);
            if (createGrave.isCancelled()) return;

            locationMap.put(finalLocationDeath, BlockData.BlockType.DEATH);

            cacheManager.getGraveMap().put(grave.getUUID(), grave);
            grave.setLocationDeath(finalLocationDeath);
            grave.setInventory(graveManager.getGraveInventory(
                    grave, (LivingEntity) victim, itemStackList, getRemovedItemStacks((LivingEntity) victim), null));
            grave.setEquipmentMap(equipmentMap != null ? equipmentMap :
                    (!versionManager.is_v1_7() ? entityManager.getEquipmentMap((LivingEntity) victim, grave) : new HashMap<>()));
            dataManager.addGrave(grave);

            if (victim instanceof Player) {
                Player player = (Player) victim;
                if (plugin.getConfig("noteblockapi.enabled", grave).getBoolean("noteblockapi.enabled")
                        && plugin.getIntegrationManager().hasNoteBlockAPI()) {
                    String deathReason = victim.getLastDamageCause() != null
                            ? victim.getLastDamageCause().getCause().name()
                            : "UNKNOWN";
                    String nbsSound = null;
                    for (String cause : plugin.getConfig("noteblockapi.death-causes", grave)
                            .getStringList("noteblockapi.death-causes")) {
                        String[] parts = cause.split(": ");
                        if (parts.length == 2 && parts[0].equalsIgnoreCase(deathReason)) {
                            nbsSound = parts[1].trim();
                            break;
                        }
                    }
                    if (nbsSound == null) {
                        nbsSound = plugin.getConfig("noteblockapi.nbs-sound", grave).getString("noteblockapi.nbs-sound");
                    }
                    if (plugin.getConfig("noteblockapi.play-locally", grave).getBoolean("noteblockapi.play-locally")) {
                        plugin.getIntegrationManager().getNoteBlockAPI().playSongForPlayer(player, nbsSound);
                    } else {
                        plugin.getIntegrationManager().getNoteBlockAPI().playSongForAllPlayers(nbsSound);
                    }
                } else {
                    plugin.getEntityManager().playPlayerSound("sound.grave-create", player, grave);
                }
            }

            if (integrationManager.hasMultiPaper()) {
                integrationManager.getMultiPaper().notifyGraveCreation(grave);
            }

            placeGraveBlocks(grave, locationMap, (LivingEntity) victim);
            plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + victim + " via GravesX API", 1);

        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while creating grave " + grave.getUUID() + " for entity " + victim + ".");
            plugin.logStackTrace(e);
        }
    }

    private List<ItemStack> getRemovedItemStacks(@NotNull LivingEntity livingEntity) {
        List<ItemStack> out = new ArrayList<>();
        var map = plugin.getCacheManager().getRemovedItemStackMap();
        if (map.containsKey(livingEntity.getUniqueId())) {
            out.addAll(map.get(livingEntity.getUniqueId()));
            map.remove(livingEntity.getUniqueId());
        }
        return out;
    }

    private void placeGraveBlocks(@NotNull Grave grave,
                                  @NotNull Map<Location, BlockData.BlockType> locationMap,
                                  @NotNull LivingEntity livingEntity) {
        for (Map.Entry<Location, BlockData.BlockType> entry : locationMap.entrySet()) {
            Location loc = entry.getKey().clone();
            int offsetX = 0, offsetY = 0, offsetZ = 0;
            switch (entry.getValue()) {
                case NORMAL:
                    offsetX = plugin.getConfig("placement.offset.x", grave).getInt("placement.offset.x");
                    offsetY = plugin.getConfig("placement.offset.y", grave).getInt("placement.offset.y");
                    offsetZ = plugin.getConfig("placement.offset.z", grave).getInt("placement.offset.z");
                    // fall-through to apply offsets
                case DEATH:
                default:
                    break;
            }
            loc.add(offsetX, offsetY, offsetZ);

            GraveBlockPlaceEvent evt = new GraveBlockPlaceEvent(grave, loc, entry.getValue(),
                    entry.getKey().getBlock(), livingEntity);
            plugin.getServer().getPluginManager().callEvent(evt);
            if (evt.isCancelled()) continue;

            plugin.getGraveManager().placeGrave(evt.getLocation(), grave);
            plugin.getEntityManager().sendMessage("message.block", livingEntity, loc, grave);
            plugin.getEntityManager().runCommands("event.command.block", livingEntity, evt.getLocation(), grave);
        }
    }
}