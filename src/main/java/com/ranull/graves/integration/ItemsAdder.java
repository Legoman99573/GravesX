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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Integration with the ItemsAdder plugin for handling custom furniture and blocks.
 */
public final class ItemsAdder extends EntityDataManager {
    private final Graves plugin;
    private final Plugin itemsAdderPlugin;

    /**
     * Constructs an ItemsAdder instance and saves data related to ItemsAdder.
     *
     * @param plugin           The Graves plugin instance.
     * @param itemsAdderPlugin The ItemsAdder plugin instance.
     */
    public ItemsAdder(Graves plugin, Plugin itemsAdderPlugin) {
        super(plugin);

        this.plugin = plugin;
        this.itemsAdderPlugin = itemsAdderPlugin;

        saveData();
    }

    /**
     * Copies resource files needed for ItemsAdder integration.
     */
    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.itemsadder.write")) {
            ResourceUtil.copyResources("data/plugin/" + itemsAdderPlugin.getName().toLowerCase() + "/data",
                    plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName() + "/data", plugin);
            ResourceUtil.copyResources("data/model/grave.json", plugin.getPluginsFolder() + "/"
                    + itemsAdderPlugin.getName() + "/data/resource_pack/assets/graves/models/graves/grave.json", plugin);
            plugin.debugMessage("Saving " + itemsAdderPlugin.getName() + " data.", 1);
        }
    }

    /**
     * Creates and places custom furniture at a specified location.
     *
     * @param location The location to place the furniture.
     * @param grave    The grave object associated with the furniture.
     */
    public void createFurniture(Location location, Grave grave) {
        location = LocationUtil.roundLocation(location).add(0.5, 0, 0.5);

        location.setYaw(BlockFaceUtil.getBlockFaceYaw(BlockFaceUtil.getYawBlockFace(location.getYaw()).getOppositeFace()));
        location.setPitch(grave.getPitch());

        if (plugin.getConfig("itemsadder.furniture.enabled", grave)
                .getBoolean("itemsadder.furniture.enabled")) {
            String name = plugin.getConfig("itemsadder.furniture.name", grave)
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
     * Removes all custom furniture associated with a specific grave.
     *
     * @param grave The grave object whose furniture is to be removed.
     */
    public void removeFurniture(Grave grave) {
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes custom furniture associated with a specific entity data.
     *
     * @param entityData The entity data for the furniture to be removed.
     */
    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes custom furniture based on a map of entity data and entities.
     *
     * @param entityDataMap A map of entity data and corresponding entities to be removed.
     */
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
     * Creates and places a custom block at a specified location.
     *
     * @param location The location to place the block.
     * @param grave    The grave object associated with the block.
     */
    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfig("itemsadder.block.enabled", grave)
                .getBoolean("itemsadder.block.enabled")) {
            String name = plugin.getConfig("itemsadder.block.name", grave)
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
     * Checks if a custom block exists at a specified location.
     *
     * @param location The location to check.
     * @return True if a custom block exists at the location, false otherwise.
     */
    public boolean isCustomBlock(Location location) {
        return CustomBlock.byAlreadyPlaced(location.getBlock()) != null;
    }

    /**
     * Removes a custom block at a specified location.
     *
     * @param location The location of the block to be removed.
     */
    public void removeBlock(Location location) {
        CustomBlock.remove(location);
    }

    /**
     * Creates a custom furniture instance with a specified name and location.
     *
     * @param name      The name of the custom furniture.
     * @param location  The location where the furniture should be placed.
     * @return The created CustomFurniture instance, or null if creation failed.
     */
    private CustomFurniture createCustomFurniture(String name, Location location) {
        return CustomFurniture.spawn(name, location.getBlock());
    }

    /**
     * Creates a custom block instance with a specified name and location.
     *
     * @param name      The name of the custom block.
     * @param location  The location where the block should be placed.
     * @return The created CustomBlock instance, or null if creation failed.
     */
    private CustomBlock createCustomBlock(String name, Location location) {
        return CustomBlock.place(name, location);
    }
}