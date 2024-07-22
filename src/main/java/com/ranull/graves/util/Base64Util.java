package com.ranull.graves.util;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for serializing and deserializing objects to and from Base64 strings.
 */
public final class Base64Util {

    /**
     * Serializes an object to a Base64 encoded string.
     *
     * @param object The object to serialize.
     * @return The Base64 encoded string representing the serialized object, or null if an error occurs.
     */
    public static String objectToBase64(Object object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream);

            bukkitObjectOutputStream.writeObject(object);
            bukkitObjectOutputStream.close();

            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException ignored) {
            // Log the exception if needed
        }

        return null;
    }

    /**
     * Deserializes a Base64 encoded string back to an object.
     *
     * @param string The Base64 encoded string to deserialize.
     * @return The deserialized object, or null if an error occurs.
     */
    public static Object base64ToObject(String string) {
        try {
            return new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(string)))
                    .readObject();
        } catch (IOException | ClassNotFoundException ignored) {
            // Log the exception if needed
        }

        return null;
    }
}