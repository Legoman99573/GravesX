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

public final class FurnitureEngine extends EntityDataManager {
    private final Graves plugin;
    private final FurnitureInteractListener furnitureInteractListener;
    private final FurnitureBreakListener furnitureBreakListener;

    public FurnitureEngine(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
        this.furnitureInteractListener = new FurnitureInteractListener(plugin, this);
        this.furnitureBreakListener = new FurnitureBreakListener(this);

        registerListeners();
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(furnitureInteractListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(furnitureBreakListener, plugin);
    }

    public void unregisterListeners() {
        if (furnitureInteractListener != null) {
            HandlerList.unregisterAll(furnitureInteractListener);
        }

        if (furnitureBreakListener != null) {
            HandlerList.unregisterAll(furnitureBreakListener);
        }
    }

    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("furnitureengine.enabled", grave)
                .getBoolean("furnitureengine.enabled")) {
            String name = plugin.getConfig("furnitureengine.name", grave)
                    .getString("furnitureengine.name", "");

            location.getBlock().setType(Material.AIR);

            if (placeFurniture(name, location, BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil
                    .getYawBlockFace(location.getYaw())))) {
                ItemFrame itemFrame = getItemFrame(location);

                if (itemFrame != null && location.getWorld() != null) {
                    createEntityData(location, itemFrame.getUniqueId(), grave.getUUID(),
                            EntityData.Type.FURNITUREENGINE);
                    plugin.debugMessage("Placing FurnitureEngine furniture for " + grave.getUUID() + " at "
                            + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                            + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
                }
            }
        }
    }

    public void removeFurniture(Grave grave) {
        cleanupItemFrame(grave);
        try {
            breakFurniture(grave.getLocationDeath());
        } catch (NullPointerException ignored) {
        }
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(Collections.singletonList(entityData)));
    }

    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            breakFurniture(entry.getValue().getLocation());
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

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
            if (furniture != null)
                furniture.breakFurniture(null, location);
        } catch (NoSuchMethodError ignored) {
            plugin.warningMessage("Furniture breaking failed at:" + location.getWorld().getName()
                    + ", x" + location.getBlockX() + ", y" + location.getBlockY() + ", z" + location.getBlockZ());
        }
    }
}