package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Serializes and deserializes cached values.
 */
public class CacheCodec {
    private final Graves plugin;
    private final boolean debug;

    public boolean isDebugEnabled() {
        return debug;
    }

    /**
     * Creates a cache codec.
     */
    public CacheCodec(Graves plugin) {
        this.plugin = plugin;
        this.debug = DebugCacheCodec.enabled(plugin);
    }

    /**
     * Serializes a value into bytes.
     */
    public byte[] encode(Object value) {
        if (value == null) return null;
        if (debug)
            return DebugCacheCodec.write(value).getBytes(StandardCharsets.UTF_8);

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                if (value instanceof Grave grave) {
                    out.writeBoolean(true);
                    out.writeObject(grave);

                    Inventory inventory = grave.getInventory();
                    if (inventory == null) {
                        out.writeInt(-1);
                    } else {
                        ItemStack[] contents = inventory.getContents();
                        out.writeInt(contents.length);
                        for (ItemStack item : contents) {
                            out.writeObject(item);
                        }
                    }
                } else {
                    out.writeBoolean(false);
                    out.writeObject(value);
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new CacheException("Failed to serialize cache value " + value.getClass().getName(), e);
        }
    }

    /**
     * Deserializes bytes into a value.
     */
    public Object decode(byte[] data) {
        if (data == null) return null;
        if (DebugCacheCodec.isText(data)) {
            Object value = DebugCacheCodec.read(new String(data, StandardCharsets.UTF_8));
            if (value instanceof Grave grave && plugin.getCacheManager() != null) {
                Grave active = plugin.getCacheManager().getViewedGrave(grave.getUUID());

                if (active != null)
                    return active;
            }
            return value;
        }
        try (BukkitObjectInputStream in =
                     new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            boolean graveEnvelope = in.readBoolean();
            Object value = in.readObject();

            if (graveEnvelope && value instanceof Grave grave) {
                Grave active = plugin.getCacheManager().getViewedGrave(grave.getUUID());
                if (active != null)
                    return active;

                int size = in.readInt();
                if (size >= 0) {
                    int inventorySize = Math.max(9, ((size + 8) / 9) * 9);
                    Inventory inventory = Bukkit.createInventory(grave, inventorySize);
                    ItemStack[] contents = new ItemStack[inventorySize];
                    for (int i = 0; i < size; i++) {
                        ItemStack item = (ItemStack) in.readObject();
                        if (i < contents.length) contents[i] = item;
                    }
                    inventory.setContents(contents);
                    grave.setInventory(inventory);
                }
            }
            return value;
        } catch (IOException | ClassNotFoundException e) {
            throw new CacheException("Failed to deserialize cache value", e);
        }
    }

    /**
     * Represents a cache serialization or deserialization error.
     */
    public static final class CacheException extends RuntimeException {

        /**
         * Creates a cache exception.
         */
        public CacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}