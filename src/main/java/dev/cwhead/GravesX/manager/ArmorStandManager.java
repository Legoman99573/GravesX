package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import dev.cwhead.GravesX.keys.GraveHologramKeys;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorStandManager extends EntityDataManager {
    private final Graves plugin;

    public ArmorStandManager(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * Creates an ArmorStand hologram at the specified location for a given grave.
     * Spawns each ArmorStand on the region thread for that line location.
     */
    public void createHologram(Location location, Grave grave) {
        if (plugin.getVersionManager().is_v1_7()) return;

        if (!plugin.getConfigManager()
                .getConfigSection("hologram.enabled", grave)
                .getBoolean("hologram.enabled")) {
            return;
        }

        double offsetX = plugin.getConfigManager().getConfigSection("hologram.offset.x", grave).getDouble("hologram.offset.x");
        double offsetY = plugin.getConfigManager().getConfigSection("hologram.offset.y", grave).getDouble("hologram.offset.y");
        double offsetZ = plugin.getConfigManager().getConfigSection("hologram.offset.z", grave).getDouble("hologram.offset.z");
        boolean marker = plugin.getConfigManager().getConfigSection("hologram.marker", grave).getBoolean("hologram.marker");

        Location base = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);

        List<String> lineList = new ArrayList<>(
                plugin.getConfigManager().getConfigSection("hologram.line", grave).getStringList("hologram.line")
        );

        double lineHeight = plugin.getConfigManager().getConfigSection("hologram.height-line", grave).getDouble("hologram.height-line");
        int lineNumber = 0;

        Collections.reverse(lineList);

        for (String line : lineList) {
            Location lineLoc = base.clone().add(0, (lineNumber + 1) * lineHeight, 0);
            int finalLineNumber = lineNumber;

            executeRegion(lineLoc, () -> {
                if (lineLoc.getWorld() == null) return;

                ArmorStand armorStand = lineLoc.getWorld().spawn(lineLoc, ArmorStand.class);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setSmall(true);

                String locKey = toLocKey(grave.getLocationDeath());

                if (plugin.getVersionManager().hasPersistentData()) {
                    PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
                    pdc.set(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING, grave.getUUID().toString());
                    pdc.set(GraveHologramKeys.GRAVE_LOCATION, PersistentDataType.STRING, locKey);
                }

                String parsed = StringUtil.parseString(line, lineLoc, grave, plugin);
                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    armorStand.setCustomName(MiniMessage.parseString(parsed));
                } else {
                    armorStand.setCustomName(parsed);
                }

                if (!plugin.getVersionManager().is_v1_7()) {
                    try { armorStand.setMarker(marker); } catch (NoSuchMethodError ignored) {}
                }

                if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                    armorStand.setInvulnerable(true);
                }

                if (plugin.getVersionManager().hasScoreboardTags()) {
                    armorStand.getScoreboardTags().add("graveHologram");
                    armorStand.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());
                    armorStand.getScoreboardTags().add("graveHologramGraveLocation:" + locKey);
                }

                HologramData hologramData = new HologramData(lineLoc, armorStand.getUniqueId(), grave.getUUID(), finalLineNumber);
                plugin.getDataManager().addHologramData(hologramData);

                if (plugin.getIntegrationManager().hasMultiPaper()) {
                    plugin.getIntegrationManager().getMultiPaper().notifyHologramCreation(hologramData);
                }
            });

            lineNumber++;
        }
    }

    /**
     * Removes all ArmorStand holograms linked to the given grave.
     * <p>
     * This is the only supported public removal entrypoint.
     * It resolves hologram data directly from CacheManager using the grave UUID.
     * </p>
     *
     * @param grave the grave whose ArmorStand holograms should be removed
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

        plugin.debugMessage("[Holograms] removeHologram(grave=" + grave.getUUID() + ") resolving cached ArmorStand holograms", 1);

        List<HologramData> hologramDataList = getCachedHologramData(grave, HologramData.Backend.ARMOR_STAND);

        plugin.debugMessage("[Holograms] removeHologram(grave=" + grave.getUUID() + ") resolved "
                + hologramDataList.size() + " cached ArmorStand hologram(s)", 1);

        if (hologramDataList.isEmpty()) {
            plugin.debugMessage("[Holograms] No cached ArmorStand holograms found for grave=" + grave.getUUID(), 2);
            return;
        }

        removeResolvedHolograms(hologramDataList);
    }

    /**
     * @deprecated Use {@link #removeHologram(Grave)} instead.
     */
    @Deprecated(forRemoval = true)
    public void removeHologram(EntityData entityData) {
        String message = "ArmorStandManager#removeHologram(EntityData) is no longer supported. "
                + "Use removeHologram(Grave) instead.";

        plugin.getLogger().severe(message + " entity=" + (entityData != null ? entityData.getUUIDEntity() : "null"));
        plugin.debugMessage("[Holograms] " + message, 1);

        throw new UnsupportedOperationException(message);
    }

    /**
     * @deprecated Use {@link #removeHologram(Grave)} instead.
     */
    @Deprecated(forRemoval = true)
    public void removeHologram(Map<EntityData, Entity> entityDataMap) {
        String message = "ArmorStandManager#removeHologram(Map<EntityData, Entity>) is no longer supported. "
                + "Use removeHologram(Grave) instead.";

        plugin.getLogger().severe(message + " size=" + (entityDataMap != null ? entityDataMap.size() : 0));
        plugin.debugMessage("[Holograms] " + message, 1);

        throw new UnsupportedOperationException(message);
    }

    /**
     * Resolves cached hologram data for a grave/backend pair.
     *
     * @param grave the grave
     * @param backend the backend to match, or null for any backend
     * @return matching hologram data entries
     */
    private List<HologramData> getCachedHologramData(Grave grave, HologramData.Backend backend) {
        List<HologramData> hologramDataList = new ArrayList<>();

        if (grave == null || grave.getUUID() == null) {
            return hologramDataList;
        }

        for (EntityData entityData : plugin.getCacheManager().getEntityMap().values()) {
            if (!(entityData instanceof HologramData hologramData)) {
                continue;
            }

            if (!grave.getUUID().equals(hologramData.getUUIDGrave())) {
                continue;
            }

            if (backend != null && hologramData.getBackend() != backend) {
                continue;
            }

            hologramDataList.add(hologramData);
        }

        return hologramDataList;
    }

    /**
     * Removes already-resolved hologram data entries.
     *
     * @param hologramDataList the hologram data list to remove
     */
    private void removeResolvedHolograms(List<? extends EntityData> hologramDataList) {
        if (hologramDataList == null || hologramDataList.isEmpty()) {
            plugin.debugMessage("[Holograms] removeResolvedHolograms skipped: no hologram data", 2);
            return;
        }

        plugin.debugMessage("[Holograms] removeResolvedHolograms count=" + hologramDataList.size(), 1);

        Map<EntityData, Entity> entityDataMap = getEntityDataMap(new ArrayList<>(hologramDataList));

        plugin.debugMessage("[Holograms] Resolved " + entityDataMap.size()
                + " live entity reference(s) from " + hologramDataList.size() + " hologram data entrie(s)", 1);

        List<EntityData> removableEntityData = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            EntityData data = entry.getKey();
            if (data == null) {
                plugin.debugMessage("[Holograms] Skipping null EntityData entry during removal", 2);
                continue;
            }

            if (!(data instanceof HologramData hologramData)) {
                plugin.debugMessage("[Holograms] Skipping non-hologram EntityData uuid=" + data.getUUIDEntity(), 2);
                continue;
            }

            if (hologramData.getBackend() != HologramData.Backend.ARMOR_STAND) {
                plugin.debugMessage("[Holograms] Skipping non-ArmorStand hologram uuid=" + data.getUUIDEntity()
                        + ", backend=" + hologramData.getBackend(), 2);
                continue;
            }

            removableEntityData.add(data);

            Entity finalEntity = entry.getValue();
            Location graveLocation = null;

            try {
                graveLocation = data.getLocation();
            } catch (Throwable ignored) {
            }

            String finalLocKey = graveLocation != null ? toLocKey(graveLocation) : null;
            String finalLocTag = finalLocKey != null ? "graveHologramGraveLocation:" + finalLocKey : null;
            Location finalGraveLocation = graveLocation;

            plugin.debugMessage("[Holograms] Scheduling ArmorStand hologram removal for entity="
                    + data.getUUIDEntity() + ", grave=" + data.getUUIDGrave()
                    + ", location=" + finalLocKey, 1);

            Runnable remover = () -> {
                int removedCount = 0;

                try {
                    if (finalEntity != null && finalEntity.isValid()) {
                        finalEntity.remove();
                        removedCount++;
                        plugin.debugMessage("[Holograms] Removed direct ArmorStand entity="
                                + finalEntity.getUniqueId(), 2);
                    }
                } catch (Throwable t) {
                    plugin.debugMessage("[Holograms] Failed direct entity removal for uuid="
                            + data.getUUIDEntity() + ": " + t.getMessage(), 1);
                }

                if (finalGraveLocation == null || finalGraveLocation.getWorld() == null) {
                    plugin.debugMessage("[Holograms] No valid location world for hologram entity="
                            + data.getUUIDEntity() + ", skipping world sweep", 2);
                    return;
                }

                try {
                    for (Entity e : finalGraveLocation.getWorld().getEntities()) {
                        if (!(e instanceof ArmorStand)) continue;
                        if (!e.isValid()) continue;
                        if (!e.getLocation().equals(finalGraveLocation)) continue;

                        boolean remove = false;

                        try {
                            Set<String> tags = e.getScoreboardTags();
                            if (finalLocTag != null && tags.contains(finalLocTag)) {
                                remove = true;
                            }
                        } catch (NoSuchMethodError ignored) {
                        }

                        try {
                            if (plugin.getVersionManager().hasPersistentData()) {
                                PersistentDataContainer pdc = e.getPersistentDataContainer();
                                String storedLoc = pdc.get(GraveHologramKeys.GRAVE_LOCATION, PersistentDataType.STRING);
                                if (finalLocKey != null && finalLocKey.equals(storedLoc)) {
                                    remove = true;
                                }
                            }
                        } catch (Throwable ignored) {
                        }

                        if (remove) {
                            e.remove();
                            removedCount++;
                            plugin.debugMessage("[Holograms] Removed matched ArmorStand entity="
                                    + e.getUniqueId() + " at " + finalLocKey, 2);
                        }
                    }

                    plugin.debugMessage("[Holograms] Removal sweep finished for hologram entity="
                            + data.getUUIDEntity() + ", removed=" + removedCount, 2);
                } catch (Throwable t) {
                    plugin.getLogger().severe(
                            "Failed removing holograms at world: " + finalGraveLocation.getWorld().getName() +
                                    ", x: " + finalGraveLocation.getBlockX() +
                                    ", y: " + finalGraveLocation.getBlockY() +
                                    ", z: " + finalGraveLocation.getBlockZ() + "."
                    );
                    plugin.logStackTrace(t);
                }
            };

            if (finalEntity != null) {
                plugin.debugMessage("[Holograms] Executing removal on entity region for entity="
                        + finalEntity.getUniqueId(), 2);
                executeRegion(finalEntity, remover);
            } else if (finalGraveLocation != null && finalGraveLocation.getWorld() != null) {
                plugin.debugMessage("[Holograms] Executing removal on location region for hologram entity="
                        + data.getUUIDEntity(), 2);
                executeRegion(finalGraveLocation, remover);
            } else {
                plugin.debugMessage("[Holograms] Executing fallback main-thread removal for hologram entity=" + data.getUUIDEntity(), 2);
                plugin.getServer().getScheduler().runTask(plugin, remover);
            }
        }

        if (!removableEntityData.isEmpty()) {
            plugin.debugMessage("[Holograms] Removing " + removableEntityData.size() + " hologram entity-data entrie(s) from DataManager records", 1);
            plugin.getDataManager().removeEntityData(removableEntityData);
        } else {
            plugin.debugMessage("[Holograms] No ArmorStand hologram entity-data entries qualified for DB/cache removal", 2);
        }
    }

    public void purgeLingeringHolograms() {
        plugin.debugMessage("[Cleanup] Starting ArmorStand hologram purge across all worlds", 1);

        for (World world : plugin.getServer().getWorlds()) {
            plugin.debugMessage("[Cleanup] Scanning world " + world.getName() + " for lingering ArmorStand holograms", 2);

            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                executeRegion(stand, () -> {
                    boolean hasHoloTag = false;
                    try {
                        if (plugin.getVersionManager().hasScoreboardTags()) {
                            hasHoloTag = stand.getScoreboardTags().contains("graveHologram");
                        }
                    } catch (Throwable ignored) {
                    }

                    boolean hasPdc = false;
                    try {
                        if (plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = stand.getPersistentDataContainer();
                            hasPdc = pdc.has(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING);
                        }
                    } catch (Throwable ignored) {
                    }

                    if (!hasHoloTag && !hasPdc) {
                        return;
                    }

                    UUID graveUuid = null;

                    try {
                        graveUuid = extractGraveUUIDFromStand(stand);
                    } catch (Throwable ignored) {
                    }

                    try {
                        if (plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = stand.getPersistentDataContainer();
                            String pdcUuid = pdc.get(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING);
                            if (pdcUuid != null) {
                                try {
                                    graveUuid = UUID.fromString(pdcUuid);
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }

                    if (graveUuid == null) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram missing grave UUID tag/PDC entity=" + stand.getUniqueId(), 2);
                        return;
                    }

                    Grave grave = null;
                    try {
                        grave = hasGrave(graveUuid);
                    } catch (Throwable ignored) {
                    }

                    if (grave == null) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram for missing grave " + graveUuid, 2);
                        return;
                    }

                    Location dbLoc = grave.getLocationDeath();
                    if (dbLoc == null || dbLoc.getWorld() == null) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram for grave " + graveUuid + " (DB location missing)", 2);
                        return;
                    }

                    String expectedLocKey = toLocKey(dbLoc);

                    String storedLocKey = null;
                    try {
                        storedLocKey = extractGraveLocationKeyFromStand(stand);
                    } catch (Throwable ignored) {
                    }

                    try {
                        if (plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = stand.getPersistentDataContainer();
                            String pdcLoc = pdc.get(GraveHologramKeys.GRAVE_LOCATION, PersistentDataType.STRING);
                            if (pdcLoc != null) storedLocKey = pdcLoc;
                        }
                    } catch (Throwable ignored) {
                    }

                    if (storedLocKey != null && !locKeysMatch(expectedLocKey, storedLocKey)) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram for grave " + graveUuid
                                + " (location mismatch stored=" + storedLocKey + " db=" + expectedLocKey + ")", 2);
                    }
                });
            }
        }

        plugin.debugMessage("[Cleanup] Finished ArmorStand hologram purge", 1);
    }

    public Grave hasGrave(UUID graveUUID) {
        if (graveUUID == null) {
            return null;
        }

        return plugin.getCacheManager().getGraveMap().get(graveUUID);
    }

    private UUID extractGraveUUIDFromStand(ArmorStand stand) {
        if (stand == null) {
            return null;
        }

        try {
            for (String tag : stand.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveUUID:")) {
                    String raw = tag.substring("graveHologramGraveUUID:".length()).trim();
                    try {
                        return UUID.fromString(raw);
                    } catch (IllegalArgumentException ignored) {
                        plugin.debugMessage("[Cleanup] Invalid grave UUID tag on ArmorStand " + stand.getUniqueId() + ": " + raw, 2);
                        return null;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private String extractGraveLocationKeyFromStand(ArmorStand stand) {
        if (stand == null) {
            return null;
        }

        try {
            for (String tag : stand.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveLocation:")) {
                    String raw = tag.substring("graveHologramGraveLocation:".length()).trim();
                    return normalizeLocKey(raw);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private String toLocKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private String normalizeLocKey(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        Pattern pSimple = Pattern.compile("^([^:,]+)[,:]\\s*(-?\\d+)[,:]\\s*(-?\\d+)[,:]\\s*(-?\\d+)$");
        Matcher m = pSimple.matcher(raw);
        if (m.find()) {
            String world = m.group(1);
            try {
                int x = Integer.parseInt(m.group(2));
                int y = Integer.parseInt(m.group(3));
                int z = Integer.parseInt(m.group(4));
                return world + ":" + x + ":" + y + ":" + z;
            } catch (NumberFormatException ignored) {
            }
        }

        Pattern pWorld = Pattern.compile("name=([^},\\s]+)");
        Pattern pX = Pattern.compile("x=([-\\d.]+)");
        Pattern pY = Pattern.compile("y=([-\\d.]+)");
        Pattern pZ = Pattern.compile("z=([-\\d.]+)");

        String world = findFirstGroup(pWorld, raw);
        String sx = findFirstGroup(pX, raw);
        String sy = findFirstGroup(pY, raw);
        String sz = findFirstGroup(pZ, raw);

        if (sx != null && sy != null && sz != null) {
            try {
                int x = (int) Math.floor(Double.parseDouble(sx));
                int y = (int) Math.floor(Double.parseDouble(sy));
                int z = (int) Math.floor(Double.parseDouble(sz));
                if (world == null || world.isEmpty()) world = "unknown";
                return world + ":" + x + ":" + y + ":" + z;
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    private String findFirstGroup(Pattern p, String s) {
        if (p == null || s == null) {
            return null;
        }

        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private boolean locKeysMatch(String expectedKey, String tagKey) {
        if (expectedKey == null || tagKey == null) return false;

        String a = normalizeLocKey(expectedKey);
        String b = normalizeLocKey(tagKey);
        if (a == null || b == null) return false;

        String[] as = a.split(":");
        String[] bs = b.split(":");
        if (as.length != 4 || bs.length != 4) return false;

        String aw = as[0];
        String bw = bs[0];
        int ax = Integer.parseInt(as[1]);
        int ay = Integer.parseInt(as[2]);
        int az = Integer.parseInt(as[3]);
        int bx = Integer.parseInt(bs[1]);
        int by = Integer.parseInt(bs[2]);
        int bz = Integer.parseInt(bs[3]);

        boolean worldKnown = !"unknown".equalsIgnoreCase(aw) && !"unknown".equalsIgnoreCase(bw);
        boolean worldOk = !worldKnown || aw.equals(bw);

        return worldOk && ax == bx && ay == by && az == bz;
    }

    private void executeRegion(Location loc, Runnable task) {
        var sched = plugin.getSchedulerManager();
        if (sched != null) {
            sched.execute(loc, task);
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private void executeRegion(Entity entity, Runnable task) {
        var sched = plugin.getSchedulerManager();
        if (sched != null) {
            sched.execute(entity, task);
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
