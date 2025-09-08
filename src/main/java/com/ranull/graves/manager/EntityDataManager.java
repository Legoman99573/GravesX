package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.type.Grave;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Manages entity data and interactions within the Graves plugin.
 */
public class EntityDataManager {
    /**
     * The main plugin instance associated with Graves.
     */
    private final Graves plugin;

    public EntityDataManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates entity data for a specified entity and grave.
     *
     * @param entity the entity for which to create the data.
     * @param grave  the grave associated with the entity.
     * @param type   the type of entity data.
     */
    public void createEntityData(Entity entity, Grave grave, EntityData.Type type) {
        if (entity == null || grave == null) {
            plugin.debugMessage("[EntityData] createEntityData(Entity) called with nulls", 2);
            return;
        }
        createEntityData(entity.getLocation(), entity.getUniqueId(), grave.getUUID(), type);
    }

    /**
     * Creates entity data for a specified location, entity UUID, grave UUID, and entity data type.
     *
     * @param location   the location of the entity.
     * @param entityUUID the UUID of the entity.
     * @param graveUUID  the UUID of the grave.
     * @param type       the type of entity data.
     */
    public void createEntityData(Location location, UUID entityUUID, UUID graveUUID, EntityData.Type type) {
        if (location == null || entityUUID == null || graveUUID == null || type == null) {
            plugin.debugMessage("[EntityData] createEntityData(Location,UUID,UUID,Type) null argument", 2);
            return;
        }

        EntityData entityData = new EntityData(location.clone(), entityUUID, graveUUID, type);
        plugin.getDataManager().addEntityData(entityData);
        plugin.debugMessage("[EntityData] Added entity data " + entityUUID + " for grave " + graveUUID + " type=" + type, 1);

        if (plugin.getIntegrationManager().hasMultiPaper()) {
            try {
                plugin.getIntegrationManager().getMultiPaper().notifyEntityCreation(entityData);
                plugin.debugMessage("[EntityData] MultiPaper notified for entity " + entityUUID, 1);
            } catch (Throwable t) {
                plugin.debugMessage("[EntityData] MultiPaper notify failed: " + t.getMessage(), 2);
            }
        }
    }

