package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.MaterialUtil;
import com.ranull.graves.util.SkinTextureUtil;
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
            return handleBlockPlacement(location, material, grave, plugin);
        }

        return new BlockData(location, grave.getUUID(), null, null);
    }

    /**
     * Handles the block placement logic.
     *
     * @param location The location where the block data should be set.
     * @param material The material of the block to set.
     * @param grave    The grave associated with the block.
     * @param plugin   The Graves plugin instance.
     * @return The BlockData representing the set block data.
     */
    private BlockData handleBlockPlacement(Location location, Material material, Grave grave, Graves plugin) {
        Block block = location.getBlock();
        String originalMaterial = block.getType().name();
        String replaceMaterial = location.getBlock().getType().name();
        String replaceData = location.getBlock().getBlockData().clone().getAsString(true);

        if (isLevelledBlock(block)) {
            replaceMaterial = null;
            replaceData = null;
        }

        if (isSpecialBlock(block)) {
            replaceMaterial = null;
            replaceData = null;
        }

        location.getBlock().setType(material);

        if (block.getBlockData() instanceof Waterlogged) {
            setWaterlogged(block, originalMaterial);
        }

        if (material == Material.PLAYER_HEAD && block.getState() instanceof Skull) {
            updateSkullBlock(block, grave, plugin);
        }

        return new BlockData(location, grave.getUUID(), replaceMaterial, replaceData);
    }

    /**
     * Checks if the block is a Levelled block.
     *
     * @param block The block to check.
     * @return True if the block is a Levelled block, false otherwise.
     */
    private boolean isLevelledBlock(Block block) {
        return block.getBlockData() instanceof Levelled && ((Levelled) block.getBlockData()).getLevel() != 0;
    }

    /**
     * Checks if the block is a special block (Nether Portal or Openable).
     *
     * @param block The block to check.
     * @return True if the block is a special block, false otherwise.
     */
    private boolean isSpecialBlock(Block block) {
        return block.getType() == Material.NETHER_PORTAL || block.getBlockData() instanceof Openable;
    }

    /**
     * Sets the waterlogged state of the block.
     *
     * @param block            The block to set the waterlogged state for.
     * @param originalMaterial The original material of the block.
     */
    private void setWaterlogged(Block block, String originalMaterial) {
        Waterlogged waterlogged = (Waterlogged) block.getBlockData();
        waterlogged.setWaterlogged(MaterialUtil.isWater(originalMaterial));
        block.setBlockData(waterlogged);
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

        applySkullData(skull, grave, plugin, headType, headBase64, headName);
    }

    /**
     * Applies the skull data to the skull block.
     *
     * @param skull     The skull block.
     * @param grave     The grave associated with the skull.
     * @param plugin    The Graves plugin instance.
     * @param headType  The type of head.
     * @param headBase64 The base64 encoded texture of the head.
     * @param headName  The name of the head.
     */
    private void applySkullData(Skull skull, Grave grave, Graves plugin, int headType, String headBase64, String headName) {
        if (headType == 0) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerUUID()));
            } else if (grave.getOwnerTexture() != null) {
                SkinTextureUtil.setSkullBlockTexture(skull, grave.getOwnerName(), grave.getOwnerTexture());
            } else if (headBase64 != null && !headBase64.equals("")) {
                SkinTextureUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
            }
        } else if (headType == 1 && headBase64 != null && !headBase64.equals("")) {
            SkinTextureUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
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
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        if (skullMeta != null) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(grave.getOwnerUUID());
                skullMeta.setOwningPlayer(offlinePlayer);
            } else if (grave.getOwnerType() != null) {
                String entityTexture = getEntityTexture(grave.getOwnerType());
                if (entityTexture != null) {
                    SkinTextureUtil.setSkullBlockTexture(skullMeta, grave.getOwnerName(), entityTexture);
                }
            }

            itemStack.setItemMeta(skullMeta);
        }

        return itemStack;
    }

    /**
     * Gets the texture for a given entity type.
     *
     * @param entityType The type of the entity.
     * @return The texture of the entity as a string, or null if no texture is available.
     */
    private String getEntityTexture(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
                return "base64_texture_for_zombie";
            case SKELETON:
                return "base64_texture_for_skeleton";
            default:
                return null;
        }
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

            return extractSkullTexture(skullMeta);
        }

        return null;
    }

    /**
     * Extracts the texture of the skull from the SkullMeta.
     *
     * @param skullMeta The SkullMeta to extract the texture from.
     * @return The texture of the skull as a string.
     */
    private String extractSkullTexture(SkullMeta skullMeta) {
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");

            profileField.setAccessible(true);

            GameProfile gameProfile = (GameProfile) profileField.get(skullMeta);

            if (gameProfile != null && gameProfile.getProperties().containsKey("textures")) {
                Collection<Property> propertyCollection = gameProfile.getProperties().get("textures");

                if (!propertyCollection.isEmpty()) {
                    return propertyCollection.stream().findFirst().get().getValue();
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }

        return null;
    }
}