package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.ResourceUtil;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Use GravesXModule: ItemsAdder instead
 *
 * Integration with the ItemsAdder plugin for handling custom furniture and blocks.
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
public class ItemsAdder extends EntityDataManager {
    private final Graves plugin;
    private final Plugin itemsAdderPlugin;

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Constructs an ItemsAdder instance and saves data related to ItemsAdder.
     *
     * @param plugin           The Graves plugin instance.
     * @param itemsAdderPlugin The ItemsAdder plugin instance.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public ItemsAdder(Graves plugin, Plugin itemsAdderPlugin) {
        super(plugin);
        this.plugin = plugin;
        this.itemsAdderPlugin = itemsAdderPlugin;
        saveData();
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     *
     * Copies resource files needed for ItemsAdder integration.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void saveData() {
        String version = itemsAdderPlugin.getDescription().getVersion();
        String targetVersion = "3.3.0";
        if (compareVersions(version, targetVersion) < 0) {
            if (plugin.getConfig().getBoolean("settings.integration.itemsadder.write")) {
                ResourceUtil.copyResources(
                        "data/plugin/" + itemsAdderPlugin.getName().toLowerCase() + "/data",
                        plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName() + "/data",
                        plugin
                );
                ResourceUtil.copyResources(
                        "data/model/grave.json",
                        plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName()
                                + "/data/resource_pack/assets/graves/models/graves/grave.json",
                        plugin
                );
                plugin.debugMessage("Saving " + itemsAdderPlugin.getName() + " data.", 1);
            }
        } else {
            deleteOldItemsAdderData(plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName() + "/data/items_packs/graves/data");
            deleteOldItemsAdderData(plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName() + "/data/resource_pack/assets/graves/grave.json");

            if (plugin.getConfig().getBoolean("settings.integration.itemsadder.write")) {
                ResourceUtil.copyResources(
                        "data/plugin/" + itemsAdderPlugin.getName().toLowerCase() + "/data",
                        plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName() + "/contents/graves/configs",
                        plugin
                );
                ResourceUtil.copyResources(
                        "data/model/grave.json",
                        plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName()
                                + "/contents/graves/resourcepack/graves/models/graves/grave.json",
                        plugin
                );
                ResourceUtil.copyResources(
                        "data/textures/block/nether_wart_block.png",
                        plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName()
                                + "/contents/graves/resourcepack/graves/textures/block/nether_wart_block.png",
                        plugin
                );
                ResourceUtil.copyResources(
                        "data/textures/block/mossy_stone_bricks.png",
                        plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName()
                                + "/contents/graves/resourcepack/graves/textures/block/mossy_stone_bricks.png",
                        plugin
                );
                plugin.debugMessage("Saving " + itemsAdderPlugin.getName() + " data.", 1);
            }
        }
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     *
     * Deletes older ItemsAdder Data to give room for new data for newer versions without affecting other files.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    private void deleteOldItemsAdderData(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    if (subFiles != null) {
                        for (File subFile : subFiles) {
                            deleteOldItemsAdderData(subFile.getPath());
                        }
                    }
                }
                file.delete();
                plugin.getLogger().info("Deleted old ItemsAdder data at: " + path);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Failed to delete old ItemsAdder data at: " + path + ". You will need to delete these manually.");
            }
        }
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     *
     * Compares versions for ItemsAdder integration.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    private int compareVersions(String v1, String v2) {
        String[] mainVersion1 = v1.split("-", 2);
        String[] mainVersion2 = v2.split("-", 2);

        String[] parts1 = mainVersion1[0].split("\\.");
        String[] parts2 = mainVersion2[0].split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (part1 < part2) return -1;
            if (part1 > part2) return 1;
        }
        return 0;
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Creates and places custom furniture at a specified location.
     *
     * @param location The location to place the furniture.
     * @param grave    The grave object associated with the furniture.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void createFurniture(Location location, Grave grave) {
        location = LocationUtil.roundLocation(location).add(0.5, 0, 0.5);
        location.setYaw(BlockFaceUtil.getBlockFaceYaw(BlockFaceUtil.getYawBlockFace(location.getYaw()).getOppositeFace()));
        location.setPitch(grave.getPitch());

        if (plugin.getConfigManager().getConfigSection("itemsadder.furniture.enabled", grave).getBoolean("itemsadder.furniture.enabled")) {
            String name = plugin.getConfigManager().getConfigSection("itemsadder.furniture.name", grave)
                    .getString("itemsadder.furniture.name", "");
            location.getBlock().setType(Material.AIR);
            CustomFurniture customFurniture = createCustomFurniture(name, location);

            if (customFurniture != null && customFurniture.getEntity() != null) {
                customFurniture.teleport(location);
                createEntityData(customFurniture.getEntity(), grave, EntityData.Type.ITEMSADDER);
                plugin.debugMessage("Placing ItemsAdder furniture for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
            } else {
                plugin.debugMessage("Can't find ItemsAdder furniture " + name, 1);
            }
        }
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Removes all custom furniture associated with a specific grave.
     *
     * @param grave The grave object whose furniture is to be removed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void removeFurniture(Grave grave) {
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Removes custom furniture associated with a specific entity data.
     *
     * @param entityData The entity data for the furniture to be removed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(List.of(entityData)));
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Removes custom furniture based on a map of entity data and entities.
     *
     * @param entityDataMap A map of entity data and corresponding entities to be removed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();
        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            CustomFurniture.remove(entry.getValue(), false);
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }
        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Creates and places a custom block at a specified location.
     *
     * @param location The location to place the block.
     * @param grave    The grave object associated with the block.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfigManager().getConfigSection("itemsadder.block.enabled", grave).getBoolean("itemsadder.block.enabled")) {
            String name = plugin.getConfigManager().getConfigSection("itemsadder.block.name", grave)
                    .getString("itemsadder.block.name", "");
            CustomBlock customBlock = createCustomBlock(name, location);

            if (customBlock != null) {
                plugin.debugMessage("Placing ItemsAdder block for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
            } else {
                plugin.debugMessage("Can't find ItemsAdder block " + name, 1);
            }
        }
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Checks if a custom block exists at a specified location.
     *
     * @param location The location to check.
     * @return True if a custom block exists at the location, false otherwise.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public boolean isCustomBlock(Location location) {
        return CustomBlock.byAlreadyPlaced(location.getBlock()) != null;
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Removes a custom block at a specified location.
     *
     * @param location The location of the block to be removed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public void removeBlock(Location location) {
        CustomBlock.remove(location);
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * True if an ItemsAdder furniture entity for this grave is currently spawned.
     *
     * @param grave The grave to check.
     * @return True if at least one valid IA furniture entity mapped to this grave exists.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public boolean hasFurniture(Grave grave) {
        if (grave == null) return false;

        Map<EntityData, Entity> map = getEntityDataMap(getLoadedEntityDataList(grave));
        if (map.isEmpty()) return false;

        for (Entity e : map.values()) {
            if (e == null) continue;
            if (!e.isValid() || e.isDead()) continue;
            try {
                if (CustomFurniture.byAlreadySpawned(e) != null) {
                    return true;
                }
            } catch (Throwable ignored) {
                return true;
            }
        }
        return false;
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * True if an ItemsAdder custom block exists at the grave location.
     *
     * @param grave The grave to check.
     * @return True if a custom block is present where the grave is placed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    public boolean hasBlock(Grave grave) {
        if (grave == null) return false;

        Location loc = grave.getLocationDeath();
        if (loc == null || loc.getWorld() == null) {
            loc = grave.getLocationDeath();
        }
        if (loc == null || loc.getWorld() == null) return false;

        return isCustomBlock(loc);
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Creates a custom furniture instance with a specified name and location.
     *
     * @param name      The name of the custom furniture.
     * @param location  The location where the furniture should be placed.
     * @return The created CustomFurniture instance, or null if creation failed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    private CustomFurniture createCustomFurniture(String name, Location location) {
        return CustomFurniture.spawn(name, location.getBlock());
    }

    /**
     * @deprecated Use GravesXModule: ItemsAdder instead
     * Creates a custom block instance with a specified name and location.
     *
     * @param name      The name of the custom block.
     * @param location  The location where the block should be placed.
     * @return The created CustomBlock instance, or null if creation failed.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    private CustomBlock createCustomBlock(String name, Location location) {
        return CustomBlock.place(name, location);
    }
}