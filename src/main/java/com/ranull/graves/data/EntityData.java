package com.ranull.graves.data;

import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents data for an entity associated with a grave, including its location, UUID, and type.
 */
public class EntityData implements Serializable {
    private final Location location;
    private final UUID uuidEntity;
    private final UUID uuidGrave;
    private final Type type;

    /**
     * Constructs a new EntityData instance.
     *
     * @param location   The location of the entity.
     * @param uuidEntity The UUID of the entity.
     * @param uuidGrave  The UUID of the associated grave.
     * @param type       The type of the entity.
     */
    public EntityData(Location location, UUID uuidEntity, UUID uuidGrave, Type type) {
        this.location = location;
        this.uuidEntity = uuidEntity;
        this.uuidGrave = uuidGrave;
        this.type = type;
    }

    /**
     * Gets the location of the entity.
     *
     * @return The location of the entity.
     */
    public Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the UUID of the entity.
     *
     * @return The UUID of the entity.
     */
    public UUID getUUIDEntity() {
        return uuidEntity;
    }

    /**
     * Gets the UUID of the associated grave.
     *
     * @return The UUID of the associated grave.
     */
    public UUID getUUIDGrave() {
        return uuidGrave;
    }

    /**
     * Gets the type of the entity.
     *
     * @return The type of the entity.
     */
    public Type getType() {
        return type;
    }

    /**
     * Enum representing the different types of entities that can be associated with a grave.
     */
    public enum Type {
        HOLOGRAM,
        ARMOR_STAND,
        ITEM_FRAME,
        FURNITURELIB,
        FURNITUREENGINE,
        ITEMSADDER,
        ORAXEN,
        PLAYERNPC
    }
}
