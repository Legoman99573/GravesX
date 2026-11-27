package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import dev.cwhead.GravesX.listener.integration.craftengine.CraftEngineBlockListener;
import dev.cwhead.GravesX.listener.integration.craftengine.CraftEngineFurnitureListener;
import dev.cwhead.GravesX.provider.CustomItemStorageProvider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CraftEngine extends EntityDataManager implements CustomItemStorageProvider {
    private static final String PROVIDER_ID = "CRAFTENGINE";

    private final Graves plugin;
    private final Plugin craftEnginePlugin;
    private final Method keyOfStringMethod;
    private final Method isCustomItemMethod;
    private final Method getCustomItemIdMethod;
    private final Method itemByIdMethod;
    private final Method blockPlaceMethod;
    private final Method blockRemoveMethod;
    private final Method blockIsCustomBlockMethod;
    private final Method blockGetCustomStateMethod;
    private final Method furniturePlaceMethod;
    private final Method furnitureRemoveMethod;
    private final Method furnitureIsFurnitureMethod;
    private final Method furnitureIsCollisionEntityMethod;
    private final Method furnitureIsSeatMethod;
    private final Method furnitureByMetaEntityMethod;
    private final Method furnitureByColliderMethod;
    private final Method furnitureBySeatMethod;
    private final CraftEngineBlockListener blockListener;
    private final CraftEngineFurnitureListener furnitureListener;

    public CraftEngine(Graves plugin, Plugin craftEnginePlugin) throws ReflectiveOperationException {
        super(plugin);
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.craftEnginePlugin = Objects.requireNonNull(craftEnginePlugin, "craftEnginePlugin");

        ClassLoader classLoader = craftEnginePlugin.getClass().getClassLoader();
        Class<?> keyClass = Class.forName("net.momirealms.craftengine.core.util.Key", true, classLoader);
        Class<?> craftEngineItems = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems", true, classLoader);
        Class<?> craftEngineBlocks = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks", true, classLoader);
        Class<?> craftEngineFurniture = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineFurniture", true, classLoader);

        this.keyOfStringMethod = keyClass.getMethod("of", String.class);
        this.isCustomItemMethod = craftEngineItems.getMethod("isCustomItem", ItemStack.class);
        this.getCustomItemIdMethod = craftEngineItems.getMethod("getCustomItemId", ItemStack.class);
        this.itemByIdMethod = craftEngineItems.getMethod("byId", String.class);
        this.blockPlaceMethod = craftEngineBlocks.getMethod("place", Location.class, keyClass, boolean.class);
        this.blockRemoveMethod = craftEngineBlocks.getMethod("remove", Block.class);
        this.blockIsCustomBlockMethod = craftEngineBlocks.getMethod("isCustomBlock", Block.class);
        this.blockGetCustomStateMethod = craftEngineBlocks.getMethod("getCustomBlockState", Block.class);
        this.furniturePlaceMethod = craftEngineFurniture.getMethod("place", Location.class, keyClass);
        this.furnitureRemoveMethod = craftEngineFurniture.getMethod("remove", Entity.class);
        this.furnitureIsFurnitureMethod = craftEngineFurniture.getMethod("isFurniture", Entity.class);
        this.furnitureIsCollisionEntityMethod = craftEngineFurniture.getMethod("isCollisionEntity", Entity.class);
        this.furnitureIsSeatMethod = craftEngineFurniture.getMethod("isSeat", Entity.class);
        this.furnitureByMetaEntityMethod = craftEngineFurniture.getMethod("getLoadedFurnitureByMetaEntity", Entity.class);
        this.furnitureByColliderMethod = craftEngineFurniture.getMethod("getLoadedFurnitureByCollider", Entity.class);
        this.furnitureBySeatMethod = craftEngineFurniture.getMethod("getLoadedFurnitureBySeat", Entity.class);
        this.blockListener = new CraftEngineBlockListener(plugin, this);
        this.furnitureListener = new CraftEngineFurnitureListener(plugin, this);

        registerListeners();
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    public Plugin getCraftEnginePlugin() {
        return craftEnginePlugin;
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(blockListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(furnitureListener, plugin);
    }

    public void unregisterListeners() {
        HandlerList.unregisterAll(blockListener);
        HandlerList.unregisterAll(furnitureListener);
    }

    public boolean isCustomItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(isCustomItemMethod.invoke(null, itemStack));
        } catch (Throwable throwable) {
            debugFailure("custom item check", throwable);
            return false;
        }
    }

    @Override
    public String getCustomItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !isCustomItem(itemStack)) {
            return null;
        }

        try {
            Object key = getCustomItemIdMethod.invoke(null, itemStack);
            return key != null ? key.toString() : null;
        } catch (Throwable throwable) {
            debugFailure("custom item id lookup", throwable);
            return null;
        }
    }

    @Override
    public ItemStack rebuildCustomItem(String id, int amount) {
        if (id == null || id.isBlank()) {
            return null;
        }

        try {
            Object definition = itemByIdMethod.invoke(null, id);
            if (definition == null) {
                return null;
            }

            Method buildBukkitItem = getBuildBukkitItemMethod(definition.getClass());
            Object object = buildBukkitItem.invoke(definition);

            if (object instanceof ItemStack itemStack) {
                itemStack.setAmount(Math.max(1, amount));
                return itemStack;
            }
        } catch (Throwable throwable) {
            debugFailure("custom item rebuild for " + id, throwable);
        }

        return null;
    }

    public void createBlock(Location location, Grave grave) {
        if (location == null || location.getWorld() == null || grave == null) {
            return;
        }

        if (!plugin.getConfigManager().getConfigSection("craftengine.block.enabled", grave)
                .getBoolean("craftengine.block.enabled", false)) {
            return;
        }

        String name = plugin.getConfigManager()
                .getConfigSection("craftengine.block.name", grave)
                .getString("craftengine.block.name", "");

        if (name == null || name.isBlank()) {
            return;
        }

        try {
            boolean placed = Boolean.TRUE.equals(blockPlaceMethod.invoke(null, location, toKey(name), true));
            if (placed) {
                plugin.debugMessage("Placing CraftEngine block for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "y, " + (location.getBlockZ() + 0.5) + "z", 1);
            } else {
                plugin.debugMessage("Can't find CraftEngine block " + name, 1);
            }
        } catch (Throwable throwable) {
            debugFailure("block place for " + name, throwable);
        }
    }

    public boolean isCustomBlock(Location location) {
        return location != null && location.getWorld() != null && isCustomBlock(location.getBlock());
    }

    public boolean isCustomBlock(Block block) {
        if (block == null || block.getWorld() == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(blockIsCustomBlockMethod.invoke(null, block));
        } catch (Throwable throwable) {
            debugFailure("custom block check", throwable);
            return false;
        }
    }

    public String getCustomBlockId(Block block) {
        if (block == null || block.getWorld() == null) {
            return null;
        }

        try {
            Object state = blockGetCustomStateMethod.invoke(null, block);
            return state != null ? getBlockStateId(state) : null;
        } catch (Throwable throwable) {
            debugFailure("custom block id lookup", throwable);
            return null;
        }
    }

    public void removeBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        try {
            blockRemoveMethod.invoke(null, location.getBlock());
        } catch (Throwable throwable) {
            debugFailure("block remove", throwable);
        }
    }

    public void createFurniture(Location location, Grave grave) {
        if (location == null || location.getWorld() == null || grave == null) {
            return;
        }

        if (!plugin.getConfigManager().getConfigSection("craftengine.furniture.enabled", grave)
                .getBoolean("craftengine.furniture.enabled", false)) {
            return;
        }

        String name = plugin.getConfigManager()
                .getConfigSection("craftengine.furniture.name", grave)
                .getString("craftengine.furniture.name", "");

        if (name == null || name.isBlank()) {
            return;
        }

        Location spawnLocation = LocationUtil.roundLocation(location).add(0.5, 0, 0.5);
        spawnLocation.setYaw(BlockFaceUtil.getBlockFaceYaw(
                BlockFaceUtil.getYawBlockFace(spawnLocation.getYaw()).getOppositeFace()
        ));
        spawnLocation.setPitch(grave.getPitch());

        try {
            spawnLocation.getBlock().setType(Material.AIR);
            Object furniture = furniturePlaceMethod.invoke(null, spawnLocation, toKey(name));
            Entity entity = getFurnitureBaseEntity(furniture);

            if (entity != null) {
                createEntityData(spawnLocation, entity.getUniqueId(), grave.getUUID(), EntityData.Type.CRAFTENGINE);

                plugin.debugMessage("Placing CraftEngine furniture for " + grave.getUUID() + " at "
                        + spawnLocation.getWorld().getName() + ", " + (spawnLocation.getBlockX() + 0.5) + "x, "
                        + (spawnLocation.getBlockY() + 0.5) + "y, " + (spawnLocation.getBlockZ() + 0.5) + "z", 1);
            } else {
                plugin.debugMessage("Can't find CraftEngine furniture " + name, 1);
            }
        } catch (Throwable throwable) {
            debugFailure("furniture place for " + name, throwable);
        }
    }

    public void removeFurniture(Grave grave) {
        if (grave == null) {
            return;
        }

        removeFurniture(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    public void removeFurniture(EntityData entityData) {
        if (entityData == null) {
            return;
        }

        removeFurniture(getEntityDataMap(List.of(entityData)));
    }

    public void removeFurniture(Map<EntityData, Entity> entityDataMap) {
        if (entityDataMap == null || entityDataMap.isEmpty()) {
            return;
        }

        List<EntityData> toRemove = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            Entity entity = entry.getValue();

            if (entity == null) {
                continue;
            }

            try {
                furnitureRemoveMethod.invoke(null, entity);
            } catch (Throwable throwable) {
                try {
                    entity.remove();
                } catch (Throwable ignored) {
                }

                debugFailure("furniture remove", throwable);
            }

            toRemove.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(toRemove);
    }

    public Grave getGrave(Entity entity) {
        if (entity == null) {
            return null;
        }

        Entity baseEntity = getFurnitureBaseEntity(entity);

        if (baseEntity != null) {
            Grave grave = super.getGrave(baseEntity);

            if (grave != null) {
                return grave;
            }
        }

        return super.getGrave(entity);
    }

    public Entity getFurnitureBaseEntity(Entity entity) {
        Object furniture = getFurniture(entity);
        return getFurnitureBaseEntity(furniture);
    }

    public boolean isFurnitureEntity(Entity entity) {
        return getFurniture(entity) != null;
    }

    public String getFurnitureId(Entity entity) {
        Object furniture = getFurniture(entity);

        if (furniture == null) {
            return null;
        }

        try {
            Object key = furniture.getClass().getMethod("id").invoke(furniture);
            return key != null ? key.toString() : null;
        } catch (Throwable throwable) {
            debugFailure("furniture id lookup", throwable);
            return null;
        }
    }

    public boolean hasFurniture(Grave grave) {
        if (grave == null) {
            return false;
        }

        Map<EntityData, Entity> map = getEntityDataMap(getLoadedEntityDataList(grave));

        if (map.isEmpty()) {
            return false;
        }

        for (Map.Entry<EntityData, Entity> entry : map.entrySet()) {
            EntityData data = entry.getKey();
            Entity entity = entry.getValue();

            if (data == null || entity == null) {
                continue;
            }

            if (data.getType() == EntityData.Type.CRAFTENGINE
                    && entity.isValid()
                    && !entity.isDead()
                    && isFurnitureEntity(entity)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasBlock(Grave grave) {
        if (grave == null) {
            return false;
        }

        Location location = grave.getLocationDeath();

        if (location == null || location.getWorld() == null) {
            return false;
        }

        return isCustomBlock(location);
    }

    private Object getFurniture(Entity entity) {
        if (entity == null) {
            return null;
        }

        try {
            if (Boolean.TRUE.equals(furnitureIsFurnitureMethod.invoke(null, entity))) {
                Object furniture = furnitureByMetaEntityMethod.invoke(null, entity);

                if (furniture != null) {
                    return furniture;
                }
            }

            if (Boolean.TRUE.equals(furnitureIsCollisionEntityMethod.invoke(null, entity))) {
                Object furniture = furnitureByColliderMethod.invoke(null, entity);

                if (furniture != null) {
                    return furniture;
                }
            }

            if (Boolean.TRUE.equals(furnitureIsSeatMethod.invoke(null, entity))) {
                return furnitureBySeatMethod.invoke(null, entity);
            }
        } catch (Throwable throwable) {
            debugFailure("furniture lookup", throwable);
        }

        return null;
    }

    private Entity getFurnitureBaseEntity(Object furniture) {
        if (furniture == null) {
            return null;
        }

        try {
            Object object = furniture.getClass().getMethod("bukkitEntity").invoke(furniture);

            if (object instanceof Entity entity) {
                return entity;
            }
        } catch (NoSuchMethodException ignored) {
            try {
                Object object = furniture.getClass().getMethod("getBukkitEntity").invoke(furniture);

                if (object instanceof Entity entity) {
                    return entity;
                }
            } catch (Throwable throwable) {
                debugFailure("furniture base entity lookup", throwable);
            }
        } catch (Throwable throwable) {
            debugFailure("furniture base entity lookup", throwable);
        }

        return null;
    }

    private Object toKey(String id) throws ReflectiveOperationException {
        return keyOfStringMethod.invoke(null, id);
    }

    private String getBlockStateId(Object state) {
        try {
            Object owner = state.getClass().getMethod("owner").invoke(state);
            Object key = owner.getClass().getMethod("key").invoke(owner);
            Object location = key.getClass().getMethod("location").invoke(key);
            Object string = location.getClass().getMethod("asString").invoke(location);
            return string != null ? string.toString() : null;
        } catch (Throwable ignored) {
            String value = state.toString();
            int propertiesIndex = value.indexOf('[');
            return propertiesIndex >= 0 ? value.substring(0, propertiesIndex) : value;
        }
    }

    private Method getBuildBukkitItemMethod(Class<?> definitionClass) throws NoSuchMethodException {
        try {
            return definitionClass.getMethod("buildBukkitItem");
        } catch (NoSuchMethodException ignored) {
            for (Method method : definitionClass.getMethods()) {
                if (method.getName().equals("buildBukkitItem")
                        && method.getParameterCount() == 0
                        && ItemStack.class.isAssignableFrom(method.getReturnType())) {
                    return method;
                }
            }

            throw ignored;
        }
    }

    private void debugFailure(String action, Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        plugin.debugMessage("CraftEngine " + action + " failed: " + cause.getMessage(), 2);
    }
}