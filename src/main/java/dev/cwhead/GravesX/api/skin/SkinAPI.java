package dev.cwhead.GravesX.api.skin;

import com.mojang.authlib.GameProfile;
import dev.cwhead.GravesX.util.SkinTextureUtil_post_1_21_9;
import me.jay.GravesX.util.SkinSignatureUtil;
import me.jay.GravesX.util.SkinTextureUtil;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Skin/texture/profile helper API.
 */
public final class SkinAPI {

    /** Utility class; prevent instantiation. */
    public SkinAPI() {}

    /**
     * Gets the skin signature of the specified entity if it is a player.
     *
     * @param entity The entity whose skin signature is to be retrieved.
     * @return The skin signature of the player, or null if the entity is not a player or the signature could not be retrieved.
     */
    public static String getSkinSignature(@NotNull Entity entity) {
        return SkinSignatureUtil.getSignature(entity);
    }

    /**
     * Sets the texture of a Skull block.
     *
     * @param skull  The Skull block.
     * @param name   The name associated with the texture.
     * @param base64 The Base64 encoded texture.
     */
    public static void setSkullTexture(@NotNull Skull skull, @NotNull String name, @NotNull String base64) {
        try {
            SkinTextureUtil.setSkullBlockTexture(skull, name, base64);
        } catch (Exception e) {
            SkinTextureUtil_post_1_21_9.setSkullBlockTexture(skull, name, base64);
        }
    }

    /**
     * Sets the texture of a Skull item stack.
     *
     * @param skullMeta The SkullMeta item meta.
     * @param name      The name associated with the texture.
     * @param base64    The Base64 encoded texture.
     */
    public static void setSkullTexture(@NotNull SkullMeta skullMeta, @NotNull String name, @NotNull String base64) {
        try {
            SkinTextureUtil.setSkullBlockTexture(skullMeta, name, base64);
        } catch (Exception e) {
            SkinTextureUtil_post_1_21_9.setSkullBlockTexture(skullMeta, name, base64);
        }
    }

    /**
     * Retrieves the texture of the specified entity.
     *
     * @param entity The entity from which to get the texture.
     * @return The Base64 encoded texture string, or null if not found.
     */
    public static @Nullable String getTexture(@NotNull Entity entity) {
        try {
            return SkinTextureUtil.getTexture(entity);
        } catch (Exception e) {
            return SkinTextureUtil_post_1_21_9.getTexture(entity);
        }
    }

    /**
     * Retrieves the GameProfile of the specified player.
     *
     * @param player The player from which to get the GameProfile.
     * @return The GameProfile of the player, or null if not found.
     */
    public static @Nullable GameProfile getPlayerGameProfile(@NotNull Player player) {
        return SkinTextureUtil.getPlayerGameProfile(player);
    }

    /**
     * Retrieves the PlauerProfile of the specified player.
     *
     * @param player The player from which to get the GameProfile.
     * @return The PlayerProfile of the player, or null if not found.
     */
    public static @Nullable PlayerProfile getPlayerProfile(@NotNull Player player) {
        return SkinTextureUtil_post_1_21_9.getPlayerProfile(player);
    }
}