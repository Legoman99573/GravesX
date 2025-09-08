package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

                        String locKey = toLocKey(grave.getLocationDeath());
                        armorStand.getScoreboardTags().add("graveHologramGraveLocation:" + locKey);
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
        List<EntityData> list = getLoadedEntityDataList(grave);
        plugin.debugMessage("[Holograms] removeHologram(grave=" + grave.getUUID() + ") loaded entities: " + list.size(), 1);

        Map<EntityData, Entity> map = getEntityDataMap(list);
        if (!plugin.getVersionManager().hasScoreboardTags()) {
            removeHologram(map);
            return;
        }

        String expectedKey = toLocKey(grave.getLocationDeath());

        Map<EntityData, Entity> toRemove = new LinkedHashMap<>();
        Map<EntityData, Entity> skipped  = new LinkedHashMap<>();

        for (Map.Entry<EntityData, Entity> e : map.entrySet()) {
            Entity entity = e.getValue();

            UUID tagUuid = extractGraveUUIDFromTags(entity);
            String tagLocKey = extractGraveLocationKeyFromTags(entity);

            boolean uuidMatches = grave.getUUID().equals(tagUuid);
            boolean locMatches  = locKeysMatch(expectedKey, tagLocKey);

            if (uuidMatches || locMatches) {
                toRemove.put(e.getKey(), e.getValue());
            } else {
                skipped.put(e.getKey(), e.getValue());
            }

            plugin.debugMessage(
                    "[Holograms] check entity=" + entity.getUniqueId()
                            + " tagUUID=" + tagUuid
                            + " tagLocKey=" + (tagLocKey == null ? "null" : tagLocKey)
                            + " expectedUUID=" + grave.getUUID()
                            + " expectedLocKey=" + expectedKey
                            + " -> " + (uuidMatches || locMatches ? "REMOVE" : "SKIP"),
                    3
            );
        }

        if (!skipped.isEmpty()) {
            plugin.debugMessage("[Holograms] Skipped " + skipped.size() + " non-matching hologram entities for grave " + grave.getUUID(), 2);
        }
        removeHologram(toRemove);
    }

    /**
     * Removes a specific hologram associated with an entity data.
     *
     * @param entityData The entity data of the hologram to remove.
     */
    public void removeHologram(EntityData entityData) {
        plugin.debugMessage("[Holograms] removeHologram(entityData=" + entityData.getUUIDEntity() + ")", 1);
        removeHologram(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes multiple holograms associated with a map of entity data to entities.
     *
     * @param entityDataMap The map of entity data to entities.
     */
    private void removeHologram(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();
        Set<UUID> affectedGraves = new LinkedHashSet<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            Entity entity = entry.getValue();

            UUID graveUUIDFromTag = extractGraveUUIDFromTags(entity);
            if (graveUUIDFromTag != null) {
                affectedGraves.add(graveUUIDFromTag);
            }

            String locKey = extractGraveLocationKeyFromTags(entity);
            if (locKey != null) {
                plugin.debugMessage("[Holograms] Removing hologram entity=" + entity.getUniqueId()
                        + " tagGraveUUID=" + graveUUIDFromTag
                        + " tagDeathLocKey=" + locKey, 2);
            }

            entity.remove();
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);

        if (!affectedGraves.isEmpty()) {
            plugin.debugMessage("Removed hologram entities for graves: " + affectedGraves, 2);
        }
    }

    /**
     * Scans all worlds for hologram ArmorStands (tag "graveHologram") and removes
     * any lingering ones:
     * - Missing/invalid graveHologramGraveUUID:uuid tag
     * - grave UUID not found in DB
     * - (optional) Tag location present but mismatches DB grave death-location
     * <p>
     * Must be called on the main thread / correct region thread by the caller.
     */
    public void purgeLingeringHolograms() {
        if (!plugin.getVersionManager().hasScoreboardTags()) return;

        for (World world : plugin.getServer().getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                try {
                    if (!stand.getScoreboardTags().contains("graveHologram")) continue;
                } catch (Throwable ignored) {
                    continue;
                }

                UUID tagUuid = extractGraveUUIDFromStand(stand);

                if (tagUuid == null) {
                    try {
                        stand.remove();
                    } catch (Throwable ignored) {
                    }
                    plugin.debugMessage("[Cleanup] Removed hologram (missing/invalid grave UUID tag) entity=" + stand.getUniqueId(), 2);
                    continue;
                }

                Grave grave;
                try {
                    grave = hasGrave(tagUuid);
                } catch (Throwable ignored) {
                    grave = null;
                }

                if (grave == null) {
                    try {
                        stand.remove();
                    } catch (Throwable ignored) {
                    }
                    plugin.debugMessage("[Cleanup] Removed hologram for missing grave " + tagUuid, 2);
                    continue;
                }

                Location dbLoc = grave.getLocationDeath();
                if (dbLoc == null || dbLoc.getWorld() == null) {
                    try {
                        stand.remove();
                    } catch (Throwable ignored) {
                    }
                    plugin.debugMessage("[Cleanup] Removed hologram for grave " + tagUuid + " (DB location missing)", 2);
                    continue;
                }

                String tagLocKey = extractGraveLocationKeyFromStand(stand);
                if (tagLocKey != null) {
                    String expectedKey = toLocKey(dbLoc);
                    if (!locKeysMatch(expectedKey, tagLocKey)) {
                        try {
                            stand.remove();
                        } catch (Throwable ignored) {
                        }
                        plugin.debugMessage("[Cleanup] Removed hologram for grave " + tagUuid
                                + " (location mismatch tag=" + tagLocKey + " db=" + expectedKey + ")", 2);
                    }
                }
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

    /**
     * Attempts to extract the grave UUID from an entity's scoreboard tags.
     * Looks for: "graveHologramGraveUUID:graveUUID".
     *
     * @param entity The entity to inspect.
     * @return The parsed UUID if present and valid; otherwise null.
     */
    private UUID extractGraveUUIDFromTags(Entity entity) {
        if (!plugin.getVersionManager().hasScoreboardTags()) {
            return null;
        }

        try {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveUUID:")) {
                    String raw = tag.substring("graveHologramGraveUUID:".length());
                    try {
                        return UUID.fromString(raw);
                    } catch (IllegalArgumentException ignored) {
                    }
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Build a normalized "world:x:y:z" key from a Bukkit Location using block coordinates.
     */
    private String toLocKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Try to extract and normalize the grave death-location key from scoreboard tags.
     * Accepts multiple legacy/raw formats and normalizes to "world:x:y:z".
     *
     * @param entity the entity whose tags should be scanned
     * @return normalized "world:x:y:z" or null if not present/parsable
     */
    private String extractGraveLocationKeyFromTags(Entity entity) {
        if (!plugin.getVersionManager().hasScoreboardTags()) return null;

        try {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveLocation:")) {
                    String raw = tag.substring("graveHologramGraveLocation:".length()).trim();
                    return normalizeLocKey(raw);
                }
            }
        } catch (Throwable ignored) {
            // Defensive for odd platforms/versions
        }
        return null;
    }

    /**
     * Normalize various possible raw location representations into "world:x:y:z".
     * Supports:
     *  - "world:x:y:z" (preferred)
     *  - "world,x,y,z"
     *  - Bukkit-like strings containing "name=world" and "x, y, z"
     */
    private String normalizeLocKey(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        // world:x:y:z or world,x,y,z
        // Group 1 = world, 2 = x, 3 = y, 4 = z
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

    /**
     * Compare two normalized or raw keys and decide if they represent the same block position.
     * If either side is raw, it will be normalized first. If a world is "unknown" on either side,
     * the comparison falls back to only x/y/z.
     */
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
}