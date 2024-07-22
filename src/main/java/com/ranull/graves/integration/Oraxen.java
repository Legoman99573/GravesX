package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.oraxen.EntityDamageListener;
import com.ranull.graves.listener.integration.oraxen.HangingBreakListener;
import com.ranull.graves.listener.integration.oraxen.PlayerInteractEntityListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
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
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Integration class for handling communication with the Oraxen plugin.
 * Manages creation, removal, and verification of Oraxen furniture and blocks.
 */
public final class Oraxen extends EntityDataManager {
    private final Graves plugin;
    private final Plugin oraxenPlugin;
    private final PlayerInteractEntityListener playerInteractEntityListener;
    private final EntityDamageListener entityDamageListener;
    private final HangingBreakListener hangingBreakListener;

    /**
     * Constructs a new Oraxen instance and initializes listeners.
     *
     * @param plugin       The main Graves plugin instance.
     * @param oraxenPlugin The Oraxen plugin instance.
     */
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
     * Saves the data related to the Oraxen plugin.
     */
    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.oraxen.write")) {
            ResourceUtil.copyResources("data/plugin/" + oraxenPlugin.getName().toLowerCase() + "/items",
                    plugin.getPluginsFolder() + "/" + oraxenPlugin.getName() + "/items", plugin);
            ResourceUtil.copyResources("data/model/grave.json",
                    plugin.getPluginsFolder() + "/" + oraxenPlugin.getName()
                            + "/pack/assets/minecraft/models/graves/grave.json", plugin);
            plugin.debugMessage("Saving " + oraxenPlugin.getName() + " data.", 1);
        }
    }

    /**
     * Registers event listeners for Oraxen-related events.
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
     * Creates and places Oraxen furniture at a specified location.
     *
     * @param location The location where the furniture will be placed.
     * @param grave    The grave related to the furniture.
     */
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
     * Removes all Oraxen furniture associated with a specified grave.
     *
     * @param grave The grave whose associated furniture will be removed.
     */
    public void removeFurniture(Grave grave) {
        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes a specific Oraxen furniture entity based on entity data.
     *
     * @param entityData The entity data of the furniture to be removed.
     */
    public void removeFurniture(EntityData entityData) {
        removeFurniture(getEntityDataMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes Oraxen furniture entities based on a map of entity data to entities.
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
     * Creates and places an Oraxen block at a specified location.
     *
     * @param location The location where the block will be placed.
     * @param grave    The grave related to the block.
     */
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
     * Checks if a block at a specified location is a custom Oraxen block.
     *
     * @param location The location of the block to check.
     * @return True if the block is a custom Oraxen block, false otherwise.
     */
    @SuppressWarnings("deprecation")
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
     * Removes a block at a specified location.
     *
     * @param location The location of the block to be removed.
     */
    public void removeBlock(Location location) {
        location.getBlock().setType(Material.AIR);
    }

    /**
     * Retrieves a FurnitureMechanic by name from the Oraxen plugin.
     *
     * @param string The name of the furniture mechanic.
     * @return The FurnitureMechanic if found, otherwise null.
     */
    public FurnitureMechanic getFurnitureMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("furniture");

        return mechanicFactory != null ? (FurnitureMechanic) mechanicFactory.getMechanic(string) : null;
    }

    /**
     * Retrieves a NoteBlockMechanic by name from the Oraxen plugin.
     *
     * @param string The name of the note block mechanic.
     * @return The NoteBlockMechanic if found, otherwise null.
     */
    public NoteBlockMechanic getNoteBlockMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");

        return mechanicFactory != null ? (NoteBlockMechanic) mechanicFactory.getMechanic(string) : null;
    }
}