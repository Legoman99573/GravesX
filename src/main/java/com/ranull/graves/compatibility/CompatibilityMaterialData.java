package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.SkinUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Openable;

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
            if (material.name().equals("SKULL") && block.getState() instanceof Skull) {
                updateSkullBlock(block, grave, plugin);
            }

            return new BlockData(location, grave.getUUID(), replaceMaterial, null);
        }

        return new BlockData(location, grave.getUUID(), null, null);
    }

    /**
     * Checks if a player can build at a given location.
     *
     * @param player   The player to check.
     * @param location The location to check.
     * @param plugin   The Graves plugin instance.
     * @return True if the player can build at the location, false otherwise.
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean canBuild(Player player, Location location, Graves plugin) {
        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(location.getBlock(),
                location.getBlock().getState(), location.getBlock(), player.getItemInHand(),
                player, true);

        plugin.getServer().getPluginManager().callEvent(blockPlaceEvent);

        return blockPlaceEvent.canBuild() && !blockPlaceEvent.isCancelled();
    }

    /**
     * Checks if a block has title data.
     *
     * @param block The block to check.
     * @return True if the block has title data, false otherwise.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean hasTitleData(Block block) {
        return block.getState() instanceof BlockState;
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

        if (headType == 0) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                skull.setOwner(grave.getOwnerName());
            } else {
                if (!plugin.getVersionManager().is_v1_7()) {
                    SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
                } else {
                    skull.setOwner(grave.getOwnerName());
                }
            }
        } else if (headType == 1 && headBase64 != null && !headBase64.equals("")) {
            if (!plugin.getVersionManager().is_v1_7()) {
                SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
            } else {
                skull.setOwner(grave.getOwnerName());
            }
        } else if (headType == 2 && headName != null && headName.length() <= 16) {
            skull.setOwner(headName);
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
        Material material = Material.matchMaterial("SKULL_ITEM");

        if (material != null) {
            ItemStack itemStack = new ItemStack(material, 1, (short) 3);

            if (itemStack.getItemMeta() != null) {
                if (grave.getOwnerType() == EntityType.PLAYER) {
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                    skullMeta.setOwner(grave.getOwnerName());
                    itemStack.setItemMeta(skullMeta);
                } else {
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                    String texture = grave.getOwnerTexture();
                    if (texture != null && !texture.isEmpty()) {
                        SkinUtil.setSkullBlockTexture(skullMeta, grave.getOwnerName(), texture);
                    }
                }
            }

            return itemStack;
        }

        return null;
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

                if (gameProfile != null && gameProfile.getProperties().containsKey("textures")) {
                    Collection<Property> propertyCollection = gameProfile.getProperties().get("textures");

                    if (!propertyCollection.isEmpty()) {
                        try {
                            return propertyCollection.stream().findFirst().get().value();
                        } catch (NoSuchMethodError meh) {
                            return propertyCollection.stream().findFirst().get().getValue();
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }
}