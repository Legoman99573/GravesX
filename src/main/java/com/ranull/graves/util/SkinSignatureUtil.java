package com.ranull.graves.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

public final class SkinSignatureUtil {
    private static String GAMEPROFILE_METHOD;

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

                    } catch(NoSuchMethodError blah) {
                        return !propertyCollection.isEmpty()
                                ? propertyCollection.stream().findFirst().get().getSignature() : null;
                    }
                }
            }
        }

        return null;
    }

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
