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

        plugin.debugMessage("[Holograms] removeResolvedTextDisplays count="
                + hologramDataList.size(), 1);

        Map<EntityData, Entity> entityDataMap =
                getEntityDataMap(new ArrayList<>(hologramDataList));

        List<EntityData> removableEntityData = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {

            EntityData data = entry.getKey();

            if (data == null) {
                continue;
            }

            if (!(data instanceof HologramData hologramData)) {
                continue;
            }

            if (hologramData.getBackend()
                    != HologramData.Backend.TEXT_DISPLAY) {
                continue;
            }

            removableEntityData.add(data);

            Entity finalEntity = entry.getValue();
            Location finalLocation = data.getLocation();
            UUID finalGraveUUID = data.getUUIDGrave();

            Runnable remover = () -> {

                int removedCount = 0;

                try {
                    if (finalEntity instanceof TextDisplay td
                            && td.isValid()) {

                        td.remove();
                        removedCount++;

                        plugin.debugMessage(
                                "[Holograms] Removed direct TextDisplay entity="
                                        + td.getUniqueId(),
                                2
                        );
                    }
                } catch (Throwable t) {
                    plugin.debugMessage(
                            "[Holograms] Failed direct TextDisplay removal entity="
                                    + data.getUUIDEntity()
                                    + ": "
                                    + t.getMessage(),
                            1
                    );
                }

                if (finalLocation == null
                        || finalLocation.getWorld() == null) {
                    return;
                }

                try {
                    Collection<Entity> nearby =
                            finalLocation.getWorld().getNearbyEntities(
                                    finalLocation,
                                    2.0,
                                    2.0,
                                    2.0
                            );

                    for (Entity e : nearby) {

                        if (!(e instanceof TextDisplay td)) {
                            continue;
                        }

                        if (!td.isValid()) {
                            continue;
                        }

                        boolean remove = false;

                        try {
                            if (plugin.getVersionManager().hasPersistentData()) {

                                PersistentDataContainer pdc =
                                        td.getPersistentDataContainer();

                                String storedUuid = pdc.get(
                                        GraveHologramKeys.GRAVE_UUID,
                                        PersistentDataType.STRING
                                );

                                if (storedUuid != null
                                        && storedUuid.equals(
                                        finalGraveUUID.toString())) {
                                    remove = true;
                                }
                            }
                        } catch (Throwable ignored) {
                        }

                        try {
                            if (!remove
                                    && plugin.getVersionManager()
                                    .hasScoreboardTags()) {

                                remove = td.getScoreboardTags().contains(
                                        "graveHologramGraveUUID:"
                                                + finalGraveUUID
                                );
                            }
                        } catch (Throwable ignored) {
                        }

                        if (remove) {
                            td.remove();
                            removedCount++;

                            plugin.debugMessage(
                                    "[Holograms] Removed matched TextDisplay entity="
                                            + td.getUniqueId()
                                            + " grave="
                                            + finalGraveUUID,
                                    2
                            );
                        }
                    }

                    plugin.debugMessage(
                            "[Holograms] TextDisplay removal sweep finished entity="
                                    + data.getUUIDEntity()
                                    + ", removed="
                                    + removedCount,
                            2
                    );

                } catch (Throwable t) {
                    plugin.logStackTrace(t);
                }
            };

            if (finalEntity != null) {
                executeRegion(finalEntity, remover);

            } else if (finalLocation != null
                    && finalLocation.getWorld() != null) {

                executeRegion(finalLocation, remover);

            } else {
                plugin.getServer()
                        .getScheduler()
                        .runTask(plugin, remover);
            }
        }

        if (!removableEntityData.isEmpty()) {
            plugin.getDataManager()
                    .removeEntityData(removableEntityData);
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
        plugin.debugMessage(
                "[Cleanup] Starting TextDisplay hologram purge across all worlds",
                1
        );

        for (World world : plugin.getServer().getWorlds()) {

            plugin.debugMessage(
                    "[Cleanup] Scanning world "
                            + world.getName()
                            + " for lingering TextDisplays",
                    2
            );

            for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {

                executeRegion(td, () -> {

                    if (!td.isValid()) {
                        return;
                    }

                    boolean hasHoloTag = false;

                    try {
                        if (plugin.getVersionManager()
                                .hasScoreboardTags()) {

                            hasHoloTag = td.getScoreboardTags()
                                    .contains("graveHologram");
                        }
                    } catch (Throwable ignored) {
                    }

                    boolean hasPdc = false;

                    try {
                        if (plugin.getVersionManager()
                                .hasPersistentData()) {

                            PersistentDataContainer pdc =
                                    td.getPersistentDataContainer();

                            hasPdc = pdc.has(
                                    GraveHologramKeys.GRAVE_UUID,
                                    PersistentDataType.STRING
                            );
                        }
                    } catch (Throwable ignored) {
                    }

                    if (!hasHoloTag && !hasPdc) {
                        return;
                    }

                    UUID graveUUID = null;

                    try {
                        if (plugin.getVersionManager()
                                .hasPersistentData()) {

                            PersistentDataContainer pdc =
                                    td.getPersistentDataContainer();

                            String raw = pdc.get(
                                    GraveHologramKeys.GRAVE_UUID,
                                    PersistentDataType.STRING
                            );

                            if (raw != null) {
                                graveUUID = UUID.fromString(raw);
                            }
                        }
                    } catch (Throwable ignored) {
                    }

                    if (graveUUID == null) {
                        try {
                            graveUUID = extractGraveUUIDFromTags(td);
                        } catch (Throwable ignored) {
                        }
                    }

                    if (graveUUID == null) {

                        td.remove();

                        plugin.debugMessage(
                                "[Cleanup] Removed TextDisplay missing grave UUID entity="
                                        + td.getUniqueId(),
                                2
                        );

                        return;
                    }

                    Grave grave = null;

                    try {
                        grave = hasGrave(graveUUID);
                    } catch (Throwable ignored) {
                    }

                    if (grave == null) {

                        td.remove();

                        plugin.debugMessage(
                                "[Cleanup] Removed TextDisplay for missing grave "
                                        + graveUUID,
                                2
                        );

                        return;
                    }

                    boolean hologramExists = isHologramExists(graveUUID);

                    if (!hologramExists) {

                        td.remove();

                        plugin.debugMessage(
                                "[Cleanup] Removed orphaned TextDisplay entity="
                                        + td.getUniqueId()
                                        + " grave="
                                        + graveUUID,
                                2
                        );
                    }
                });
            }
        }

        plugin.debugMessage(
                "[Cleanup] Finished TextDisplay hologram purge",
                1
        );
    }

    private boolean isHologramExists(UUID graveUUID) {
        boolean hologramExists = false;

        try {
            for (EntityData entityData :
                    plugin.getCacheManager()
                            .getEntityMap()
                            .values()) {

                if (!(entityData instanceof HologramData hologramData)) {
                    continue;
                }

                if (hologramData.getBackend()
                        != HologramData.Backend.TEXT_DISPLAY) {
                    continue;
                }

                if (!graveUUID.equals(
                        hologramData.getUUIDGrave())) {
                    continue;
                }

                hologramExists = true;
                break;
            }
        } catch (Throwable ignored) {
        }
        return hologramExists;
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

    private String toLocKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
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