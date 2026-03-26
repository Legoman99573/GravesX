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
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextDisplay-based holograms (1.19+).
 */
public class TextDisplayManager extends EntityDataManager {
    private final Graves plugin;

    public TextDisplayManager(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

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

        Location base = LocationUtil.roundLocation(location)
                .add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);

        List<String> lines = new ArrayList<>(
                plugin.getConfigManager().getConfigSection("hologram.line", grave).getStringList("hologram.line")
        );
        if (lines.isEmpty()) return;

        double lineHeight = plugin.getConfigManager()
                .getConfigSection("hologram.height-line", grave)
                .getDouble("hologram.height-line", 0.28D);

        int lineCount = lines.size();

        double centerShiftY = ((lineCount + 1) / 2.0D) * lineHeight;
        Location spawnLoc = base.clone().add(0.0D, centerShiftY, 0.0D);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            String raw = lines.get(i);

            int multiplier = (lineCount - i); // top line -> N, bottom line -> 1
            Location lineLocForParse = base.clone().add(0.0D, multiplier * lineHeight, 0.0D);

            String parsed = StringUtil.parseString(raw, lineLocForParse, grave, plugin);

            if (i > 0) sb.append('\n');
            sb.append(parsed);
        }
        String finalText = sb.toString();

        executeRegion(spawnLoc, () -> {
            TextDisplay display = spawnTextDisplay(spawnLoc);
            if (display == null) return;

            String locKey = toLocKey(grave.getLocationDeath());

            if (plugin.getVersionManager().hasPersistentData()) {
                try {
                    PersistentDataContainer pdc = display.getPersistentDataContainer();
                    pdc.set(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING, grave.getUUID().toString());
                    pdc.set(GraveHologramKeys.GRAVE_LOCATION, PersistentDataType.STRING, locKey);
                } catch (Throwable ignored) {}
            }

            if (plugin.getVersionManager().hasScoreboardTags()) {
                try {
                    display.getScoreboardTags().add("graveHologram");
                    display.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());
                    display.getScoreboardTags().add("graveHologramGraveLocation:" + locKey);
                } catch (Throwable ignored) {}
            }

            applyDisplayOptions(display, grave);

            if (plugin.getIntegrationManager().hasMiniMessage()) {
                display.setText(MiniMessage.parseString(finalText));
            } else {
                display.setText(finalText);
            }

            HologramData hologramData = new HologramData(spawnLoc, display.getUniqueId(), grave.getUUID(), 0, HologramData.Backend.TEXT_DISPLAY);
            plugin.getDataManager().addHologramData(hologramData);

            if (plugin.getIntegrationManager().hasMultiPaper()) {
                plugin.getIntegrationManager().getMultiPaper().notifyHologramCreation(hologramData);
            }
        });
    }

    public void removeHologram(Grave grave) {
        if (grave == null) {
            plugin.debugMessage("[Holograms] removeTextDisplayHologram(grave) skipped: grave is null", 1);
            return;
        }

        if (grave.getUUID() == null) {
            plugin.debugMessage("[Holograms] removeTextDisplayHologram(grave) skipped: grave UUID is null", 1);
            return;
        }

        plugin.debugMessage("[Holograms] removeTextDisplayHologram(grave=" + grave.getUUID()
                + ") resolving cached TextDisplay holograms", 1);

        List<HologramData> hologramDataList = getCachedHologramData(grave, HologramData.Backend.TEXT_DISPLAY);

        plugin.debugMessage("[Holograms] removeTextDisplayHologram(grave=" + grave.getUUID() + ") resolved "
                + hologramDataList.size() + " cached TextDisplay hologram(s)", 1);

        if (hologramDataList.isEmpty()) {
            plugin.debugMessage("[Holograms] No cached TextDisplay holograms found for grave=" + grave.getUUID(), 2);
            return;
        }

        removeResolvedHolograms(hologramDataList);
    }

    /**
     * @deprecated Use {@link #removeHologram(Grave)} instead.
     */
    @Deprecated(forRemoval = true)
    public void removeHologram(EntityData entityData) {
        String message = "TextDisplayManager#removeHologram(EntityData) is no longer supported. "
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
        String message = "TextDisplayManager#removeHologram(Map<EntityData, Entity>) is no longer supported. "
                + "Use removeHologram(Grave) instead.";

        plugin.getLogger().severe(message + " size=" + (entityDataMap != null ? entityDataMap.size() : 0));
        plugin.debugMessage("[Holograms] " + message, 1);

        throw new UnsupportedOperationException(message);
    }

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

    private void removeResolvedHolograms(List<? extends EntityData> hologramDataList) {
        if (hologramDataList == null || hologramDataList.isEmpty()) {
            plugin.debugMessage("[Holograms] removeResolvedTextDisplays skipped: no hologram data", 2);
            return;
        }

        plugin.debugMessage("[Holograms] removeResolvedTextDisplays count=" + hologramDataList.size(), 1);

        List<EntityData> removableEntityData = new ArrayList<>();

        for (EntityData data : hologramDataList) {
            if (data == null) {
                plugin.debugMessage("[Holograms] Skipping null EntityData entry during TextDisplay removal", 2);
                continue;
            }

            if (!(data instanceof HologramData hologramData)) {
                plugin.debugMessage("[Holograms] Skipping non-hologram EntityData uuid=" + data.getUUIDEntity(), 2);
                continue;
            }

            if (hologramData.getBackend() != HologramData.Backend.TEXT_DISPLAY) {
                plugin.debugMessage("[Holograms] Skipping non-TextDisplay hologram uuid=" + data.getUUIDEntity()
                        + ", backend=" + hologramData.getBackend(), 2);
                continue;
            }

            removableEntityData.add(data);

            UUID entityUuid = data.getUUIDEntity();
            Entity entity = null;

            if (entityUuid != null) {
                try {
                    entity = Bukkit.getEntity(entityUuid);
                } catch (Throwable t) {
                    plugin.debugMessage("[Holograms] Failed Bukkit entity lookup for TextDisplay uuid="
                            + entityUuid + ": " + t.getMessage(), 2);
                }
            }

            Entity finalEntity = entity;
            Location finalLocation = data.getLocation();

            plugin.debugMessage("[Holograms] Scheduling TextDisplay hologram removal for entity="
                    + data.getUUIDEntity() + ", grave=" + data.getUUIDGrave()
                    + ", location=" + (finalLocation != null ? toLocKey(finalLocation) : "null"), 1);

            Runnable remover = () -> {
                boolean removed = false;

                try {
                    Entity liveEntity = finalEntity;

                    if (liveEntity == null && entityUuid != null) {
                        try {
                            liveEntity = Bukkit.getEntity(entityUuid);
                        } catch (Throwable ignored) {
                        }
                    }

                    if (!(liveEntity instanceof TextDisplay td)) {
                        plugin.debugMessage("[Holograms] No live TextDisplay found for entity=" + entityUuid, 2);
                        return;
                    }

                    if (!td.isValid()) {
                        plugin.debugMessage("[Holograms] TextDisplay already invalid for entity=" + entityUuid, 2);
                        return;
                    }

                    boolean matches = false;

                    try {
                        if (plugin.getVersionManager().hasScoreboardTags()
                                && td.getScoreboardTags().contains("graveHologram")) {
                            matches = true;
                        }
                    } catch (Throwable ignored) {
                    }

                    try {
                        if (!matches && plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = td.getPersistentDataContainer();
                            String pdcUuid = pdc.get(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING);
                            if (pdcUuid != null && data.getUUIDGrave() != null
                                    && data.getUUIDGrave().toString().equals(pdcUuid)) {
                                matches = true;
                            }
                        }
                    } catch (Throwable ignored) {
                    }

                    if (!matches) {
                        plugin.debugMessage("[Holograms] Live entity did not match TextDisplay hologram markers for entity="
                                + entityUuid, 2);
                        return;
                    }

                    td.remove();
                    removed = true;
                    plugin.debugMessage("[Holograms] Removed TextDisplay entity=" + td.getUniqueId(), 2);
                } catch (Throwable t) {
                    plugin.debugMessage("[Holograms] Failed TextDisplay removal for entity="
                            + entityUuid + ": " + t.getMessage(), 1);
                }

                if (!removed) {
                    plugin.debugMessage("[Holograms] TextDisplay removal completed with no entity removed for entity="
                            + entityUuid, 2);
                }
            };

            if (finalEntity != null) {
                plugin.debugMessage("[Holograms] Executing TextDisplay removal on entity region for entity="
                        + finalEntity.getUniqueId(), 2);
                executeRegion(finalEntity, remover);
            } else if (finalLocation != null && finalLocation.getWorld() != null) {
                plugin.debugMessage("[Holograms] Executing TextDisplay removal on location region for hologram entity="
                        + data.getUUIDEntity(), 2);
                executeRegion(finalLocation, remover);
            } else {
                plugin.debugMessage("[Holograms] Executing fallback main-thread TextDisplay removal for hologram entity="
                        + data.getUUIDEntity(), 2);
                plugin.getServer().getScheduler().runTask(plugin, remover);
            }
        }

        if (!removableEntityData.isEmpty()) {
            plugin.debugMessage("[Holograms] Removing " + removableEntityData.size() + " TextDisplay hologram entity-data entrie(s) from DataManager records", 1);
            plugin.getDataManager().removeEntityData(removableEntityData);
        } else {
            plugin.debugMessage("[Holograms] No TextDisplay hologram entity-data entries qualified for DB/cache removal", 2);
        }
    }

    public void updateTextDisplay(Entity entity,
                                  List<String> cfgLines,
                                  double lineHeight,
                                  Location base,
                                  Grave grave) {
        if (!(entity instanceof TextDisplay td)) return;

        updateTextDisplay(td, cfgLines, lineHeight, base, grave);
    }

    private void updateTextDisplay(TextDisplay textDisplay, List<String> cfgLines, double lineHeight, Location base, Grave grave) {
        applyDisplayOptions(textDisplay, grave);

        List<String> cfgLinesOriginalOrder = new ArrayList<>(cfgLines);
        int lineCount = cfgLinesOriginalOrder.size();
        double centerShiftY = ((lineCount + 1) / 2.0D) * lineHeight;
        Location expectedLoc = base.clone().add(0.0D, centerShiftY, 0.0D);

        try {
            Location cur = textDisplay.getLocation();
            if (cur.getWorld() != null && expectedLoc.getWorld() != null && cur.getWorld().equals(expectedLoc.getWorld())) {
                double dx = cur.getX() - expectedLoc.getX();
                double dy = cur.getY() - expectedLoc.getY();
                double dz = cur.getZ() - expectedLoc.getZ();
                if ((dx * dx + dy * dy + dz * dz) > 0.0001D) {
                    textDisplay.teleport(expectedLoc);
                }
            }
        } catch (Throwable t) {
            plugin.debugMessage("Failed to get Target Location for grave " + grave.getUUID()
                    + ". Holograms will not update. \n" + Arrays.toString(t.getStackTrace()), 2);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            String raw = cfgLinesOriginalOrder.get(i);

            int multiplier = (lineCount - i);
            Location lineLocForParse = base.clone().add(0.0D, multiplier * lineHeight, 0.0D);

            String parsed = StringUtil.parseString(raw, lineLocForParse, grave, plugin);
            if (i > 0) sb.append('\n');
            sb.append(parsed);
        }

        String fullText = sb.toString();
        if (plugin.getIntegrationManager().hasMiniMessage()) {
            textDisplay.setText(MiniMessage.parseString(fullText));
        } else {
            textDisplay.setText(fullText);
        }
    }

    public void purgeLingeringHolograms() {
        plugin.debugMessage("[Cleanup] Starting TextDisplay hologram purge across all worlds", 1);

        for (World world : plugin.getServer().getWorlds()) {
            plugin.debugMessage("[Cleanup] Scanning world " + world.getName() + " for lingering TextDisplays", 2);

            for (Entity e : world.getEntities()) {
                if (!(e instanceof TextDisplay td)) continue;

                executeRegion(td, () -> {
                    if (!td.isValid()) return;

                    boolean hasHoloTag = false;
                    try {
                        if (plugin.getVersionManager().hasScoreboardTags()) {
                            hasHoloTag = td.getScoreboardTags().contains("graveHologram");
                        }
                    } catch (Throwable ignored) {}

                    boolean hasPdc = false;
                    try {
                        if (plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = td.getPersistentDataContainer();
                            hasPdc = pdc.has(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING);
                        }
                    } catch (Throwable ignored) {}

                    if (!hasHoloTag && !hasPdc) return;

                    UUID graveUuid = null;

                    try { graveUuid = extractGraveUUIDFromTags(td); } catch (Throwable ignored) {}

                    try {
                        if (plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = td.getPersistentDataContainer();
                            String pdcUuid = pdc.get(GraveHologramKeys.GRAVE_UUID, PersistentDataType.STRING);
                            if (pdcUuid != null) {
                                try { graveUuid = UUID.fromString(pdcUuid); } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}

                    if (graveUuid == null) {
                        td.remove();
                        plugin.debugMessage("[Cleanup] Removed TextDisplay missing grave UUID tag/PDC entity=" + td.getUniqueId(), 2);
                        return;
                    }

                    Grave grave = null;
                    try { grave = hasGrave(graveUuid); } catch (Throwable ignored) {}

                    if (grave == null) {
                        td.remove();
                        plugin.debugMessage("[Cleanup] Removed TextDisplay for missing grave " + graveUuid, 2);
                        return;
                    }

                    Location dbLoc = grave.getLocationDeath();
                    if (dbLoc == null || dbLoc.getWorld() == null) {
                        td.remove();
                        plugin.debugMessage("[Cleanup] Removed TextDisplay for grave " + graveUuid + " (DB location missing)", 2);
                        return;
                    }

                    String expectedLocKey = toLocKey(dbLoc);

                    String storedLocKey = null;
                    try { storedLocKey = extractGraveLocationKeyFromTags(td); } catch (Throwable ignored) {}

                    try {
                        if (plugin.getVersionManager().hasPersistentData()) {
                            PersistentDataContainer pdc = td.getPersistentDataContainer();
                            String pdcLoc = pdc.get(GraveHologramKeys.GRAVE_LOCATION, PersistentDataType.STRING);
                            if (pdcLoc != null) storedLocKey = pdcLoc;
                        }
                    } catch (Throwable ignored) {}

                    if (storedLocKey != null && !locKeysMatch(expectedLocKey, storedLocKey)) {
                        td.remove();
                        plugin.debugMessage("[Cleanup] Removed TextDisplay for grave " + graveUuid
                                + " (location mismatch stored=" + storedLocKey + " db=" + expectedLocKey + ")", 2);
                    }
                });
            }
        }

        plugin.debugMessage("[Cleanup] Finished TextDisplay hologram purge", 1);
    }

    public Grave hasGrave(UUID graveUUID) {
        return plugin.getCacheManager().getGraveMap().get(graveUUID);
    }

    private TextDisplay spawnTextDisplay(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        try {
            Entity ent = loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            return (ent instanceof TextDisplay td) ? td : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void applyDisplayOptions(TextDisplay display, Grave grave) {
        if (display == null) return;

        boolean seeThrough = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.see-through", grave)
                .getBoolean("hologram.text-display.see-through", true);
        display.setSeeThrough(seeThrough);

        boolean shadowed = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.shadowed", grave)
                .getBoolean("hologram.text-display.shadowed", false);
        display.setShadowed(shadowed);

        boolean defaultBackground = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.default-background", grave)
                .getBoolean("hologram.text-display.default-background", false);
        display.setDefaultBackground(defaultBackground);

        int textOpacity = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.text-opacity", grave)
                .getInt("hologram.text-display.text-opacity", 255);
        display.setTextOpacity((byte) Math.max(0, Math.min(255, textOpacity)));

        String bg = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.background", grave)
                .getString("hologram.text-display.background", "");
        if (bg != null && !bg.isBlank()) {
            Integer argb = parseHexColorToARGB(bg.trim());
            if (argb != null) {
                display.setBackgroundColor(Color.fromARGB(argb));
            }
        }

        int lineWidth = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.line-width", grave)
                .getInt("hologram.text-display.line-width", 0);
        if (lineWidth > 0) {
            display.setLineWidth(lineWidth);
        }

        String alignRaw = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.alignment", grave)
                .getString("hologram.text-display.alignment", "CENTER");
        display.setAlignment(parseAlignment(alignRaw));

        String billboardRaw = plugin.getConfigManager()
                .getConfigSection("hologram.text-display.billboard", grave)
                .getString("hologram.text-display.billboard", "CENTER");
        display.setBillboard(parseBillboard(billboardRaw));
    }

    private TextDisplay.TextAlignment parseAlignment(String raw) {
        if (raw == null) return TextDisplay.TextAlignment.CENTER;
        try {
            return TextDisplay.TextAlignment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TextDisplay.TextAlignment.CENTER;
        }
    }

    private Display.Billboard parseBillboard(String raw) {
        if (raw == null) return Display.Billboard.CENTER;
        try {
            return Display.Billboard.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Display.Billboard.CENTER;
        }
    }

    private Integer parseHexColorToARGB(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);

        if (s.length() == 6) s = "FF" + s;
        if (s.length() != 8) return null;

        try {
            long v = Long.parseLong(s, 16);
            return (int) (v & 0xFFFFFFFFL);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private UUID extractGraveUUIDFromTags(Entity entity) {
        if (!plugin.getVersionManager().hasScoreboardTags()) return null;
        try {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("graveHologramGraveUUID:")) {
                    String raw = tag.substring("graveHologramGraveUUID:".length()).trim();
                    try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return null; }
                }
            }
        } catch (Throwable ignored) {}
        return null;
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