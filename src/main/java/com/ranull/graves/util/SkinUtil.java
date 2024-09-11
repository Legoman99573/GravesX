package com.ranull.graves.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.ranull.skulltextureapi.SkullTextureAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * Utility class for handling player skins and textures.
 */
public final class SkinUtil {
    private static String GAMEPROFILE_METHOD;

    /**
     * Sets the texture of a Skull block.
     *
     * @param skull  The Skull block.
     * @param name   The name associated with the texture.
     * @param base64 The Base64 encoded texture.
     */
    public static void setSkullBlockTexture(Skull skull, String name, String base64) {
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        gameProfile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skull, gameProfile);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
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
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        gameProfile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, gameProfile);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Retrieves the texture of an Entity.
     *
     * @param entity The entity from which to get the texture.
     * @return The Base64 encoded texture string, or null if not found.
     */
    public static String getTexture(Entity entity) {
        if (entity instanceof Player) {
            GameProfile gameProfile = getPlayerGameProfile((Player) entity);

            if (gameProfile != null) {
                PropertyMap propertyMap = gameProfile.getProperties();

                if (propertyMap.containsKey("textures")) {
                    Collection<Property> propertyCollection = propertyMap.get("textures");
                    try {
                        return !propertyCollection.isEmpty()
                                ? propertyCollection.stream().findFirst().get().value() : null;
                    } catch (NoSuchMethodError blah) {
                        return !propertyCollection.isEmpty()
                                ? propertyCollection.stream().findFirst().get().getValue() : null;
                    }
                }
            }
        } else {
            Plugin skullTextureAPIPlugin = Bukkit.getServer().getPluginManager().getPlugin("SkullTextureAPI");

            if (skullTextureAPIPlugin != null && skullTextureAPIPlugin.isEnabled()
                    && skullTextureAPIPlugin instanceof SkullTextureAPI) {
                try {
                    String base64 = SkullTextureAPI.getTexture(entity);

                    if (base64 != null && !base64.equals("")) {
                        return base64;
                    }
                } catch (NoSuchMethodError ignored) {
                }
            }
        }

        return null;
    }

    /**
     * Retrieves the texture signature of an Entity.
     *
     * @param entity The entity from which to get the texture signature.
     * @return The texture signature string, or null if not found.
     */
    public static String getSignature(Entity entity) {
        if (entity instanceof Player) {
            GameProfile gameProfile = getPlayerGameProfile((Player) entity);

            if (gameProfile != null) {
                PropertyMap propertyMap = gameProfile.getProperties();

                if (propertyMap.containsKey("textures")) {
                    Collection<Property> propertyCollection = propertyMap.get("textures");

                    try {
                        return !propertyCollection.isEmpty()
                                ? propertyCollection.stream().findFirst().get().signature() : null;
                    } catch (NoSuchMethodError blah) {
                        return !propertyCollection.isEmpty()
                                ? propertyCollection.stream().findFirst().get().getSignature() : null;
                    }
                }
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
    public static GameProfile getPlayerGameProfile(Player player) {
        try {
            Object playerObject = player.getClass().getMethod("getHandle").invoke(player);

            if (GAMEPROFILE_METHOD == null) {
                findGameProfileMethod(playerObject);
            }

            if (GAMEPROFILE_METHOD != null && !GAMEPROFILE_METHOD.equals("")) {
                Method gameProfile = playerObject.getClass().getMethod(GAMEPROFILE_METHOD);
                gameProfile.setAccessible(true);
                return (GameProfile) gameProfile.invoke(playerObject);
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ignored) {
        }

        return null;
    }

    /**
     * Finds and sets the method name for retrieving a GameProfile.
     *
     * @param playerObject The player object from which to find the method.
     */
    private static void findGameProfileMethod(Object playerObject) {
        for (Method method : playerObject.getClass().getMethods()) {
            if (method.getReturnType().getName().endsWith("GameProfile")) {
                GAMEPROFILE_METHOD = method.getName();
                return;
            }
        }

        GAMEPROFILE_METHOD = "";
    }
}