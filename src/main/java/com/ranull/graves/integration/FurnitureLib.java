package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.furniturelib.ProjectBreakListener;
import com.ranull.graves.listener.integration.furniturelib.ProjectClickListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.Utilitis.LocationUtil;
import de.Ste3et_C0st.FurnitureLib.main.FurniturePlugin;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.entity.fContainerEntity;
import de.Ste3et_C0st.FurnitureLib.main.entity.fEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manages integration with the FurnitureLib plugin for creating, removing, and interacting with furniture.
 */
public final class FurnitureLib extends EntityDataManager {

    private final Graves plugin;
    private final de.Ste3et_C0st.FurnitureLib.main.FurnitureLib furnitureLib;
    private final ProjectClickListener projectClickListener;
    private final ProjectBreakListener projectBreakListener;

    /**
     * Constructs a FurnitureLib instance.
     *
     * @param plugin The main plugin instance.
     */
    public FurnitureLib(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
        this.furnitureLib = de.Ste3et_C0st.FurnitureLib.main.FurnitureLib.getInstance();
        this.projectClickListener = new ProjectClickListener(plugin, this);
        this.projectBreakListener = new ProjectBreakListener(this);
        registerListeners();
        new Furniture(plugin);
    }

    /**
     * Registers FurnitureLib event listeners.
     */
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(projectClickListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(projectBreakListener, plugin);
    }

    /**
     * Unregisters FurnitureLib event listeners.
     */
    public void unregisterListeners() {
        if (projectClickListener != null) {
            HandlerList.unregisterAll(projectClickListener);
        }
        if (projectBreakListener != null) {
            HandlerList.unregisterAll(projectBreakListener);
        }
    }

    /**
     * Checks if a player can build at a specific location using FurnitureLib's protection.
     *
     * @param location The location to check.
     * @param player   The player to check permissions for.
     * @return True if the player can build, false otherwise.
     */
    public boolean canBuild(Location location, Player player) {
        return furnitureLib.getPermManager().useProtectionLib()
                && furnitureLib.getPermManager().canBuild(player, location);
    }

