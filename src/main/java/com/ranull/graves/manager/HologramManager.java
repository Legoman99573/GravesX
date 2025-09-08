package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        if (location == null) {
            plugin.debugMessage("[Holograms] createHologram(): location was null for grave " + grave.getUUID(), 2);
            return;
        }
        if (location.getWorld() == null) {
            plugin.debugMessage("[Holograms] createHologram(): world was null for grave " + grave.getUUID(), 2);
            return;
        }

        boolean enabled = !plugin.getVersionManager().is_v1_7()
                && plugin.getConfig("hologram.enabled", grave).getBoolean("hologram.enabled");
        if (!enabled) {
            plugin.debugMessage("[Holograms] Disabled by version/config for grave " + grave.getUUID(), 1);
            return;
        }

        double offsetX = plugin.getConfig("hologram.offset.x", grave).getDouble("hologram.offset.x");
        double offsetY = plugin.getConfig("hologram.offset.y", grave).getDouble("hologram.offset.y");
        double offsetZ = plugin.getConfig("hologram.offset.z", grave).getDouble("hologram.offset.z");
        boolean marker = plugin.getConfig("hologram.marker", grave).getBoolean("hologram.marker");
        double lineHeight = plugin.getConfig("hologram.height-line", grave).getDouble("hologram.height-line");
        List<String> lineList = new ArrayList<>(plugin.getConfig("hologram.line", grave).getStringList("hologram.line"));

        plugin.debugMessage(String.format(
                "[Holograms] Creating hologram for grave %s at %s (offset=%.2f/%.2f/%.2f, marker=%s, lineHeight=%.2f, lines=%d)",
                grave.getUUID(), location, offsetX, offsetY, offsetZ, marker, lineHeight, lineList.size()), 1);

        Location base = LocationUtil.roundLocation(location)
                .add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);

        Collections.reverse(lineList);

        int lineNumber = 0;
        int created = 0;

        for (String rawLine : lineList) {
            try {
                base.add(0, lineHeight, 0);

                if (base.getWorld() == null) {
                    plugin.debugMessage("[Holograms] World became null during spawn; aborting.", 2);
                    break;
                }

                int finalLineNumber = lineNumber;
                ArmorStand stand = base.getWorld().spawn(base, ArmorStand.class, as -> {
                    as.setVisible(false);
                    as.setGravity(false);
                    as.setCustomNameVisible(true);
                    as.setSmall(true);

                    String parsed = StringUtil.parseString(rawLine, base, grave, plugin);
                    if (plugin.getIntegrationManager().hasMiniMessage()) {
                        as.setCustomName(MiniMessage.parseString(parsed));
                        plugin.debugMessage("[Holograms] Using MiniMessage for line " + finalLineNumber + " on " + grave.getUUID(), 1);
                    } else {
                        as.setCustomName(parsed);
                    }

                    if (!plugin.getVersionManager().is_v1_7()) {
                        try {
                            as.setMarker(marker);
                        } catch (NoSuchMethodError ignored) {
                            plugin.debugMessage("[Holograms] setMarker not available on this version.", 1);
                        }
                    }

                    if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                        try {
                            as.setInvulnerable(true);
                        } catch (Throwable ignored) {
                            plugin.debugMessage("[Holograms] setInvulnerable not available on this version.", 1);
                        }
                    }

                    // Tags (if available)
                    if (plugin.getVersionManager().hasScoreboardTags()) {
                        try {
                            as.getScoreboardTags().add("graveHologram");
                            as.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());
                        } catch (Throwable ignored) {
                            plugin.debugMessage("[Holograms] Failed to add scoreboard tags.", 2);
                        }
                    }
                });

                HologramData data = new HologramData(base.clone(), stand.getUniqueId(), grave.getUUID(), lineNumber);
                plugin.getDataManager().addHologramData(data);
                plugin.debugMessage("[Holograms] Spawned ArmorStand " + stand.getUniqueId()
                        + " for grave " + grave.getUUID() + " (line " + lineNumber + ")", 1);

                if (plugin.getIntegrationManager().hasMultiPaper()) {
                    try {
                        plugin.getIntegrationManager().getMultiPaper().notifyHologramCreation(data);
                        plugin.debugMessage("[Holograms] MultiPaper notified for hologram " + stand.getUniqueId(), 1);
                    } catch (Throwable t) {
                        plugin.debugMessage("[Holograms] MultiPaper notify failed: " + t.getMessage(), 2);
                    }
                }

                created++;
                lineNumber++;

            } catch (Throwable t) {
                plugin.debugMessage("[Holograms] Failed to spawn hologram line for grave "
                        + grave.getUUID() + ": " + t.getMessage(), 2);
            }
        }

        plugin.debugMessage("[Holograms] Created " + created + " hologram line(s) for grave " + grave.getUUID(), 1);
    }

    /**
     * Removes all holograms associated with a grave.
     *
     * @param grave The grave whose holograms should be removed.
     */
    public void removeHologram(Grave grave) {
        List<EntityData> list = getLoadedEntityDataList(grave);
        plugin.debugMessage("[Holograms] removeHologram(grave=" + grave.getUUID() + ") loaded entities: " + list.size(), 1);
        removeHologram(getEntityDataMap(list));
    }

    /**
     * Removes a specific hologram associated with an entity data.
     *
     * @param entityData The entity data of the hologram to remove.
     */
    public void removeHologram(EntityData entityData) {
        if (entityData == null) {
            plugin.debugMessage("[Holograms] removeHologram(EntityData) called with null", 2);
            return;
        }
        plugin.debugMessage("[Holograms] removeHologram(entityData=" + entityData.getUUIDEntity() + ")", 1);
        removeHologram(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes multiple holograms associated with a map of entity data to entities.
     *
     * @param entityDataMap The map of entity data to entities.
     */
    public void removeHologram(Map<EntityData, Entity> entityDataMap) {
        if (entityDataMap == null || entityDataMap.isEmpty()) {
            plugin.debugMessage("[Holograms] removeHologram(map) called with empty map", 1);
            return;
        }

        List<EntityData> entityDataList = new ArrayList<>();
        int removed = 0;

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            EntityData ed = entry.getKey();
            Entity entity = entry.getValue();

            if (entity == null) {
                plugin.debugMessage("[Holograms] Null entity for data " + (ed != null ? ed.getUUIDEntity() : "null"), 2);
                if (ed != null) entityDataList.add(ed);
                continue;
            }

            try {
                if (entity instanceof ArmorStand) {
                    ArmorStand as = (ArmorStand) entity;

                    if (as.isValid()) {
                        as.remove();
                        plugin.debugMessage("[Holograms] Removed ArmorStand " + as.getUniqueId(), 1);
                    } else {
                        plugin.debugMessage("[Holograms] ArmorStand already invalid " + as.getUniqueId(), 1);
                    }

                    plugin.getGravesXScheduler().runTaskLater(as.getLocation(), () -> {
                        try {
                            if (as.isValid()) {
                                as.remove();
                                plugin.debugMessage("[Holograms] Post-check removed ArmorStand " + as.getUniqueId(), 1);
                            }
                        } catch (Throwable t) {
                            plugin.debugMessage("[Holograms] Post-check removal failed: " + t.getMessage(), 2);
                        }
                    }, 1L);

                } else {
                    entity.remove();
                    plugin.debugMessage("[Holograms] Removed entity " + entity.getUniqueId()
                            + " type=" + entity.getType(), 1);
                }

                if (ed != null) entityDataList.add(ed);
                removed++;

            } catch (Throwable t) {
                plugin.debugMessage("[Holograms] Failed removing entity "
                        + entity.getUniqueId() + ": " + t.getMessage(), 2);
            }
        }

        if (!entityDataList.isEmpty()) {
            plugin.getDataManager().removeEntityData(entityDataList);
            plugin.debugMessage("[Holograms] Removed " + entityDataList.size()
                    + " hologram data entr" + (entityDataList.size() == 1 ? "y" : "ies"), 1);
        } else {
            plugin.debugMessage("[Holograms] No hologram data entries to remove (entities removed=" + removed + ")", 1);
        }
    }
}