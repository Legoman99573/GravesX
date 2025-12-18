package dev.cwhead.GravesX.util;

import com.ranull.graves.util.Base64Util;
import com.ranull.skulltextureapi.SkullTextureAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.util.UUID;

/**
 * Utility class for handling player skins and textures.
 */
public class SkinTextureUtil_post_1_21_9 {

    private SkinTextureUtil_post_1_21_9() {}

    /** Create a PlayerProfile with a custom Base64 texture */
    private static PlayerProfile createProfileWithTexture(String name, String base64) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), name);
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(Base64Util.extractSkinURL(base64));
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Sets the texture of a Skull block.
     *
     * @param skull  The Skull block.
     * @param name   The name associated with the texture.
     * @param base64 The Base64 encoded texture.
     */
    public static void setSkullBlockTexture(Skull skull, String name, String base64) {
        try {
            PlayerProfile profile = createProfileWithTexture(name, base64);
            skull.setOwnerProfile(profile);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("Failed to set Skull texture: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Sets the texture of a Skull item stack.
     *
     * @param skullMeta The SkullMeta item meta.
     * @param name      The name associated with the texture.
     * @param base64    The Base64 encoded texture.
     */
    public static void setSkullBlockTexture(SkullMeta skullMeta, String name, String base64) {
        try {
            PlayerProfile profile = createProfileWithTexture(name, base64);
            skullMeta.setOwnerProfile(profile);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("Failed to set SkullMeta texture: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Retrieves the texture of an Entity.
     *
     * @param entity The entity from which to get the texture.
     * @return The Base64 encoded texture string, or null if not found.
     */
    public static String getTexture(Entity entity) {
        if (entity instanceof Player player) {
            PlayerProfile profile = player.getPlayerProfile();
            if (profile.getTextures().getSkin() != null) {
                return profile.getTextures().getSkin().toString();
            }
        } else {
            Plugin api = Bukkit.getPluginManager().getPlugin("SkullTextureAPI");
            if (api instanceof SkullTextureAPI && api.isEnabled()) {
                try {
                    String base64 = SkullTextureAPI.getTexture(entity);
                    if (base64 != null && !base64.isEmpty()) return base64;
                } catch (NoSuchMethodError ignored) {}
            }
        }
        return null;
    }

    /**
     * Retrieves the GameProfile of a Player.
     *
     * @param player The player from which to get the GameProfile.
     * @return The GameProfile of the player, or null if not found.
     */
    public static PlayerProfile getPlayerProfile(Player player) {
        return player.getPlayerProfile();
    }


    /** Create a PlayerProfile with a custom Base64 texture (UUID + name) */
    public static PlayerProfile createProfileWithTexture(UUID uuid, String name, String base64) {
        PlayerProfile profile = Bukkit.createPlayerProfile(uuid, name);
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(Base64Util.extractSkinURL(base64));
        profile.setTextures(textures);
        return profile;
    }
}
