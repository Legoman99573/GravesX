package com.ranull.graves.integration;

import com.nexomc.nexo.mechanics.MechanicFactory;
import com.nexomc.nexo.mechanics.MechanicsManager;
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic;
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.nexo.EntityDamageListener;
import com.ranull.graves.listener.integration.nexo.HangingBreakListener;
import com.ranull.graves.listener.integration.nexo.PlayerInteractEntityListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class Nexo extends EntityDataManager {
    private final Graves plugin;
    private final Plugin nexoPlugin;
    private final PlayerInteractEntityListener playerInteractEntityListener;
    private final EntityDamageListener entityDamageListener;
    private final HangingBreakListener hangingBreakListener;

    /**
     * Initializes the EntityDataManager with the specified plugin instance.
     *
     * @param plugin the Graves plugin instance.
     */
    public Nexo(Graves plugin, Plugin nexoPlugin) {
        super(plugin);
        this.plugin = plugin;
        this.nexoPlugin = nexoPlugin;
        this.playerInteractEntityListener = new PlayerInteractEntityListener(plugin, this);
        this.entityDamageListener = new EntityDamageListener(this);
        this.hangingBreakListener = new HangingBreakListener(this);

        saveData();
        registerListeners();
    }

    /**
     * Saves the data related to the Nexo plugin.
     */
    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.nexo.write")) {
            String pluginFolder = plugin.getPluginsFolder() + "/" + nexoPlugin.getName();

            try {
                File gravesDir = new File(pluginFolder + "/items");
                if (!gravesDir.exists()) gravesDir.mkdirs();

                File graveModelDir = new File(pluginFolder + "/pack/assets/minecraft/models/gravesx");
                if (!graveModelDir.exists()) graveModelDir.mkdirs();

                // Copy resources
                copyResource(
                        "data/plugin/" + nexoPlugin.getName().toLowerCase() + "/items/graves.yml",
                        new File(gravesDir, "graves.yml")
                );
                copyResource(
                        "data/model/grave.json",
                        new File(graveModelDir, "grave.json")
                );
            } catch (Exception e) {
                plugin.logStackTrace(e);
            }

            plugin.debugMessage("Saving " + nexoPlugin.getName() + " data.", 1);
        }
    }

    /**
     * Copies resource from jar to the nexo folder.
     */
    private void copyResource(String resourcePath, File outFile) {
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                plugin.debugMessage("Resource not found: " + resourcePath, 1);
                return;
            }
            Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().info("An error occurred while copying over " + resourcePath + " to " + outFile + ".");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Registers event listeners for Nexo-related events.
     */
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(playerInteractEntityListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(entityDamageListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(hangingBreakListener, plugin);
    }

    /**
     * Unregisters event listeners to prevent memory leaks or other issues.
     */
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
     * Creates and places Nexo furniture at a specified location.
     *
     * @param location The location where the furniture will be placed.
     * @param grave    The grave related to the furniture.
     */
    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("nexo.furniture.enabled", grave)
                .getBoolean("nexo.furniture.enabled")) {
            try {
                String name = plugin.getConfig("nexo.furniture.name", grave)
                        .getString("nexo.furniture.name", "");
                FurnitureMechanic furnitureMechanic = getFurnitureMechanic(name);

                if (furnitureMechanic != null && location.getWorld() != null) {
                    location.getBlock().setType(Material.AIR);

                    Entity furniture = furnitureMechanic.place(location, location.getYaw(), BlockFace.UP);
                    if (furniture != null) {
                        createEntityData(location, furniture.getUniqueId(), grave.getUUID(), EntityData.Type.NEXO);
                        plugin.debugMessage("Placing Nexo furniture for " + grave.getUUID() + " at "
                                + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                                + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
                    }
                }
            } catch (NoSuchMethodError ignored) {
                plugin.warningMessage("This version of Minecraft does not support " + nexoPlugin.getName()
                        + " furniture");
            }
        }
    }

    /**
     * Removes all Nexo furniture associated with a specified grave.
     *
     * @param grave The grave whose associated furniture will be removed.
     */
    public void removeFurniture(Grave grave) {
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes a specific Nexo furniture entity based on entity data.
     *
     * @param entityData The entity data of the furniture to be removed.
     */
    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes Nexo furniture entities based on a map of entity data to entities.
     *
     * @param entityDataMap A map of entity data to entities to be removed.
     */
    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * Creates and places a Nexo block at a specified location.
     *
     * @param location The location where the block will be placed.
     * @param grave    The grave related to the block.
     */
    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfig("nexo.block.enabled", grave).getBoolean("nexo.block.enabled")) {
            String name = plugin.getConfig("nexo.block.name", grave).getString("nexo.block.name", "");

            if (!name.isEmpty() && location.getWorld() != null) {
                Block block = location.getBlock();
                NoteBlockMechanicFactory.Companion.setBlockModel(block, name);

                plugin.debugMessage(
                        "Placing nexo block for " + grave.getUUID() + " at " +
                                location.getWorld().getName() + ", " +
                                (location.getBlockX() + 0.5) + "x, " +
                                (location.getBlockY() + 0.5) + "y, " +
                                (location.getBlockZ() + 0.5) + "z",
                        1
                );
            }
        }
    }

    /**
     * Removes a block at a specified location.
     *
     * @param location The location of the block to be removed.
     */
    public void removeBlock(Location location) {
        location.getBlock().setType(Material.AIR);
    }

    /**
     * Checks if a block at a specified location is a custom Nexo block.
     *
     * @param location The location of the block to check.
     * @return True if the block is a custom Nexo block, false otherwise.
     */
    @SuppressWarnings("deprecation")
    public boolean isCustomBlock(Location location) {
        if (location.getBlock().getBlockData() instanceof NoteBlock) {
            NoteBlock noteBlock = (NoteBlock) location.getBlock().getBlockData();

            int id = (int) (noteBlock.getInstrument().getType()) * 25
                    + (int) noteBlock.getNote().getId()
                    + (noteBlock.isPowered() ? 400 : 0)
                    - 26;

            String key = String.valueOf(id); // Convert int to String

            NoteBlockMechanicFactory factory = NoteBlockMechanicFactory.Companion.instance();

            return factory != null && factory.getMechanic(key) != null;
        }

        return false;
    }
    
    /**
     * Retrieves a FurnitureMechanic by name from the Nexo plugin.
     *
     * @param string The name of the furniture mechanic.
     * @return The FurnitureMechanic if found, otherwise null.
     */
    public FurnitureMechanic getFurnitureMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.INSTANCE.getMechanicFactory("furniture");

        return mechanicFactory != null ? (FurnitureMechanic) mechanicFactory.getMechanic(string) : null;
    }

    /**
     * Retrieves a NoteBlockMechanic by name from the Nexo plugin.
     *
     * @param string The name of the note block mechanic.
     * @return The NoteBlockMechanic if found, otherwise null.
     */
    public NoteBlockMechanic getNoteBlockMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.INSTANCE.getMechanicFactory("noteblock");

        return mechanicFactory != null ? (NoteBlockMechanic) mechanicFactory.getMechanic(string) : null;
    }
}