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

    private Base64Util() {}

    /**
     * Serializes an object to a Base64 encoded string.
     *
     * @param object The object to serialize.
     * @return The Base64 encoded string representing the serialized object, or null if an error occurs.
     */
    public static String objectToBase64(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {

            boos.writeObject(object);
            boos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ignored) {

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
        byte[] data;
        try {
            data = Base64.getDecoder().decode(string);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        try (BukkitObjectInputStream bois = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            return bois.readObject();
        } catch (IOException | ClassNotFoundException ignored) {

        }
        return null;
    }
}