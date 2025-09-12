package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.oraxen.EntityDamageListener;
import com.ranull.graves.listener.integration.oraxen.HangingBreakListener;
import com.ranull.graves.listener.integration.oraxen.PlayerInteractEntityListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ResourceUtil;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Recommend Nexo as a replacement.
 *
 * Integration class for handling communication with the Oraxen plugin.
 * Manages creation, removal, and verification of Oraxen furniture and blocks.
 */
@Deprecated
public final class Oraxen extends EntityDataManager {
    private final Graves plugin;
    private final Plugin oraxenPlugin;
    private final PlayerInteractEntityListener playerInteractEntityListener;
    private final EntityDamageListener entityDamageListener;
    private final HangingBreakListener hangingBreakListener;

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Constructs a new Oraxen instance and initializes listeners.
     *
     * @param plugin       The main Graves plugin instance.
     * @param oraxenPlugin The Oraxen plugin instance.
     */
    @Deprecated
    public Oraxen(Graves plugin, Plugin oraxenPlugin) {
        super(plugin);

        this.plugin = plugin;
        this.oraxenPlugin = oraxenPlugin;
        this.playerInteractEntityListener = new PlayerInteractEntityListener(plugin, this);
        this.entityDamageListener = new EntityDamageListener(this);
        this.hangingBreakListener = new HangingBreakListener(this);

        saveData();
        registerListeners();
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Saves the data related to the Oraxen plugin.
     */
    @Deprecated
    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.oraxen.write")) {
            String pluginFolder = plugin.getPluginsFolder() + "/" + oraxenPlugin.getName();

            try {
                File itemsDir = new File(pluginFolder + "/items");
                if (!itemsDir.exists()) itemsDir.mkdirs();

                File modelDir = new File(pluginFolder + "/pack/assets/minecraft/models/gravesx");
                if (!modelDir.exists()) modelDir.mkdirs();

                // Copy resources
                copyResource(
                        "data/plugin/" + oraxenPlugin.getName().toLowerCase() + "/items/graves.yml",
                        new File(itemsDir, "graves.yml")
                );
                copyResource(
                        "data/model/grave.json",
                        new File(modelDir, "grave.json")
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save " + oraxenPlugin.getName() + " data.");
                plugin.logStackTrace(e);
            }

            plugin.debugMessage("Saving " + oraxenPlugin.getName() + " data.", 1);
        }
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Copies resource from jar to the oraxen folder.
     */
    @Deprecated
    private void copyResource(String resourcePath, File outFile) {
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                plugin.debugMessage("Resource not found: " + resourcePath, 1);
                return;
            }
            Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().info("An error occurred while copying " + resourcePath + " to " + outFile + ".");
            plugin.logStackTrace(e);
        }
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Registers event listeners for Oraxen-related events.
     */
    @Deprecated
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(playerInteractEntityListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(entityDamageListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(hangingBreakListener, plugin);
    }

    /**
     * Unregisters event listeners to prevent memory leaks or other issues.
     */
    @Deprecated
    public void unregisterListeners() {
        if (playerInteractEntityListener != null) {
            HandlerList.unregisterAll(playerInteractEntityListener);
        }

        if (entityDamageListener != null) {
            HandlerList.unregisterAll(entityDamageListener);
        }

        if (hangingBreakListener != null) {
            HandlerList.unregisterAll(hangingBreakListener);
        }
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Creates and places Oraxen furniture at a specified location.
     *
     * @param location The location where the furniture will be placed.
     * @param grave    The grave related to the furniture.
     */
    @Deprecated
    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("oraxen.furniture.enabled", grave)
                .getBoolean("oraxen.furniture.enabled")) {
            try {
                String name = plugin.getConfig("oraxen.furniture.name", grave)
                        .getString("oraxen.furniture.name", "");
                FurnitureMechanic furnitureMechanic = getFurnitureMechanic(name);

                if (furnitureMechanic != null && location.getWorld() != null) {
                    location.getBlock().setType(Material.AIR);

                    Entity furniture = furnitureMechanic.place(location, location.getYaw(), BlockFace.UP);
                    if (furniture != null) {
                        createEntityData(location, furniture.getUniqueId(), grave.getUUID(), EntityData.Type.ORAXEN);
                        plugin.debugMessage("Placing Oraxen furniture for " + grave.getUUID() + " at "
                                + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                                + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
                    }
                }
            } catch (NoSuchMethodError ignored) {
                plugin.warningMessage("This version of Minecraft does not support " + oraxenPlugin.getName()
                        + " furniture");
            }
        }
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Removes all Oraxen furniture associated with a specified grave.
     *
     * @param grave The grave whose associated furniture will be removed.
     */
    @Deprecated
    public void removeFurniture(Grave grave) {
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Removes a specific Oraxen furniture entity based on entity data.
     *
     * @param entityData The entity data of the furniture to be removed.
     */
    @Deprecated
    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Removes Oraxen furniture entities based on a map of entity data to entities.
     *
     * @param entityDataMap A map of entity data to entities to be removed.
     */
    @Deprecated
    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Creates and places an Oraxen block at a specified location.
     *
     * @param location The location where the block will be placed.
     * @param grave    The grave related to the block.
     */
    @Deprecated
    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfig("oraxen.block.enabled", grave)
                .getBoolean("oraxen.block.enabled")) {
            String name = plugin.getConfig("oraxen.block.name", grave)
                    .getString("oraxen.block.name", "");
            NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(name);

            if (noteBlockMechanic != null && location.getWorld() != null) {
                location.getBlock().setBlockData(NoteBlockMechanicFactory
                        .createNoteBlockData(noteBlockMechanic.getCustomVariation()), false);
                plugin.debugMessage("Placing Oraxen block for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
            }
        }
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Checks if a block at a specified location is a custom Oraxen block.
     *
     * @param location The location of the block to check.
     * @return True if the block is a custom Oraxen block, false otherwise.
     */
    @Deprecated
    public boolean isCustomBlock(Location location) {
        if (location.getBlock().getBlockData() instanceof NoteBlock) {
            NoteBlock noteBlock = (NoteBlock) location.getBlock().getBlockData();

            return NoteBlockMechanicFactory.getBlockMechanic((int) (noteBlock
                    .getInstrument().getType()) * 25 + (int) noteBlock.getNote().getId()
                    + (noteBlock.isPowered() ? 400 : 0) - 26) != null;
        }

        return false;
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Removes a block at a specified location.
     *
     * @param location The location of the block to be removed.
     */
    @Deprecated
    public void removeBlock(Location location) {
        location.getBlock().setType(Material.AIR);
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Retrieves a FurnitureMechanic by name from the Oraxen plugin.
     *
     * @param string The name of the furniture mechanic.
     * @return The FurnitureMechanic if found, otherwise null.
     */
    @Deprecated
    public FurnitureMechanic getFurnitureMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("furniture");

        return mechanicFactory != null ? (FurnitureMechanic) mechanicFactory.getMechanic(string) : null;
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Retrieves a NoteBlockMechanic by name from the Oraxen plugin.
     *
     * @param string The name of the note block mechanic.
     * @return The NoteBlockMechanic if found, otherwise null.
     */
    @Deprecated
    public NoteBlockMechanic getNoteBlockMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");

        return mechanicFactory != null ? (NoteBlockMechanic) mechanicFactory.getMechanic(string) : null;
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * True if an Oraxen furniture entity for this grave is currently spawned.
     *
     * @param grave The grave to check.
     * @return True if at least one valid Oraxen furniture entity mapped to this grave exists.
     */
    @Deprecated
    public boolean hasFurniture(Grave grave) {
        if (grave == null) return false;

        Map<EntityData, Entity> map = getEntityDataMap(getLoadedEntityDataList(grave));
        if (map.isEmpty()) return false;

        for (Map.Entry<EntityData, Entity> e : map.entrySet()) {
            EntityData data = e.getKey();
            Entity ent = e.getValue();
            if (data == null || ent == null) continue;

            // Ensure it’s our Oraxen-tracked furniture and still alive.
            if (data.getType() == EntityData.Type.ORAXEN && ent.isValid() && !ent.isDead()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * True if an Oraxen custom block exists at the grave location.
     *
     * @param grave The grave to check.
     * @return True if a custom Oraxen block is present where the grave is placed.
     */
    @Deprecated
    public boolean hasBlock(Grave grave) {
        if (grave == null) return false;

        Location loc = grave.getLocationDeath();
        if (loc == null || loc.getWorld() == null) {
            loc = grave.getLocationDeath();
        }
        if (loc == null || loc.getWorld() == null) return false;

        return isCustomBlock(loc);
    }
}