package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.integration.MiniMessage;
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
     * Spawns each ArmorStand on the region thread for that line location.
     */
    public void createHologram(Location location, Grave grave) {
        if (!plugin.getVersionManager().is_v1_7()
                && plugin.getConfig("hologram.enabled", grave).getBoolean("hologram.enabled")) {

            double offsetX = plugin.getConfig("hologram.offset.x", grave).getDouble("hologram.offset.x");
            double offsetY = plugin.getConfig("hologram.offset.y", grave).getDouble("hologram.offset.y");
            double offsetZ = plugin.getConfig("hologram.offset.z", grave).getDouble("hologram.offset.z");
            boolean marker = plugin.getConfig("hologram.marker", grave).getBoolean("hologram.marker");

            Location base = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);

            List<String> lineList = new ArrayList<>(plugin.getConfig("hologram.line", grave).getStringList("hologram.line"));
            double lineHeight = plugin.getConfig("hologram.height-line", grave).getDouble("hologram.height-line");
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

                        pdc.set(
                                GraveHologramKeys.GRAVE_UUID,
                                PersistentDataType.STRING,
                                grave.getUUID().toString()
                        );


                        pdc.set(
                                GraveHologramKeys.GRAVE_LOCATION,
                                PersistentDataType.STRING,
                                locKey
                        );
                    }

                    if (plugin.getIntegrationManager().hasMiniMessage()) {
                        String newLine = StringUtil.parseString(line, lineLoc, grave, plugin);
                        armorStand.setCustomName(MiniMessage.parseString(newLine));
                    } else {
                        armorStand.setCustomName(StringUtil.parseString(line, lineLoc, grave, plugin));
                    }

                    if (!plugin.getVersionManager().is_v1_7()) {
                        try { armorStand.setMarker(marker); } catch (NoSuchMethodError ignored) {}
                    }
                    if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                        if (!plugin.getConfig("drop.spear-attack", grave).getBoolean("drop.spear-attack", false)) {
                            armorStand.setInvulnerable(true);
                        }
                    }

                    if (plugin.getVersionManager().hasScoreboardTags()) {
                        armorStand.getScoreboardTags().add("graveHologram");
                        armorStand.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());
                        armorStand.getScoreboardTags().add("graveHologramGraveLocation:" + locKey);
                    }

                    HologramData hologramData = new HologramData(lineLoc, armorStand.getUniqueId(),
                            grave.getUUID(), finalLineNumber);
                    plugin.getDataManager().addHologramData(hologramData);

                    if (plugin.getIntegrationManager().hasMultiPaper()) {
                        plugin.getIntegrationManager().getMultiPaper().notifyHologramCreation(hologramData);
                    }
                });

                lineNumber++;
            }
        }
    }

    /**
     * Removes all holograms associated with a grave.
     */
    public void removeHologram(Grave grave) {
        removeHologram(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes a specific hologram associated with an entity data.
     */
    public void removeHologram(EntityData entityData) {
        plugin.debugMessage("[Holograms] removeHologram(entityData=" + entityData.getUUIDEntity() + ")", 1);
        removeHologram(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes multiple holograms associated with a map of entity data to entities.
     * Entity removals are executed on the entity's region thread.
     */
    public void removeHologram(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            Entity entity = entry.getValue();
            EntityData data = entry.getKey();
            entityDataList.add(data);

            if (entity != null && entity.isValid()) {
                entity.remove();
            }

            Location graveLocation = data.getLocation();
            if (graveLocation == null || graveLocation.getWorld() == null) continue;

            String locTag = "graveHologramGraveLocation:" + toLocKey(graveLocation);
            String locKey = toLocKey(graveLocation);

            executeRegion(graveLocation, () -> {
                try {
                    for (Entity e : graveLocation.getWorld().getEntities()) {
                        if (!(e instanceof ArmorStand)) continue;
                        if (!e.isValid()) continue;

                        if (!e.getLocation().equals(graveLocation)) continue;

                        boolean remove = false;

                        try {
                            Set<String> tags = e.getScoreboardTags();
                            if (tags.contains(locTag)) {
                                remove = true;
                            }
                        } catch (NoSuchMethodError ignored) {}

                        try {
                            if (plugin.getVersionManager().hasPersistentData()) {
                                PersistentDataContainer pdc = e.getPersistentDataContainer();

                                String storedLoc = pdc.get(
                                        GraveHologramKeys.GRAVE_LOCATION,
                                        PersistentDataType.STRING
                                );

                                if (storedLoc != null && storedLoc.equals(locKey)) {
                                    remove = true;
                                }
                            }
                        } catch (Throwable ignored) {}

                        if (remove) {
                            e.remove();
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().severe(
                            "Failed removing holograms at world: " + graveLocation.getWorld().getName() +
                                    ", x: " + graveLocation.getBlockX() +
                                    ", y: " + graveLocation.getBlockY() +
                                    ", z: " + graveLocation.getBlockZ() + "."
                    );
                    plugin.logStackTrace(t);
                }
            });
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * Scans all worlds for hologram ArmorStands and schedules a region-thread check/removal
     * for each matching stand. The outer iteration is lightweight; all state reads/writes are
     * performed inside the entity's region execution.
     */
    public void purgeLingeringHolograms() {
        if (!plugin.getVersionManager().hasScoreboardTags()) return;

        for (World world : plugin.getServer().getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {

                executeRegion(stand, () -> {

                    boolean hasHoloTag = false;

                    try {
                        hasHoloTag = stand.getScoreboardTags().contains("graveHologram");
                    } catch (Throwable ignored) {
                    }

                    boolean hasPdc = false;
                    try {
                        PersistentDataContainer pdc = stand.getPersistentDataContainer();
                        if (pdc.has(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING)) {
                            hasPdc = true;
                        }
                    } catch (Throwable ignored) {
                        // PDC not supported on this version
                    }

                    if (!hasHoloTag && !hasPdc) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram with no tags or PDC: " + stand.getUniqueId(), 2);
                        return;
                    }

                    UUID tagUuid = null;

                    try {
                        tagUuid = extractGraveUUIDFromStand(stand);
                    } catch (Throwable ignored) {}

                    try {
                        PersistentDataContainer pdc = stand.getPersistentDataContainer();
                        String pdcUuid = pdc.get(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING);
                        if (pdcUuid != null) {
                            try {
                                tagUuid = UUID.fromString(pdcUuid);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } catch (Throwable ignored) {}

                    if (tagUuid == null) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram missing grave UUID tag/PDC entity=" + stand.getUniqueId(), 2);
                        return;
                    }

                    Grave grave;
                    try {
                        grave = hasGrave(tagUuid);
                    } catch (Throwable ignored) {
                        grave = null;
                    }

                    if (grave == null) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram for missing grave " + tagUuid, 2);
                        return;
                    }

                    Location dbLoc = grave.getLocationDeath();
                    if (dbLoc == null || dbLoc.getWorld() == null) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram for grave " + tagUuid + " (DB location missing)", 2);
                        return;
                    }

                    String expectedLocKey = toLocKey(dbLoc);

                    String storedLocKey = null;

                    try {
                        storedLocKey = extractGraveLocationKeyFromStand(stand);
                    } catch (Throwable ignored) {}

                    try {
                        PersistentDataContainer pdc = stand.getPersistentDataContainer();
                        String pdcLoc = pdc.get(GraveHologramKeys.GRAVE_LOCATION, PersistentDataType.STRING);
                        if (pdcLoc != null) storedLocKey = pdcLoc;
                    } catch (Throwable ignored) {}

                    if (storedLocKey != null && !locKeysMatch(expectedLocKey, storedLocKey)) {
                        stand.remove();
                        plugin.debugMessage("[Cleanup] Removed hologram for grave " + tagUuid
                                + " (location mismatch stored=" + storedLocKey + " db=" + expectedLocKey + ")", 2);
                    }
                });
            }
        }
    }

    public Grave hasGrave(UUID graveUUID) {
        return plugin.getCacheManager().getGraveMap().get(graveUUID);
    }

    private UUID extractGraveUUIDFromStand(ArmorStand stand) {
        try {
            for (String tag : stand.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveUUID:")) {
                    String raw = tag.substring("graveHologramGraveUUID:".length()).trim();
                    try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return null; }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private String extractGraveLocationKeyFromStand(ArmorStand stand) {
        try {
            for (String tag : stand.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveLocation:")) {
                    String raw = tag.substring("graveHologramGraveLocation:".length()).trim();
                    return normalizeLocKey(raw);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private UUID extractGraveUUIDFromTags(Entity entity) {
        if (!plugin.getVersionManager().hasScoreboardTags()) return null;
        try {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveUUID:")) {
                    String raw = tag.substring("graveHologramGraveUUID:".length());
                    try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { /* continue */ }
                    break;
                }
            }
        } catch (Throwable ignored) { /* defensive */ }
        return null;
    }

    private String toLocKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private String extractGraveLocationKeyFromTags(Entity entity) {
        if (!plugin.getVersionManager().hasScoreboardTags()) return null;
        try {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveLocation:")) {
                    String raw = tag.substring("graveHologramGraveLocation:".length()).trim();
                    return normalizeLocKey(raw);
                }
            }
        } catch (Throwable ignored) {}
        return null;
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
            } catch (NumberFormatException ignored) {}
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
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    private String findFirstGroup(Pattern p, String s) {
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

        String aw = as[0], bw = bs[0];
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
        var sched = plugin.getGravesXScheduler();
        if (sched != null) {
            sched.execute(loc, task);
        } else {
            plugin.getGravesXScheduler().runTask(task);
        }
    }

    private void executeRegion(Entity entity, Runnable task) {
        var sched = plugin.getGravesXScheduler();
        if (sched != null) {
            sched.execute(entity, task);
        } else {
            plugin.getGravesXScheduler().runTask(task);
        }
    }
}