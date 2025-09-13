package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import me.jay.GravesX.util.SkinTextureUtil;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Openable;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * An implementation of the Compatibility interface for handling material data.
 */
public final class CompatibilityMaterialData implements Compatibility {

    /**
     * Sets the block data for a given location and material, associating it with a grave.
     *
     * @param location The location where the block data should be set.
     * @param material The material of the block to set.
     * @param grave    The grave associated with the block.
     * @param plugin   The Graves plugin instance.
     * @return The BlockData representing the set block data.
     */
    @Override
    public BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin) {
        if (material != null) {
            Block block = location.getBlock();
            String replaceMaterial = location.getBlock().getType().name();

            // Air
            if (block.getType().name().equals("NETHER_PORTAL") || block.getState().getData() instanceof Openable) {
                replaceMaterial = null;
            }

            // Set type
            location.getBlock().setType(material);

            // Update skull
            if ((material.name().equals("SKULL") || material.name().equals("PLAYER_HEAD")) && block.getState() instanceof Skull) {
                updateSkullBlock(block, grave, plugin);
            }

            return new BlockData(location, grave.getUUID(), replaceMaterial, null);
        }

        return new BlockData(location, grave.getUUID(), null, null);
    }

    /**
     * Checks if a player can build at a given location by simulating a block placement.
     * <p>
     * This method supports both modern (1.9+) and legacy (1.8 and below) servers.
     * On modern servers, it attempts placement with both main and off hand items.
     * On legacy servers, it falls back to the deprecated {@link BlockPlaceEvent} constructor.
     *
     * @param player   The player attempting to build.
     * @param location The location where the block would be placed.
     * @param plugin   The instance of the Graves plugin, used for version checking and event dispatch.
     * @return {@code true} if the player is allowed to build at the specified location; {@code false} otherwise.
     */
    @Override
    public boolean canBuild(Player player, Location location, Graves plugin) {
        Plugin landProtectionAddonPlugin = plugin.getServer().getPluginManager().getPlugin("GravesXAddon-LandProtection");
        if (landProtectionAddonPlugin != null && landProtectionAddonPlugin.isEnabled()) return true;
        Block placedBlock = location.getBlock();
        BlockState replacedBlockState = placedBlock.getState();
        Block placedAgainst = null;

        for (BlockFace face : BlockFace.values()) {
            Block relative = placedBlock.getRelative(face);
            if (relative.getType().isSolid()) {
                placedAgainst = relative;
                break;
            }
        }

        if (placedAgainst == null) {
            placedAgainst = placedBlock.getRelative(BlockFace.DOWN);
        }

        if (plugin.getVersionManager().is_v1_8() || plugin.getVersionManager().is_v1_7()) {
            @SuppressWarnings("deprecation")
            BlockPlaceEvent legacyEvent = new BlockPlaceEvent(
                    placedBlock,
                    replacedBlockState,
                    placedAgainst,
                    player.getItemInHand(),
                    player,
                    true
            );
            plugin.getServer().getPluginManager().callEvent(legacyEvent);
            return legacyEvent.canBuild() && !legacyEvent.isCancelled();
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType().isAir()) continue;

                BlockPlaceEvent event = new BlockPlaceEvent(
                        placedBlock,
                        replacedBlockState,
                        placedAgainst,
                        item,
                        player,
                        true,
                        slot
                );

                plugin.getServer().getPluginManager().callEvent(event);

                if (event.canBuild() && !event.isCancelled()) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T getEnumConstant(Class<?> enumClass, String name) {
        return (T) Enum.valueOf((Class<T>) enumClass, name);
    }

    /**
     * Checks if a block has title data (e.g., a name or custom title).
     *
     * Supports signs (both front and back sides for 1.20+), skulls, and nameable containers.
     *
     * @param block The block to check.
     * @return True if the block has title data, false otherwise.
     */
    @Override
    public boolean hasTitleData(Block block) {
        BlockState state = block.getState();

        if (state instanceof Sign) {
            Sign sign = (Sign) state;

            try {
                Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");
                Object frontSide = sign.getClass().getMethod("getSide", sideClass)
                        .invoke(sign, getEnumConstant(sideClass, "FRONT"));
                Object backSide = sign.getClass().getMethod("getSide", sideClass)
                        .invoke(sign, getEnumConstant(sideClass, "BACK"));

                for (int i = 0; i < 4; i++) {
                    String frontLine = (String) frontSide.getClass().getMethod("getLine", int.class).invoke(frontSide, i);
                    if (frontLine != null && !frontLine.trim().isEmpty()) {
                        return true;
                    }
                    String backLine = (String) backSide.getClass().getMethod("getLine", int.class).invoke(backSide, i);
                    if (backLine != null && !backLine.trim().isEmpty()) {
                        return true;
                    }
                }
            } catch (ReflectiveOperationException e) {
                @SuppressWarnings("deprecation")
                String[] lines = sign.getLines();
                for (String line : lines) {
                    if (line != null && !line.trim().isEmpty()) {
                        return true;
                    }
                }
            }

        } else if (state instanceof Skull) {
            Skull skull = (Skull) state;
            return skull.hasOwner();

        } else if (state instanceof Nameable) {
            Nameable nameable = (Nameable) state;
            String name = nameable.getCustomName();
            return name != null && !name.trim().isEmpty();
        }

        return false;
    }

    /**
     * Updates the skull block with the owner or texture data.
     *
     * @param block  The skull block to update.
     * @param grave  The grave associated with the skull.
     * @param plugin The Graves plugin instance.
     */
    @SuppressWarnings("deprecation")
    private void updateSkullBlock(Block block, Grave grave, Graves plugin) {
        int headType = plugin.getConfig("block.head.type", grave).getInt("block.head.type");
        String headBase64 = plugin.getConfig("block.head.base64", grave).getString("block.head.base64");
        String headName = plugin.getConfig("block.head.name", grave).getString("block.head.name");
        Skull skull = (Skull) block.getState();

        skull.setSkullType(SkullType.PLAYER);
        skull.setRotation(BlockFaceUtil.getYawBlockFace(grave.getYaw()).getOppositeFace());

        boolean useFallback = plugin.getVersionManager().is_v1_7();

        try {
            if (headType == 0) {
                if (grave.getOwnerType() == EntityType.PLAYER) {
                    if (!useFallback && headBase64 != null && !headBase64.isEmpty()) {
                        SkinTextureUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
                    } else {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(grave.getOwnerName());
                        try {
                            skull.setOwningPlayer(player);
                        } catch (NoSuchMethodError | NoClassDefFoundError e) {
                            skull.setOwner(grave.getOwnerName());
                        }
                    }
                } else {
                    try {
                        skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerName()));
                    } catch (NoSuchMethodError | NoClassDefFoundError e) {
                        skull.setOwner(grave.getOwnerName());
                    }
                }

            } else if (headType == 1 && headBase64 != null && !headBase64.isEmpty()) {
                if (!useFallback) {
                    SkinTextureUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
                } else {
                    try {
                        skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerName()));
                    } catch (NoSuchMethodError | NoClassDefFoundError e) {
                        skull.setOwner(grave.getOwnerName());
                    }
                }

            } else if (headType == 2 && headName != null && headName.length() <= 16) {
                try {
                    skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(headName));
                } catch (NoSuchMethodError | NoClassDefFoundError e) {
                    skull.setOwner(headName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to use new skull method; falling back. Reason: " + e.getMessage());
            skull.setOwner(grave.getOwnerName());
        }

        skull.update();
    }

    /**
     * Gets the skull item stack for a given grave.
     *
     * @param grave  The grave associated with the skull.
     * @param plugin The Graves plugin instance.
     * @return The ItemStack representing the skull.
     */
    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getSkullItemStack(Grave grave, Graves plugin) {
        Material material;

        material = Material.matchMaterial("PLAYER_HEAD");

        ItemStack itemStack;

        if (material != null) {
            itemStack = new ItemStack(material, 1);
        } else {
            material = Material.matchMaterial("SKULL_ITEM");
            if (material == null) {
                return null;
            }
            itemStack = new ItemStack(material, 1, (short) 3);
        }

        if (itemStack.getItemMeta() instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (grave.getOwnerType() == EntityType.PLAYER) {
                try {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(grave.getOwnerName());
                    skullMeta.setOwningPlayer(offlinePlayer);
                } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
                    skullMeta.setOwner(grave.getOwnerName());
                }

            } else {
                String texture = grave.getOwnerTexture();
                if (texture != null && !texture.isEmpty()) {
                    SkinTextureUtil.setSkullBlockTexture(skullMeta, grave.getOwnerName(), texture);
                }
            }
            itemStack.setItemMeta(skullMeta);
        }

        return itemStack;
    }

    /**
     * Gets the texture of a skull item stack.
     *
     * @param itemStack The item stack representing the skull.
     * @return The texture of the skull as a string.
     */
    @Override
    public String getSkullTexture(ItemStack itemStack) {
        Material material = Material.matchMaterial("SKULL_ITEM");

        if (material != null && itemStack.getType() == material && itemStack.getItemMeta() != null) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

            try {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");

                profileField.setAccessible(true);

                GameProfile gameProfile = (GameProfile) profileField.get(skullMeta);

                if (gameProfile != null && getTexturesKey(gameProfile)) {
                    try {
                        Collection<Property> propertyCollection = gameProfile.properties().get("textures");

                        if (!propertyCollection.isEmpty()) {
                            try {
                                return propertyCollection.stream().findFirst().get().value();
                            } catch (NoSuchMethodError blah) {
                                return propertyCollection.stream().findFirst().get().getValue();
                            }

                        }
                    } catch (NoSuchMethodError bleh) {
                        Collection<Property> propertyCollection = gameProfile.getProperties().get("textures");

                        if (!propertyCollection.isEmpty()) {
                            try {
                                return propertyCollection.stream().findFirst().get().value();
                            } catch (NoSuchMethodError blah) {
                                return propertyCollection.stream().findFirst().get().getValue();
                            }

                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Returns whether the profile has a "textures" property
     * (works with both {@code properties()} and {@code getProperties()}).
     *
     * @param gameProfile profile to check
     * @return true if "textures" exists; false otherwise. Returns error if both methods don't work.
     */
    private boolean getTexturesKey(GameProfile gameProfile) {
        try {
            return gameProfile.properties().containsKey("textures");
        } catch (NoSuchMethodError blah) {
            try {
                return gameProfile.getProperties().containsKey("textures");
            } catch (Exception ex) {
                Bukkit.getLogger().severe("Failed to get textures key. Cause: " + ex.getCause());
                ex.printStackTrace();
                return false;
            }
        }
    }
}