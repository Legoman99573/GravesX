package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
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
     */
    public void createHologram(Location location, Grave grave) {
        if (plugin.getVersionManager().isHasTextDisplays()) {
            plugin.getTextDisplayManager().createHologram(location, grave);
        } else {
            plugin.getArmorStandManager().createHologram(location, grave);
        }
    }

    /**
     * Removes all holograms associated with a grave.
     */
    public void removeHologram(Grave grave) {
        removeHologram(getLoadedEntityDataList(grave));
    }

    /**
     * Removes multiple holograms associated with a list of entity data records.
     */
    public void removeHologram(List<EntityData> entityDataList) {
        if (entityDataList == null || entityDataList.isEmpty()) {
            return;
        }

        Map<EntityData, Entity> entityDataMap = getEntityDataMap(entityDataList);
        removeHologram(entityDataMap);
    }

    /**
     * Removes a specific hologram associated with an {@link EntityData} record.
     */
    public void removeHologram(EntityData entityData) {
        if (entityData instanceof HologramData hologramData) {
            if (hologramData.getBackend() == HologramData.Backend.TEXT_DISPLAY) {
                plugin.getTextDisplayManager().removeHologram(hologramData);
            } else {
                plugin.getArmorStandManager().removeHologram(hologramData);
            }
            return;
        }

        if (plugin.getVersionManager().isHasTextDisplays()) {
            plugin.getTextDisplayManager().removeHologram(entityData);
        } else {
            plugin.getArmorStandManager().removeHologram(entityData);
        }
    }

    /**
     * Removes multiple holograms associated with a map of entity data to entities.
     */
    public void removeHologram(Map<EntityData, Entity> entityDataMap) {

        if (entityDataMap.isEmpty()) return;

        HologramData.Backend backend = null;

        for (EntityData data : entityDataMap.keySet()) {
            if (data instanceof HologramData h) {
                backend = h.getBackend();
                break;
            }
        }

        if (backend == HologramData.Backend.TEXT_DISPLAY) {
            plugin.getTextDisplayManager().removeHologram(entityDataMap);
        } else {
            plugin.getArmorStandManager().removeHologram(entityDataMap);
        }
    }

    /**
     * Purges lingering hologram entities that appear to belong to GravesX.
     */
    public void purgeLingeringHolograms() {
        if (plugin.getVersionManager().isHasTextDisplays()) {
            plugin.getTextDisplayManager().purgeLingeringHolograms();
            plugin.getArmorStandManager().purgeLingeringHolograms();
        } else {
            plugin.getArmorStandManager().purgeLingeringHolograms();
        }
    }

    /**
     * Returns the cached grave by UUID (if present).
     */
    public Grave hasGrave(UUID graveUUID) {
        return plugin.getCacheManager().getGraveMap().get(graveUUID);
    }
}