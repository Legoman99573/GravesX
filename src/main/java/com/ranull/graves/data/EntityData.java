package com.ranull.graves.data;

import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents data for an entity associated with a grave, including its location, UUID, and type.
 */
public class EntityData implements Serializable {
    /**
     * The location of the grave.
     * <p>
     * This is the {@link Location} in the game world where the grave is situated.
     * </p>
     */
    private final Location location;

    /**
     * The unique identifier of the entity associated with the grave.
     * <p>
     * This {@link UUID} identifies the specific entity that is linked to this grave.
     * </p>
     */
    private final UUID uuidEntity;

    /**
     * The unique identifier of the grave.
     * <p>
     * This {@link UUID} uniquely identifies the grave itself.
     * </p>
     */
    private final UUID uuidGrave;

    /**
     * The type of entity associated with the grave.
     * <p>
     * This {@link Type} enum value indicates the type of entity that is related to the grave.
     * </p>
     */
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
        /**
         * Represents a hologram entity.
         */
        HOLOGRAM,

        /**
         * Represents an armor stand entity.
         */
        ARMOR_STAND,

        /**
         * Represents an item frame entity.
         */
        ITEM_FRAME,

        /**
         * Represents an entity from the FurnitureLib plugin.
         */
        FURNITURELIB,

        /**
         * Represents an entity from the FurnitureEngine plugin.
         */
        FURNITUREENGINE,

        /**
         * Represents an entity from the ItemsAdder plugin.
         */
        ITEMSADDER,

        /**
         * Represents an entity from the Oraxen plugin.
         */
        ORAXEN,

        /**
         * Represents an entity from PlayerNPC plugin.
         */
        PLAYERNPC,

        /**
         * Represents an entity from CitizensNPC plugin.
         */
        CITIZENSNPC
    }
}
