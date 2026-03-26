package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The HologramManager class is responsible for managing holograms associated with graves.
 */
public class HologramManager extends EntityDataManager {
    private final Graves plugin;

    public HologramManager(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * Creates a hologram at the specified location for a given grave.
     *
     * @param location the hologram location
     * @param grave    the grave
     */
    public void createHologram(Location location, Grave grave) {
        if (grave == null) {
            plugin.debugMessage("[Holograms] createHologram skipped: grave is null", 1);
            return;
        }

        plugin.debugMessage("[Holograms] Creating hologram for grave=" + grave.getUUID()
                + " at " + (location != null ? toLocKey(location) : "null"), 1);

        if (plugin.getVersionManager().isHasTextDisplays()) {
            plugin.debugMessage("[Holograms] Using TextDisplay backend for grave=" + grave.getUUID(), 2);
            plugin.getTextDisplayManager().createHologram(location, grave);
        } else {
            plugin.debugMessage("[Holograms] Using ArmorStand backend for grave=" + grave.getUUID(), 2);
            plugin.getArmorStandManager().createHologram(location, grave);
        }
    }

    /**
     * Removes all holograms associated with a grave.
     * <p>
     * This is the only supported public removal entrypoint.
     * </p>
     *
     * @param grave the grave whose holograms should be removed
     */
    public void removeHologram(Grave grave) {
        if (grave == null) {
            plugin.debugMessage("[Holograms] removeHologram(grave) skipped: grave is null", 1);
            return;
        }

        if (grave.getUUID() == null) {
            plugin.debugMessage("[Holograms] removeHologram(grave) skipped: grave UUID is null", 1);
            return;
        }

        plugin.debugMessage("[Holograms] removeHologram(grave=" + grave.getUUID() + ") starting", 1);

        if (plugin.getVersionManager().isHasTextDisplays()) {
            plugin.debugMessage("[Holograms] Routing grave=" + grave.getUUID()
                    + " removal to TextDisplayManager and ArmorStandManager", 2);

            plugin.getTextDisplayManager().removeHologram(grave);
            plugin.getArmorStandManager().removeHologram(grave);
        } else {
            plugin.debugMessage("[Holograms] Routing grave=" + grave.getUUID()
                    + " removal to ArmorStandManager only", 2);

            plugin.getArmorStandManager().removeHologram(grave);
        }

        plugin.debugMessage("[Holograms] removeHologram(grave=" + grave.getUUID() + ") finished dispatch", 1);
    }

    /**
     * @deprecated Use {@link #removeHologram(Grave)} instead.
     *
     * @param entityDataList the old entity-data list removal input
     */
    @Deprecated(forRemoval = true)
    public void removeHologram(List<EntityData> entityDataList) {
        String message = "HologramManager#removeHologram(List<EntityData>) is no longer supported. "
                + "Use removeHologram(Grave) instead.";

        plugin.getLogger().severe(message + " size=" + (entityDataList != null ? entityDataList.size() : 0));
        plugin.debugMessage("[Holograms] " + message, 1);

        throw new UnsupportedOperationException(message);
    }

    /**
     * @deprecated Use {@link #removeHologram(Grave)} instead.
     *
     * @param entityData the old entity-data removal input
     */
    @Deprecated(forRemoval = true)
    public void removeHologram(EntityData entityData) {
        String message = "HologramManager#removeHologram(EntityData) is no longer supported. "
                + "Use removeHologram(Grave) instead.";

        plugin.getLogger().severe(message + " entity=" + (entityData != null ? entityData.getUUIDEntity() : "null"));
        plugin.debugMessage("[Holograms] " + message, 1);

        throw new UnsupportedOperationException(message);
    }

    /**
     * @deprecated Use {@link #removeHologram(Grave)} instead.
     *
     * @param entityDataMap the old entity/entity-data map removal input
     */
    @Deprecated(forRemoval = true)
    public void removeHologram(Map<EntityData, Entity> entityDataMap) {
        String message = "HologramManager#removeHologram(Map<EntityData, Entity>) is no longer supported. "
                + "Use removeHologram(Grave) instead.";

        plugin.getLogger().severe(message + " size=" + (entityDataMap != null ? entityDataMap.size() : 0));
        plugin.debugMessage("[Holograms] " + message, 1);

        throw new UnsupportedOperationException(message);
    }

    /**
     * Purges lingering hologram entities that appear to belong to GravesX.
     */
    public void purgeLingeringHolograms() {
        plugin.debugMessage("[Cleanup] Starting hologram purge dispatch", 1);

        if (plugin.getVersionManager().isHasTextDisplays()) {
            plugin.debugMessage("[Cleanup] Purging TextDisplay and ArmorStand holograms", 2);
            plugin.getTextDisplayManager().purgeLingeringHolograms();
            plugin.getArmorStandManager().purgeLingeringHolograms();
        } else {
            plugin.debugMessage("[Cleanup] Purging ArmorStand holograms only", 2);
            plugin.getArmorStandManager().purgeLingeringHolograms();
        }

        plugin.debugMessage("[Cleanup] Finished hologram purge dispatch", 1);
    }

    /**
     * Returns the cached grave by UUID (if present).
     *
     * @param graveUUID the grave UUID
     * @return the grave, or null if not cached
     */
    public Grave hasGrave(UUID graveUUID) {
        if (graveUUID == null) {
            plugin.debugMessage("[Holograms] hasGrave lookup skipped: graveUUID is null", 2);
            return null;
        }

        Grave grave = plugin.getCacheManager().getGraveMap().get(graveUUID);

        plugin.debugMessage("[Holograms] hasGrave lookup for " + graveUUID + " -> "
                + (grave != null ? "hit" : "miss"), 3);

        return grave;
    }

    private String toLocKey(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }

        return loc.getWorld().getName() + ":"
                + loc.getBlockX() + ":"
                + loc.getBlockY() + ":"
                + loc.getBlockZ();
    }
}