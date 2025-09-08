package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.Method;
import java.util.*;

/**
 * The HologramManager class is responsible for managing holograms associated with graves.
 */
public final class HologramManager extends EntityDataManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    /**
     * Initializes a new instance of the HologramManager class.
     *
     * @param plugin The plugin instance.
     */
    public HologramManager(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * Creates a hologram at the specified location for a given grave.
     *
     * @param location The location where the hologram should be created.
     * @param grave    The grave associated with the hologram.
     */
    public void createHologram(Location location, Grave grave) {
        if (!plugin.getVersionManager().is_v1_7()
                && plugin.getConfig("hologram.enabled", grave).getBoolean("hologram.enabled")) {
            double offsetX = plugin.getConfig("hologram.offset.x", grave).getDouble("hologram.offset.x");
            double offsetY = plugin.getConfig("hologram.offset.y", grave).getDouble("hologram.offset.y");
            double offsetZ = plugin.getConfig("hologram.offset.z", grave).getDouble("hologram.offset.z");
            boolean marker = plugin.getConfig("hologram.marker", grave).getBoolean("hologram.marker");
            location = LocationUtil.roundLocation(location)
                    .add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);
            List<String> lineList = plugin.getConfig("hologram.line", grave)
                    .getStringList("hologram.line");
            double lineHeight = plugin.getConfig("hologram.height-line", grave)
                    .getDouble("hologram.height-line");
            int lineNumber = 0;

            Collections.reverse(lineList);

            for (String line : lineList) {
                location.add(0, lineHeight, 0);

                if (location.getWorld() != null) {
                    ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
                    armorStand.setVisible(false);
                    armorStand.setGravity(false);
                    armorStand.setCustomNameVisible(true);
                    armorStand.setSmall(true);

                    // Use MiniMessage if available to format the custom name
                    if (plugin.getIntegrationManager().hasMiniMessage()) {
                        String newLine = StringUtil.parseString(line, location, grave, plugin);
                        armorStand.setCustomName(MiniMessage.parseString(newLine));
                    } else {
                        armorStand.setCustomName(StringUtil.parseString(line, location, grave, plugin));
                    }

                    if (!plugin.getVersionManager().is_v1_7()) {
                        try {
                            armorStand.setMarker(marker);
                        } catch (NoSuchMethodError ignored) {
                        }
                    }

                    if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                        armorStand.setInvulnerable(true);
                    }

                    if (plugin.getVersionManager().hasScoreboardTags()) {
                        armorStand.getScoreboardTags().add("graveHologram");
                        armorStand.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());
                    }

                    HologramData hologramData = new HologramData(location, armorStand.getUniqueId(),
                            grave.getUUID(), lineNumber);

                    plugin.getDataManager().addHologramData(hologramData);
                    lineNumber++;

                    if (plugin.getIntegrationManager().hasMultiPaper()) {
                        plugin.getIntegrationManager().getMultiPaper().notifyHologramCreation(hologramData);
                    }
                }
            }
        }
    }

    /**
     * Removes all holograms associated with a grave.
     *
     * @param grave The grave whose holograms should be removed.
     */
    public void removeHologram(Grave grave) {
        Map<EntityData, Entity> entityDataMap = getEntityDataMap(getLoadedEntityDataList(grave));
        removeHologram(entityDataMap);

        sweepAndRemoveByTags(grave);

        try {
            plugin.getDataManager().removeHologramData(grave);
        } catch (Throwable ignored) {
        }
    }
    /**
     * Removes a specific hologram associated with an entity data.
     *
     * @param entityData The entity data of the hologram to remove.
     */
    public void removeHologram(EntityData entityData) {
        if (entityData == null) return;

        Map<EntityData, Entity> map = getEntityDataMap(Collections.singletonList(entityData));
        Entity mapped = map.get(entityData);
        Location center = (mapped != null ? mapped.getLocation() : tryGetLocation(entityData));

        removeHologram(map);

        java.util.UUID graveId = extractGraveUUID(entityData);
        if (graveId != null && center != null) {
            sweepAndRemoveByTags(graveId, center);
        }
    }

    private java.util.UUID extractGraveUUID(EntityData data) {
        if (data == null) return null;
        try {
            Method m = data.getClass().getMethod("getUUIDGrave");
            Object o = m.invoke(data);
            if (o instanceof UUID) return (UUID) o;
        } catch (Throwable ignored) {}
        return null;
    }

    private Location tryGetLocation(EntityData data) {
        if (data == null) return null;
        try {
            Method m = data.getClass().getMethod("getLocationDeath");
            Object o = m.invoke(data);
            return (o instanceof Location) ? (Location) o : null;
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Removes multiple holograms associated with a map of entity data to entities.
     *
     * @param entityDataMap The map of entity data to entities.
     */
    public void removeHologram(Map<EntityData, Entity> entityDataMap) {
        if (entityDataMap == null || entityDataMap.isEmpty()) return;

        Runnable task = () -> {
            List<EntityData> removed = new ArrayList<>();

            for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
                Entity entity = entry.getValue();
                if (entity instanceof ArmorStand) {
                    ArmorStand as = (ArmorStand) entity;

                    if (as.isValid()) as.remove();

                    new BukkitRunnable() {
                        @Override public void run() {
                            if (as.isValid()) as.remove();
                        }
                    }.runTaskLater(plugin, 1L);
                } else if (entity != null && entity.isValid()) {
                    entity.remove();
                }

                removed.add(entry.getKey());
            }

            plugin.getDataManager().removeEntityData(removed);
        };

        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Sweeps for stray ArmorStands that look like our holograms (by scoreboard tags)
     * around the grave location and removes them. Main-thread safe to call.
     */
    private void sweepAndRemoveByTags(Grave grave) {
        Location base = grave.getLocationDeath();
        if (base == null || base.getWorld() == null) return;

        Runnable task = () -> {
            double r = 16.0;
            BoundingBox box = BoundingBox.of(base, r, r, r);
            String graveTag = tagFor(grave.getUUID());

            for (ArmorStand as : base.getWorld().getEntitiesByClass(ArmorStand.class)) {
                if (!as.isValid()) continue;
                if (!box.contains(as.getLocation().toVector())) continue;

                Set<String> tags = as.getScoreboardTags();
                if (tags.contains("graveHologram") && tags.contains(graveTag)) {
                    as.remove();
                    new BukkitRunnable() {
                        @Override public void run() {
                            if (as.isValid()) as.remove();
                        }
                    }.runTaskLater(plugin, 1L);
                }
            }
        };

        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }

    private void sweepAndRemoveByTags(java.util.UUID graveId, Location center) {
        if (graveId == null || center == null || center.getWorld() == null) return;

        Runnable task = () -> {
            double r = 16.0;
            BoundingBox box = BoundingBox.of(center, r, r, r);
            String graveTag = tagFor(graveId);

            for (org.bukkit.entity.ArmorStand as : center.getWorld().getEntitiesByClass(org.bukkit.entity.ArmorStand.class)) {
                if (!as.isValid()) continue;
                if (!box.contains(as.getLocation().toVector())) continue;

                java.util.Set<String> tags = as.getScoreboardTags();
                if (tags.contains("graveHologram") && tags.contains(graveTag)) {
                    as.remove();
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override public void run() {
                            if (as.isValid()) as.remove();
                        }
                    }.runTaskLater(plugin, 1L);
                }
            }
        };

        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }

    private static String tagFor(UUID id) {
        return "graveHologramGraveUUID:" + id;
    }
}
