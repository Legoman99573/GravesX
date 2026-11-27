package dev.cwhead.GravesX.event.graveevent;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXMethodNotSupportedException;
import dev.cwhead.GravesX.exception.GravesXNullPointerException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity-based grave event.
 * <p>
 * This subclass provides accessors for entity-related details (entity, living entity,
 * killer/target, etc.).
 * </p>
 */
public class GraveEntityEvent extends GraveEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The entity associated with the event.
     * <p>
     * This {@link Entity} represents the entity that is involved in the event.
     * </p>
     */
    private final @NotNull Entity entity;

    /**
     * The living entity involved in the event.
     * <p>
     * This {@link LivingEntity} represents the living entity that is part of the event.
     * </p>
     */
    private final @Nullable LivingEntity livingEntity;

    /**
     * The target living entity of the event.
     * <p>
     * This {@link LivingEntity} represents the living entity that is the target or affected by the event.
     * </p>
     */
    private final @Nullable LivingEntity targetEntity;

    /**
     * Constructs a new {@code GraveEntityEvent}.
     *
     * @param grave        The grave associated with the event.
     * @param entity       The entity involved in the event (non-null).
     * @param location     The location of the event.
     * @param blockType    The type of block involved in the event, if any.
     * @param block        The block involved in the event, if any.
     * @param livingEntity The living entity associated with the event, if any.
     * @param targetEntity The entity targeted by the event, if any.
     */
    public GraveEntityEvent(
            @NotNull Grave grave,
            @NotNull Entity entity,
            @Nullable Location location,
            @Nullable BlockData.BlockType blockType,
            @Nullable Block block,
            @Nullable LivingEntity livingEntity,
            @Nullable LivingEntity targetEntity
    ) {
        super(grave, location, blockType, block);
        this.entity = Objects.requireNonNull(entity, "entity");
        this.livingEntity = livingEntity;
        this.targetEntity = targetEntity;
    }

    /**
     * Gets the entity involved in the event.
     *
     * @return The entity involved in the event.
     */
    public @NotNull Entity getEntity() {
        return entity;
    }

    /**
     * Gets the entity name in the event.
     *
     * @return The entity name involved in the event.
     */
    public @NotNull String getEntityName() {
        return entity.getName();
    }

    /**
     * Gets the entity custom name in the event.
     *
     * @return The entity custom name involved in the event.
     * @throws GravesXMethodNotSupportedException if the entity has no custom name
     */
    public @NotNull String getEntityCustomName() {
        final String custom = entity.getCustomName();
        if (custom == null) {
            throw new GravesXMethodNotSupportedException("Entity has no custom name.");
        }
        return custom;
    }

    /**
     * Gets the entity unique ID involved in the event.
     *
     * @return The entity unique ID involved in the event.
     */
    public @NotNull UUID getEntityUniqueId() {
        return entity.getUniqueId();
    }

    /**
     * Gets the entity targeted by the event.
     *
     * @return The target entity, or null if not applicable.
     */
    public @Nullable LivingEntity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Gets the type of the target entity.
     *
     * @return The type of the target entity, or null if not applicable.
     */
    public @Nullable EntityType getEntityType() {
        return (targetEntity != null) ? targetEntity.getType() : null;
    }

    /**
     * Determines if entity in an event is Player or Entity.
     *
     * @return true if player, false otherwise
     */
    public boolean isEntityActuallyPlayer() {
        return entity instanceof Player;
    }

    /**
     * Checks if there is a living entity associated with the event.
     *
     * @return true if a living entity is present
     */
    public boolean hasLivingEntity() {
        return livingEntity != null;
    }

    /**
     * Gets the living entity associated with the event.
     *
     * @return The living entity
     * @throws GravesXNullPointerException if no living entity is present
     */
    public @NotNull LivingEntity getLivingEntity() {
        if (livingEntity == null) {
            throw new GravesXNullPointerException(this, "livingEntity");
        }
        return livingEntity;
    }

    /**
     * Gets the living entity victim name associated with the event.
     *
     * @return The victim name
     * @throws GravesXNullPointerException if no living entity is present
     */
    public @NotNull String getLivingEntityVictim() {
        if (livingEntity == null) {
            throw new GravesXNullPointerException(this, "livingEntity");
        }
        return livingEntity.getName();
    }

    /**
     * Gets the living entity victim uuid associated with the event.
     *
     * @return The victim UUID
     * @throws GravesXNullPointerException if no living entity is present
     */
    public @NotNull UUID getLivingEntityVictimId() {
        if (livingEntity == null) {
            throw new GravesXNullPointerException(this, "livingEntity");
        }
        return livingEntity.getUniqueId();
    }

    /**
     * Checks if the living entity has a killer associated with the event.
     *
     * @return true if a killer is present
     */
    public boolean hasKiller() {
        // Bukkit API: LivingEntity#getKiller() returns Player (nullable)
        return livingEntity != null && livingEntity.getKiller() != null;
    }

    /**
     * Gets the living entity killer associated with the event.
     *
     * @return The killer as a {@link LivingEntity}
     * @throws GravesXNullPointerException if no killer is present
     */
    public @NotNull LivingEntity getLivingEntityKiller() {
        if (livingEntity == null || livingEntity.getKiller() == null) {
            throw new GravesXNullPointerException(this, "livingEntity#getKiller()");
        }
        return livingEntity.getKiller();
    }

    /**
     * Gets the killer's name.
     *
     * @return The killer name
     * @throws GravesXNullPointerException if no killer is present
     */
    public @NotNull String getLivingEntityKillerName() {
        if (livingEntity == null || livingEntity.getKiller() == null) {
            throw new GravesXNullPointerException(this, "livingEntity#getKiller()");
        }
        return livingEntity.getKiller().getName();
    }

    /**
     * Gets the killer's unique ID.
     *
     * @return The killer UUID
     * @throws GravesXNullPointerException if no killer is present
     */
    public @NotNull UUID getLivingEntityKillerUniqueId() {
        if (livingEntity == null || livingEntity.getKiller() == null) {
            throw new GravesXNullPointerException(this, "livingEntity#getKiller()");
        }
        return livingEntity.getKiller().getUniqueId();
    }

    /**
     * Gets the victim entity type.
     *
     * @return The victim type
     * @throws GravesXNullPointerException if no living entity is present
     */
    public @NotNull EntityType getLivingEntityVictimType() {
        if (livingEntity == null) {
            throw new GravesXNullPointerException(this, "livingEntity");
        }
        return livingEntity.getType();
    }

    /**
     * Gets the killer entity type.
     *
     * @return The killer type
     * @throws GravesXNullPointerException if no killer is present
     */
    public @NotNull EntityType getLivingEntityKillerType() {
        if (livingEntity == null || livingEntity.getKiller() == null) {
            throw new GravesXNullPointerException(this, "killer");
        }
        return livingEntity.getKiller().getType();
    }

    public boolean hasPlayer() {
        return entity instanceof Player;
    }

    public @Nullable Player getPlayer() {
        if (entity instanceof Player p) {
            return p;
        }
        throw new GravesXNullPointerException(this, "player");
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static list of handlers for this event.
     *
     * @return The static handler list for this event.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}