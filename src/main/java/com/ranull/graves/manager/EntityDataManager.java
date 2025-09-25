package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.type.Grave;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages entity data and interactions within the Graves plugin.
 */
public class EntityDataManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    private static Method SERVER_GET_ENTITY;

    static {
        try {
            SERVER_GET_ENTITY = org.bukkit.Server.class.getMethod("getEntity", UUID.class);
        } catch (Throwable ignored) { /* not available */ }
    }

    /**
     * Initializes the EntityDataManager with the specified plugin instance.
     *
     * @param plugin the Graves plugin instance.
     */
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
        EntityData entityData = new EntityData(location.clone(), entityUUID, graveUUID, type);

        plugin.getDataManager().addEntityData(entityData);

        if (plugin.getIntegrationManager().hasMultiPaper()) {
            plugin.getIntegrationManager().getMultiPaper().notifyEntityCreation(entityData);
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
        if (plugin.getDataManager().hasChunkData(location)) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(location);

            if (chunkData.getEntityDataMap().containsKey(uuid)) {
                return chunkData.getEntityDataMap().get(uuid);
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

        return entityData != null && plugin.getCacheManager().getGraveMap()
                .containsKey(entityData.getUUIDGrave())
                ? plugin.getCacheManager().getGraveMap().get(entityData.getUUIDGrave()) : null;
    }

    /**
     * Retrieves a grave for a specified entity.
     *
     * @param entity the entity for which to retrieve the grave.
     * @return the grave, or null if not found.
     */
    public Grave getGrave(Entity entity) {
        return getGrave(entity.getLocation(), entity.getUniqueId());
    }

    /**
     * Removes entity data for a specified entity data.
     *
     * @param entityData the entity data to remove.
     */
    public void removeEntityData(EntityData entityData) {
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

        for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getCacheManager().getChunkMap().entrySet()) {
            ChunkData chunkData = chunkDataEntry.getValue();

            if (chunkData.isLoaded()) {
                for (EntityData entityData : new ArrayList<>(chunkData.getEntityDataMap().values())) {
                    if (entityData != null && grave.getUUID().equals(entityData.getUUIDGrave())) {
                        entityDataList.add(entityData);
                    }
                }
            }
        }

        return entityDataList;
    }

    /**
     * Retrieves a map of entity data and their corresponding entities from a list of entity data.
     *
     * @param entityDataList the list of entity data to map.
     * @return the map of entity data and entities.
     */
    public Map<EntityData, Entity> getEntityDataMap(List<EntityData> entityDataList) {
        Map<EntityData, Entity> entityDataMap = new HashMap<>();

        for (EntityData entityData : entityDataList) {
            Location location = entityData.getLocation();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }

            Entity found = fastGetEntity(entityData.getUUIDEntity());
            if (found != null) {
                entityDataMap.put(entityData, found);
                continue;
            }

            int cx = location.getBlockX() >> 4;
            int cz = location.getBlockZ() >> 4;
            if (!world.isChunkLoaded(cx, cz)) {
                continue;
            }

            Entity scanned = scanChunkForEntityRegionSafe(world, cx, cz, entityData.getUUIDEntity(), location);
            if (scanned != null) {
                entityDataMap.put(entityData, scanned);
            }
        }

        return entityDataMap;
    }

    /**
     * Removes a list of entity data.
     *
     * @param entityDataList the list of entity data to remove.
     */
    public void removeEntityData(List<EntityData> entityDataList) {
        List<EntityData> removedEntityDataList = new ArrayList<>();

        for (EntityData entityData : entityDataList) {
            Location location = entityData.getLocation();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }

            // If we can resolve the entity quickly/safely, mark for removal.
            Entity found = fastGetEntity(entityData.getUUIDEntity());
            if (found != null) {
                removedEntityDataList.add(entityData);
                continue;
            }

            int cx = location.getBlockX() >> 4;
            int cz = location.getBlockZ() >> 4;
            if (!world.isChunkLoaded(cx, cz)) {
                continue;
            }

            Entity scanned = scanChunkForEntityRegionSafe(world, cx, cz, entityData.getUUIDEntity(), location);
            if (scanned != null) {
                removedEntityDataList.add(entityData);
            }
        }

        plugin.getDataManager().removeEntityData(removedEntityDataList);
    }

    /**
     * Try fast, version-friendly entity lookup:
     *  - World#getEntity(UUID) when present (Paper/Bukkit newer)
     *  - Server#getEntity(UUID) when present (Paper)
     * Returns null if unavailable or not found.
     */
    private Entity fastGetEntity(UUID uuid) {
        try {
            if (SERVER_GET_ENTITY != null) {
                Object e = SERVER_GET_ENTITY.invoke(plugin.getServer(), uuid);
                if (e instanceof Entity) return (Entity) e;
            }
        } catch (Throwable ignored) { /* continue */ }

        return null;
    }

    /**
     * Scan a loaded chunk for a specific entity UUID on the correct region thread if possible.
     * If a region scheduler is available, the scan is executed in-region and we await briefly.
     * If not (legacy servers), we fall back to a direct on-thread scan (original behavior).
     */
    private Entity scanChunkForEntityRegionSafe(World world, int cx, int cz, UUID uuid, Location anchor) {
        // If we have a region-aware scheduler, use it to do the scan safely.
        var sched = plugin.getGravesXScheduler();
        if (sched != null) {
            final AtomicReference<Entity> ref = new AtomicReference<>(null);
            final CountDownLatch latch = new CountDownLatch(1);

            sched.execute(anchor, () -> {
                try {
                    Chunk chunk = world.getChunkAt(cx, cz);
                    for (Entity e : chunk.getEntities()) {
                        if (uuid.equals(e.getUniqueId())) {
                            ref.set(e);
                            break;
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    latch.countDown();
                }
            });

            try {
                latch.await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return ref.get();
        }

        try {
            Chunk chunk = world.getChunkAt(cx, cz);
            for (Entity e : chunk.getEntities()) {
                if (uuid.equals(e.getUniqueId())) {
                    return e;
                }
            }
        } catch (Throwable ignored) {
            // Keep legacy behavior forgiving.
        }
        return null;
    }
}