package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.MaterialUtil;
import com.ranull.graves.util.SkinUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * An implementation of the Compatibility interface for handling block data.
 */
public final class CompatibilityBlockData implements Compatibility {

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
            String originalMaterial = block.getType().name();
            String replaceMaterial = location.getBlock().getType().name();
            String replaceData = location.getBlock().getBlockData().clone().getAsString(true);

            // Levelled
            if (block.getBlockData() instanceof Levelled) {
                Levelled leveled = (Levelled) block.getBlockData();

                if (leveled.getLevel() != 0) {
                    replaceMaterial = null;
                    replaceData = null;
                }
            }

            // Air
            if (block.getType() == Material.NETHER_PORTAL || block.getBlockData() instanceof Openable) {
                replaceMaterial = null;
                replaceData = null;
            }

            // Set type
            location.getBlock().setType(material);

            // Waterlogged
            if (block.getBlockData() instanceof Waterlogged) {
                Waterlogged waterlogged = (Waterlogged) block.getBlockData();

                waterlogged.setWaterlogged(MaterialUtil.isWater(originalMaterial));
                block.setBlockData(waterlogged);
            }

            // Update skull
            if (material == Material.PLAYER_HEAD && block.getState() instanceof Skull) {
                updateSkullBlock(block, grave, plugin);
            }

            return new BlockData(location, grave.getUUID(), replaceMaterial, replaceData);
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
    @Override
    public boolean canBuild(Player player, Location location, Graves plugin) {
        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(location.getBlock(),
                location.getBlock().getState(), location.getBlock(), player.getInventory().getItemInMainHand(),
                player, true, EquipmentSlot.HAND);

        plugin.getServer().getPluginManager().callEvent(blockPlaceEvent);

        return blockPlaceEvent.canBuild() && !blockPlaceEvent.isCancelled();
    }

    /**
     * Checks if a block has title data.
     *
     * @param block The block to check.
     * @return True if the block has title data, false otherwise.
     */
    @Override
    public boolean hasTitleData(Block block) {
        return block.getState() instanceof TileState;
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
        Rotatable skullRotate = (Rotatable) block.getBlockData();

        skullRotate.setRotation(BlockFaceUtil.getYawBlockFace(grave.getYaw()).getOppositeFace());
        skull.setBlockData(skullRotate);

        if (headType == 0) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerUUID()));
            } else if (grave.getOwnerTexture() != null) {
                SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), grave.getOwnerTexture());
            } else if (headBase64 != null && !headBase64.equals("")) {
                SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
            }
        } else if (headType == 1 && headBase64 != null && !headBase64.equals("")) {
            SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
        } else if (headType == 2 && headName != null && headName.length() <= 16) {
            skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(headName));
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
    @Override
    public ItemStack getSkullItemStack(Grave grave, Graves plugin) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

        if (itemStack.getItemMeta() != null) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(grave.getOwnerUUID());
                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                skullMeta.setOwningPlayer(offlinePlayer);
                itemStack.setItemMeta(skullMeta);
            } else {
                // TODO ENTITY
            }
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
        if (itemStack.getType() == Material.PLAYER_HEAD && itemStack.getItemMeta() != null) {
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