    /**
     * Retrieves entity data for a specified location and entity UUID.
     *
     * @param location the location of the entity.
     * @param uuid     the UUID of the entity.
     * @return the entity data, or null if not found.
     */
    public EntityData getEntityData(Location location, UUID uuid) {
        if (location == null || uuid == null) return null;

        if (plugin.getDataManager().hasChunkData(location)) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(location);
            EntityData ed = chunkData.getEntityDataMap().get(uuid);
            if (ed != null) {
                plugin.debugMessage("[EntityData] Hit entity data " + uuid + " in chunk cache", 1);
                return ed;
            }
        }
        return null;
    }

    /**
     * Retrieves a grave for a specified location and entity UUID.
     *
     * @param location the location of the entity.
     * @param uuid     the UUID of the entity.
     * @return the grave, or null if not found.
     */
    public Grave getGrave(Location location, UUID uuid) {
        EntityData entityData = getEntityData(location, uuid);
        if (entityData == null) return null;

        Map<UUID, Grave> graves = plugin.getCacheManager().getGraveMap();
        Grave g = graves.get(entityData.getUUIDGrave());
        plugin.debugMessage("[EntityData] getGrave for entity " + uuid + " -> " + (g != null ? g.getUUID() : "null"), 1);
        return g;
    }

    /**
     * Retrieves a grave for a specified entity.
     *
     * @param entity the entity for which to retrieve the grave.
     * @return the grave, or null if not found.
     */
    public Grave getGrave(Entity entity) {
        if (entity == null) return null;
        return getGrave(entity.getLocation(), entity.getUniqueId());
    }

    /**
     * Removes entity data for a specified entity data.
     *
     * @param entityData the entity data to remove.
     */
    public void removeEntityData(EntityData entityData) {
        if (entityData == null) {
            plugin.debugMessage("[EntityData] removeEntityData(single) called with null", 2);
            return;
        }
        removeEntityData(Collections.singletonList(entityData));
    }

    /**
     * Retrieves a list of loaded entity data associated with a specified grave.
     *
     * @param grave the grave for which to retrieve the loaded entity data.
     * @return the list of loaded entity data.
     */
    public List<EntityData> getLoadedEntityDataList(Grave grave) {
        List<EntityData> entityDataList = new ArrayList<>();
        if (grave == null) return entityDataList;

        for (ChunkData chunkData : plugin.getCacheManager().getChunkMap().values()) {
            if (!chunkData.isLoaded()) continue;

            for (EntityData entityData : new ArrayList<>(chunkData.getEntityDataMap().values())) {
                if (entityData != null && grave.getUUID().equals(entityData.getUUIDGrave())) {
                    entityDataList.add(entityData);
                }
            }
        }

        plugin.debugMessage("[EntityData] Loaded entity data count for grave "
                + grave.getUUID() + ": " + entityDataList.size(), 1);
        return entityDataList;
    }

    /**
     * Retrieves a map of entity data and their corresponding entities from a list of entity data.
     * Prefers direct lookups by UUID; falls back to scanning chunk entities.
     *
     * @param entityDataList the list of entity data to map.
     * @return the map of entity data and entities.
     */
    public Map<EntityData, Entity> getEntityDataMap(List<EntityData> entityDataList) {
        Map<EntityData, Entity> entityDataMap = new HashMap<>();
        if (entityDataList == null || entityDataList.isEmpty()) return entityDataMap;

        if (isOnRegionThread()) {
            plugin.debugMessage("[EntityData] getEntityDataMap called off main/region thread; " +
                    "ensure callers use scheduler for world access", 2);
        }

        int resolvedDirect = 0;
        int resolvedScan = 0;
        int skippedUnloaded = 0;

        for (EntityData ed : entityDataList) {
            if (ed == null) continue;

            Location loc = ed.getLocation();
            UUID uuid = ed.getUUIDEntity();
            if (loc == null || uuid == null) continue;

            World w = loc.getWorld();
            if (w == null) continue;

            Entity e = tryGetEntityFast(w, uuid);
            if (e != null) {
                entityDataMap.put(ed, e);
                resolvedDirect++;
                continue;
            }

            Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                skippedUnloaded++;
                plugin.debugMessage("[EntityData] Chunk not loaded for entity " + uuid
                        + " at " + chunk.getX() + "," + chunk.getZ() + " (skipping scan)", 1);
                continue;
            }

            for (Entity ent : chunk.getEntities()) {
                if (uuid.equals(ent.getUniqueId())) {
                    entityDataMap.put(ed, ent);
                    resolvedScan++;
                    break;
                }
            }
        }

        plugin.debugMessage("[EntityData] Mapped entities: direct=" + resolvedDirect
                + ", scan=" + resolvedScan + ", skippedUnloaded=" + skippedUnloaded
                + ", totalOut=" + entityDataMap.size(), 1);
        return entityDataMap;
    }

    /**
     * Removes a list of entity data (DB rows) if their entities are present in the world.
     *
     * @param entityDataList the list of entity data to remove.
     */
    public void removeEntityData(List<EntityData> entityDataList) {
        if (entityDataList == null || entityDataList.isEmpty()) return;

        if (isOnRegionThread()) {
            plugin.debugMessage("[EntityData] removeEntityData called off main/region thread; " +
                    "ensure callers use scheduler for world access", 2);
        }

        List<EntityData> removedEntityDataList = new ArrayList<>();
        int scanned = 0, removed = 0, skippedUnloaded = 0;

        for (EntityData ed : entityDataList) {
            if (ed == null) continue;
            Location loc = ed.getLocation();
            UUID uuid = ed.getUUIDEntity();
            if (loc == null || uuid == null) continue;

            World w = loc.getWorld();
            if (w == null) continue;

            Entity e = tryGetEntityFast(w, uuid);
            if (e != null) {
                removedEntityDataList.add(ed);
                removed++;
                continue;
            }

            Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                skippedUnloaded++;
                plugin.debugMessage("[EntityData] Chunk not loaded for removal scan of " + uuid, 1);
                continue;
            }

            for (Entity ent : chunk.getEntities()) {
                scanned++;
                if (uuid.equals(ent.getUniqueId())) {
                    removedEntityDataList.add(ed);
                    removed++;
                    break;
                }
            }
        }

        if (!removedEntityDataList.isEmpty()) {
            plugin.getDataManager().removeEntityData(removedEntityDataList);
            plugin.debugMessage("[EntityData] Removed " + removedEntityDataList.size()
                    + " entity-data row(s) (scanned=" + scanned + ", skippedUnloaded=" + skippedUnloaded + ")", 1);
        } else {
            plugin.debugMessage("[EntityData] No entity-data rows to remove (entities not found)", 1);
        }
    }

    /**
     * Try to obtain an entity directly by UUID (World#getEntity or Bukkit#getEntity),
     * catching linkage errors for older servers. Returns null if not found.
     */
    private Entity tryGetEntityFast(World world, UUID uuid) {
        if (uuid == null) return null;

        try {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null && (world == null || e.getWorld() == world)) {
                return e;
            }
        } catch (Throwable t) {
            plugin.debugMessage("[EntityData] Bukkit#getEntity failed: " + t.getMessage(), 2);
        }
        return null;
    }

    /**
     * Returns true if running on a safe thread/region context.
     * Tries GravesX scheduler's introspection if present; falls back to Bukkit.isPrimaryThread().
     */
    private boolean isOnRegionThread() {
        try {
            Method m = plugin.getGravesXScheduler().getClass().getMethod("isRegionThread", Location.class);
            Object r = m.invoke(plugin.getGravesXScheduler(), (Location) null);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (NoSuchMethodException ignored) {
            // fall back
        } catch (Throwable t) {
            plugin.debugMessage("[EntityData] isRegionThread check failed: " + t.getMessage(), 2);
        }
        try {
            return !Bukkit.isPrimaryThread();
        } catch (Throwable ignored) {
            return false;
        }
    }
}