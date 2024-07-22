package com.ranull.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class for handling reflection operations related to Bukkit and Minecraft server classes.
 */
public final class ReflectionUtil {

    /**
     * Triggers the main hand swing animation for the specified player using reflection to access Minecraft server methods.
     *
     * @param player The player whose main hand swing animation is to be triggered.
     */
    public static void swingMainHand(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", getClass("Packet"));
            Object packetPlayOutAnimation = getClass("PacketPlayOutAnimation")
                    .getConstructor(getClass("Entity"), int.class).newInstance(entityPlayer, 0);

            sendPacket.invoke(playerConnection, packetPlayOutAnimation);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException
                 | ClassNotFoundException | InstantiationException ignored) {
        }
    }

    /**
     * Retrieves a class from the net.minecraft.server package using the server version from the Bukkit package.
     *
     * @param clazz The name of the class to retrieve.
     * @return The class object for the specified class name.
     * @throws ClassNotFoundException If the class cannot be found.
     */
    public static Class<?> getClass(String clazz) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName()
                .replace(".", ",").split(",")[3] + "." + clazz);
    }
}