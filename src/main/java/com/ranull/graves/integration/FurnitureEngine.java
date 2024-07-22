package com.ranull.graves.integration;

import com.mira.furnitureengine.furniture.FurnitureManager;
import com.mira.furnitureengine.furniture.core.Furniture;
import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.furnitureengine.FurnitureBreakListener;
import com.ranull.graves.listener.integration.furnitureengine.FurnitureInteractListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages FurnitureEngine integration for creating, removing, and interacting with furniture.
 */
public final class FurnitureEngine extends EntityDataManager {

    private final Graves plugin;
    private final FurnitureInteractListener furnitureInteractListener;
    private final FurnitureBreakListener furnitureBreakListener;

    /**
     * Constructs a FurnitureEngine instance.
     *
     * @param plugin The main plugin instance.
     */
    public FurnitureEngine(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
        this.furnitureInteractListener = new FurnitureInteractListener(plugin, this);
        this.furnitureBreakListener = new FurnitureBreakListener(this);
        registerListeners();
    }

    /**
     * Registers FurnitureEngine event listeners.
     */
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(furnitureInteractListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(furnitureBreakListener, plugin);
    }

    /**
     * Unregisters FurnitureEngine event listeners.
     */
    public void unregisterListeners() {
        if (furnitureInteractListener != null) {
            HandlerList.unregisterAll(furnitureInteractListener);
        }
        if (furnitureBreakListener != null) {
            HandlerList.unregisterAll(furnitureBreakListener);
        }
    }

    /**
     * Creates and places furniture at the specified location.
     *
     * @param location The location where the furniture should be placed.
     * @param grave    The grave associated with the furniture.
     */
    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("furnitureengine.enabled", grave).getBoolean("furnitureengine.enabled")) {
            String name = plugin.getConfig("furnitureengine.name", grave).getString("furnitureengine.name", "");
            location.getBlock().setType(Material.AIR);
            if (placeFurniture(name, location, BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil.getYawBlockFace(location.getYaw())))) {
                ItemFrame itemFrame = getItemFrame(location);
                if (itemFrame != null && location.getWorld() != null) {
                    createEntityData(location, itemFrame.getUniqueId(), grave.getUUID(), EntityData.Type.FURNITUREENGINE);
                    plugin.debugMessage("Placing FurnitureEngine furniture for " + grave.getUUID() + " at "
                            + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                            + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
                }
            }
        }
    }

    /**
     * Removes furniture associated with the specified grave.
     *
     * @param grave The grave for which to remove furniture.
     */
    public void removeFurniture(Grave grave) {
        cleanupItemFrame(grave);
        try {
            breakFurniture(grave.getLocationDeath());
        } catch (NullPointerException ignored) {
        }
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes specific furniture entity data.
     *
     * @param entityData The entity data of the furniture to remove.
     */
    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes multiple pieces of furniture based on a map of entity data to entities.
     *
     * @param entityDataMap A map of entity data to entities to remove.
     */
    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();
        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            breakFurniture(entry.getValue().getLocation());
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }
        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * Retrieves the ItemFrame at a specified location.
     *
     * @param location The location to search for the ItemFrame.
     * @return The found ItemFrame, or null if none found.
     */
    public ItemFrame getItemFrame(Location location) {
        location = location.clone().add(0.0D, 1.0D, 0.0D);
        if (location.getWorld() != null) {
            for (Entity entity : location.getWorld().getNearbyEntities(location, 0.13D, 0.2D, 0.13D)) {
                if (entity instanceof ItemFrame) {
                    return (ItemFrame) entity;
                }
            }
        }
        return null;
    }

    /**
     * Cleans up ItemFrames near the death location of a grave.
     *
     * @param grave The grave to clean up.
     */
    public void cleanupItemFrame(Grave grave) {
        Location location = grave.getLocationDeath();
        if (location.getWorld() != null) {
            for (Entity entity : location.getWorld().getNearbyEntities(location, 0.70, 1.0D, 0.7D)) {
                if (entity instanceof ItemFrame) {
                    entity.remove();
                }
            }
        }
    }

    private boolean placeFurniture(String name, Location location, Rotation rotation) {
        try {
            Furniture furniture = FurnitureManager.getInstance().getFurniture(name);
            furniture.spawn(location, rotation, null);
            return true;
        } catch (NoSuchMethodError ignored) {
            plugin.warningMessage("Furniture placing failed at:" + location.getWorld().getName()
                    + ", x" + location.getBlockX() + ", y" + location.getBlockY() + ", z" + location.getBlockZ());
            return false;
        }
    }

    private void breakFurniture(Location location) {
        try {
            Furniture furniture = FurnitureManager.getInstance().isFurniture(location);
            if (furniture != null) {
                furniture.breakFurniture(null, location);
            }
        } catch (NoSuchMethodError ignored) {
            plugin.warningMessage("Furniture breaking failed at:" + location.getWorld().getName()
                    + ", x" + location.getBlockX() + ", y" + location.getBlockY() + ", z" + location.getBlockZ());
        }
    }
}