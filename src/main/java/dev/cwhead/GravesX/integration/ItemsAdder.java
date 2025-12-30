package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ItemsAdder v4 runtime integration.
 *
 * <p>This class provides basic spawn/remove checks for ItemsAdder blocks and furniture
 * used by GravesX. The integration is event-gated via {@link #isReady()} and should be
 * enabled when ItemsAdder finishes loading content (e.g. after {@code /iareload} or {@code /iazip}).</p>
 */
public final class ItemsAdder extends EntityDataManager {

    /** GravesX plugin instance. */
    private final Graves plugin;

    /** Whether ItemsAdder has finished loading and the API is safe to use. */
    private final AtomicBoolean ready = new AtomicBoolean(false);

    /**
     * Creates the integration manager.
     *
     * @param plugin GravesX plugin instance
     */
    public ItemsAdder(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * @return {@code true} if ItemsAdder content has been loaded and integration is enabled
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * Sets the current readiness state.
     *
     * @param value new readiness state
     */
    public void setReady(boolean value) {
        ready.set(value);
    }

    /**
     * Ensures the integration is ready before performing an action.
     *
     * @param grave grave being processed (may be null)
     * @param action short action label for debug output
     * @return {@code true} if ready, otherwise {@code false}
     */
    private boolean requireReady(Grave grave, String action) {
        if (ready.get()) return true;

        if (grave != null) {
            plugin.debugMessage("ItemsAdder not ready; skipping " + action + " for grave " + grave.getUUID(), 2);
        } else {
            plugin.debugMessage("ItemsAdder not ready; skipping " + action, 2);
        }
        return false;
    }

    /**
     * Spawns an ItemsAdder furniture entity for the given grave, if enabled in config.
     *
     * @param location target location
     * @param grave grave context
     */
    public void createFurniture(Location location, Grave grave) {
        if (grave == null || location == null || location.getWorld() == null) return;
        if (!requireReady(grave, "furniture spawn")) return;

        location = LocationUtil.roundLocation(location).add(0.5, 0, 0.5);
        location.setYaw(BlockFaceUtil.getBlockFaceYaw(
                BlockFaceUtil.getYawBlockFace(location.getYaw()).getOppositeFace()
        ));
        location.setPitch(grave.getPitch());

        if (!plugin.getConfigManager().getConfigSection("itemsadder.furniture.enabled", grave)
                .getBoolean("itemsadder.furniture.enabled")) {
            return;
        }

        String name = plugin.getConfigManager()
                .getConfigSection("itemsadder.furniture.name", grave)
                .getString("itemsadder.furniture.name", "");

        if (name.isEmpty()) return;

        location.getBlock().setType(Material.AIR);

        CustomFurniture furniture = CustomFurniture.spawn(name, location.getBlock());
        Entity entity = (furniture != null) ? furniture.getEntity() : null;

        if (furniture != null && entity != null) {
            furniture.teleport(location);
            createEntityData(entity, grave, EntityData.Type.ITEMSADDER);

            plugin.debugMessage("Placing ItemsAdder furniture for " + grave.getUUID() + " at "
                    + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                    + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
        } else {
            plugin.debugMessage("Can't find ItemsAdder furniture " + name, 1);
        }
    }

    /**
     * Removes all ItemsAdder furniture entities associated with a grave.
     *
     * @param grave grave context
     */
    public void removeFurniture(Grave grave) {
        if (grave == null) return;
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes a single ItemsAdder furniture entity referenced by entity data.
     *
     * @param entityData entity data entry
     */
    public void removeFurniture(EntityData entityData) {
        if (entityData == null) return;
        removeFurniture(getEntityDataMap(List.of(entityData)));
    }

    /**
     * Removes ItemsAdder furniture entities for each entry in the provided map and
     * deletes their associated entity data records.
     *
     * @param entityDataMap entity data to entity map
     */
    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        if (entityDataMap == null || entityDataMap.isEmpty()) return;

        List<EntityData> toRemove = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            Entity e = entry.getValue();
            if (e == null) continue;

            try {
                CustomFurniture.remove(e, false);
            } catch (Throwable t) {
                try {
                    e.remove();
                } catch (Throwable ignored) {}
            }

            toRemove.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(toRemove);
    }

    /**
     * Places an ItemsAdder custom block for the given grave, if enabled in config.
     *
     * @param location target location
     * @param grave grave context
     */
    public void createBlock(Location location, Grave grave) {
        if (grave == null || location == null || location.getWorld() == null) return;
        if (!requireReady(grave, "block place")) return;

        if (!plugin.getConfigManager().getConfigSection("itemsadder.block.enabled", grave)
                .getBoolean("itemsadder.block.enabled")) {
            return;
        }

        String name = plugin.getConfigManager()
                .getConfigSection("itemsadder.block.name", grave)
                .getString("itemsadder.block.name", "");

        if (name.isEmpty()) return;

        CustomBlock block = CustomBlock.place(name, location);

        if (block != null) {
            plugin.debugMessage("Placing ItemsAdder block for " + grave.getUUID() + " at "
                    + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                    + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
        } else {
            plugin.debugMessage("Can't find ItemsAdder block " + name, 1);
        }
    }

    /**
     * Checks whether the block at the location is an ItemsAdder custom block.
     *
     * @param location location to test
     * @return {@code true} if the block is a custom block
     */
    public boolean isCustomBlock(Location location) {
        if (location != null && location.getWorld() != null) {
            CustomBlock.byAlreadyPlaced(location.getBlock());
        }
        return false;
    }

    /**
     * Removes an ItemsAdder custom block at the given location.
     *
     * @param location block location
     */
    public void removeBlock(Location location) {
        if (location == null || location.getWorld() == null) return;

        boolean removed = CustomBlock.remove(location);
        if (!removed) {
            plugin.debugMessage("Failed to remove ItemsAdder block at " + location.getWorld().getName() + ", "
                    + location.getBlockX() + "x, " + location.getBlockY() + "y, " + location.getBlockZ() + "z", 2);
        }
    }

    /**
     * Checks whether this grave currently has a valid ItemsAdder furniture entity attached.
     *
     * <p>This looks up the grave's stored entity data and verifies at least one linked entity
     * is still alive/valid and recognized by ItemsAdder as furniture.</p>
     *
     * @param grave grave context
     * @return {@code true} if a linked ItemsAdder furniture entity is present
     */
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
     * Checks whether the grave location currently contains an ItemsAdder custom block.
     *
     * @param grave grave context
     * @return {@code true} if an ItemsAdder custom block is present at the grave location
     */
    public boolean hasBlock(Grave grave) {
        if (grave == null) return false;

        Location loc = grave.getLocationDeath();
        if (loc == null || loc.getWorld() == null) return false;

        return isCustomBlock(loc);
    }
}