package com.ranull.graves.data;

import org.bukkit.Location;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Represents data for an entity associated with a grave, including its location, UUID, and type.
 */
public class EntityData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * Returns {@code true} if this entity data is associated with the given grave UUID.
     *
     * @param graveUUID the grave UUID to compare
     * @return {@code true} if {@code graveUUID} matches this entry's grave UUID
     */
    public boolean isForGrave(UUID graveUUID) {
        return this.uuidGrave.equals(graveUUID);
    }

    /**
     * Returns {@code true} if this entity data is for the given entity UUID.
     *
     * @param entityUUID the entity UUID to compare
     * @return {@code true} if {@code entityUUID} matches this entry's entity UUID
     */
    public boolean isForEntity(UUID entityUUID) {
        return this.uuidEntity.equals(entityUUID);
    }

    /**
     * Returns {@code true} if this entity is in the same world as the provided location.
     *
     * @param other the other location to compare
     * @return {@code true} if both locations have non-null worlds and they match
     */
    public boolean isSameWorld(Location other) {
        return other != null
                && this.location.getWorld() != null
                && other.getWorld() != null
                && this.location.getWorld().equals(other.getWorld());
    }

    /**
     * Returns {@code true} if this entity location is within the given squared distance
     * of the provided location.
     *
     * <p>Uses {@link Location#distanceSquared(Location)} to avoid a sqrt.</p>
     *
     * @param other           the other location
     * @param maxDistanceSq   max allowed squared distance (e.g. {@code 16} for 4 blocks)
     * @return {@code true} if within range and in the same world
     */
    public boolean isWithinDistanceSquared(Location other, double maxDistanceSq) {
        if (!isSameWorld(other)) {
            return false;
        }
        return this.location.distanceSquared(other) <= maxDistanceSq;
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
         * Represents a mannequin entity.
         */
        MANNEQUIN,

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
         * Represents an entity from the Nexo plugin.
         */
        NEXO,

        /**
         * Represents an entity from PlayerNPC plugin.
         */
        PLAYERNPC,

        /**
         * Represents an entity from CraftEngine plugin.
         */
        CRAFTENGINE,

        /**
         * Represents a custom entry, whether that be an addon or module.
         */
        CUSTOM;

        /**
         * Returns {@code true} if this type is one of the "vanilla" grave entity types.
         *
         * @return {@code true} for {@link #HOLOGRAM}, {@link #ARMOR_STAND}, {@link #ITEM_FRAME}, {@link #MANNEQUIN}
         */
        public boolean isVanilla() {
            return this == HOLOGRAM || this == ARMOR_STAND || this == ITEM_FRAME || this == MANNEQUIN;
        }

        /**
         * Returns {@code true} if this type represents a third-party integration.
         *
         * @return {@code true} for integration-backed types
         */
        public boolean isIntegration() {
            return this == FURNITURELIB
                    || this == FURNITUREENGINE
                    || this == ITEMSADDER
                    || this == ORAXEN
                    || this == NEXO
                    || this == PLAYERNPC;
        }

        /**
         * Returns {@code true} if this type is {@link #CUSTOM}.
         *
         * @return {@code true} when {@link #CUSTOM}
         */
        public boolean isCustom() {
            return this == CUSTOM;
        }

        /**
         * Safely resolves a {@link Type} from a string.
         *
         * <p>Accepts case-insensitive values like "hologram" or "ITEMSADDER". If the input is
         * {@code null}, blank, or unrecognized, {@link Type#CUSTOM} is returned.</p>
         *
         * @param value       the input string
         * @return the resolved {@link Type} or {@link Type#CUSTOM} when invalid
         */
        public static Type fromString(String value) {
            if (value == null) {
                return CUSTOM;
            }

            String cleaned = value.trim();
            if (cleaned.isEmpty()) {
                return CUSTOM;
            }

            try {
                return Type.valueOf(cleaned.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return CUSTOM;
            }
        }
    }
}