    /**
     * Creates and places furniture at the specified location.
     *
     * @param location The location where the furniture should be placed.
     * @param grave    The grave associated with the furniture.
     */
    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("furniturelib.enabled", grave)
                .getBoolean("furniturelib.enabled")) {
            String name = plugin.getConfig("furniturelib.name", grave)
                    .getString("furniturelib.name", "");
            Project project = furnitureLib.getFurnitureManager().getProject(name);

            if (project != null && project.haveModelSchematic()) {
                location.getBlock().setType(Material.AIR);

                ObjectID objectID = new ObjectID(project.getName(), project.getPlugin().getName(), location);
                location.setYaw(furnitureLib.getLocationUtil().FaceToYaw(LocationUtil.yawToFace(location.getYaw())
                        .getOppositeFace()));
                furnitureLib.spawn(project, objectID);
                objectID.setUUID(UUID.randomUUID());
                objectID.getBlockList().stream()
                        .filter(signLocation -> signLocation.getBlock().getType().name().contains("SIGN"))
                        .forEach((signLocation) -> setSign(signLocation.getBlock(),
                                plugin.getConfig("furniturelib.line", grave)
                                        .getStringList("furniturelib.line"), grave));

                if (plugin.getConfig("furniturelib.head.replace", grave).getBoolean("furniturelib.head.replace")) {
                    objectID.getPacketList().stream()
                            .filter(fEntity -> fEntity instanceof fContainerEntity)
                            .map(fEntity -> (fContainerEntity) fEntity)
                            .forEach(fContainerEntity -> setSkull(fContainerEntity, grave));
                }

                furnitureLib.getFurnitureManager().addObjectID(objectID);
                createEntityData(objectID.getStartLocation(), objectID.getUUID(), grave.getUUID(),
                        EntityData.Type.FURNITURELIB);
            } else {
                plugin.debugMessage("Can't find FurnitureLib furniture " + name, 1);
            }
        }
    }

    /**
     * Removes furniture associated with the specified grave.
     *
     * @param grave The grave for which to remove furniture.
     */
    public void removeFurniture(Grave grave) {
        removeFurniture(getLoadedEntityDataList(grave));
    }

    /**
     * Removes specific furniture entity data.
     *
     * @param entityData The entity data of the furniture to remove.
     */
    public void removeFurniture(EntityData entityData) {
        removeFurniture(Collections.singletonList(entityData));
    }

    /**
     * Removes multiple pieces of furniture based on a list of entity data.
     *
     * @param entityDataList A list of entity data for the furniture to remove.
     */
    public void removeFurniture(List<EntityData> entityDataList) {
        List<EntityData> removeEntityDataList = new ArrayList<>();
        for (EntityData entityData : entityDataList) {
            for (ObjectID objectID : furnitureLib.getFurnitureManager().getObjectList()) {
                if (objectID.getUUID() != null && objectID.getUUID().equals(entityData.getUUIDEntity())) {
                    furnitureLib.getFurnitureManager().remove(objectID);
                    removeEntityDataList.add(entityData);
                }
            }
        }
        plugin.getDataManager().removeEntityData(removeEntityDataList);
    }

    private void setSign(Block block, List<String> stringList, Grave grave) {
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            int counter = 0;
            for (String string : stringList) {
                if (counter <= 4) {
                    if (plugin.getIntegrationManager().hasMiniMessage()) {
                        String newLine = StringUtil.parseString(string, block.getLocation(), grave, plugin);
                        sign.setLine(counter, MiniMessage.parseString(newLine));
                    } else {
                        sign.setLine(counter, StringUtil.parseString(string, block.getLocation(), grave, plugin));
                    }
                    counter++;
                } else {
                    break;
                }
            }
            sign.update(true, false);
        }
    }

    private void setSkull(fContainerEntity fEntity, Grave grave) {
        List<String> materialList = plugin.getConfig("furniturelib.head.material", grave)
                .getStringList("furniturelib.head.material");
        ItemStack itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        if (fEntity.getItemInMainHand() != null && materialList.contains(fEntity.getItemInMainHand().getType().name())
                && isSkullTextureBlank(fEntity.getItemInMainHand())) {
            fEntity.setItemInMainHand(itemStack);
        }
        if (fEntity.getItemInOffHand() != null && materialList.contains(fEntity.getItemInOffHand().getType().name())
                && isSkullTextureBlank(fEntity.getItemInOffHand())) {
            fEntity.setItemInOffHand(itemStack);
        }
        if (fEntity.getHelmet() != null && materialList.contains(fEntity.getHelmet().getType().name())
                && isSkullTextureBlank(fEntity.getHelmet())) {
            fEntity.setHelmet(itemStack);
        }
        if (fEntity.getChestPlate() != null && materialList.contains(fEntity.getChestPlate().getType().name())
                && isSkullTextureBlank(fEntity.getChestPlate())) {
            fEntity.setChestPlate(itemStack);
        }
        if (fEntity.getLeggings() != null && materialList.contains(fEntity.getLeggings().getType().name())
                && isSkullTextureBlank(fEntity.getLeggings())) {
            fEntity.setLeggings(itemStack);
        }
        if (fEntity.getBoots() != null && materialList.contains(fEntity.getBoots().getType().name())
                && isSkullTextureBlank(fEntity.getBoots())) {
            fEntity.setBoots(itemStack);
        }
    }

    private boolean isSkullTextureBlank(ItemStack itemStack) {
        return plugin.getCompatibility().getSkullTexture(itemStack) == null;
    }

    /**
     * Inner class to handle FurnitureLib plugin functionalities and project registration.
     */
    public class Furniture extends FurniturePlugin {

        /**
         * Constructs a Furniture instance.
         *
         * @param plugin The main plugin instance.
         */
        public Furniture(Plugin plugin) {
            super(plugin);
            register();
        }

        @Override
        public void registerProjects() {
            String path = "data" + File.separator + "plugin" + File.separator + "furniturelib";
            try {
                new Project("Grave1", getPlugin(), getResource(path + File.separator + "Grave1.dModel"));
                new Project("Grave2", getPlugin(), getResource(path + File.separator + "Grave2.dModel"));
                new Project("Grave3", getPlugin(), getResource(path + File.separator + "Grave3.dModel"));
                new Project("Skull1", getPlugin(), getResource(path + File.separator + "Skull1.dModel"));
            } catch (Exception exception) {
                plugin.warningMessage(exception.getMessage());
            }
        }

        @Override
        public void applyPluginFunctions() {
            furnitureLib.getFurnitureManager().getProjects().stream().filter(project -> project.getPlugin().getName()
                    .equals(getPlugin().getDescription().getName())).forEach(Project::applyFunction);
        }

        @Override
        public void onFurnitureLateSpawn(ObjectID objectID) {
        }
    }
}
