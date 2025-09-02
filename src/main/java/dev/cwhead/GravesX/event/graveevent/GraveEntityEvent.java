package dev.cwhead.GravesX.event.graveevent;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXEventMethodNotSupportedException;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.Location;
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
    public GraveEntityEvent(@NotNull Grave grave, @NotNull Entity entity, @Nullable Location location, @Nullable BlockData.BlockType blockType, @Nullable org.bukkit.block.Block block, @Nullable LivingEntity livingEntity, @Nullable LivingEntity targetEntity) {
        super(grave, location, blockType, block);
        this.entity = Objects.requireNonNull(entity, "entity");
        this.livingEntity = livingEntity;
        this.targetEntity = targetEntity;
    }

    /**
     * Gets the entity involved in the event.
     *
     * @return The entity involved in the event, or null if not applicable.
     */
    public @NotNull Entity getEntity() { return entity; }

    /**
     * Gets the entity name in the event.
     *
     * @return The entity name involved in the event, or null if not found.
     */
    public @NotNull String getEntityName() { return entity.getName(); }

    /**
     * Gets the entity custom name in the event.
     *
     * @return The entity custom name involved in the event, or null if not found.
     */
    public @NotNull String getEntityCustomName() {
        String custom = entity.getCustomName();
        if (custom == null)
            throw new GravesXEventMethodNotSupportedException("Entity has no custom name.");
        return custom;
    }

    /**
     * Gets the entity unique ID involved in the event.
     *
     * @return The entity unique ID involved in the event.
     */
    public @NotNull UUID getEntityUniqueId() { return entity.getUniqueId(); }

    /**
     * Gets the entity targeted by the event.
     *
     * @return The target entity, or null if not applicable.
     */
    public @Nullable LivingEntity getTargetEntity() { return targetEntity; }

    /**
     * Gets the type of the target entity.
     *
     * @return The type of the target entity, or null if not applicable.
     */
    public @Nullable EntityType getEntityType() {
        return targetEntity != null ? targetEntity.getType() : null;
    }

    /**
     * Determines if entity in an event is Player or Entity
     *
     * @return true if player, false if entity
     */
    public boolean isEntityActuallyPlayer() {
        return entity instanceof org.bukkit.entity.Player;
    }

    /**
     * Checks the living entity associated with the event.
     *
     * @return The living entity, or null if not applicable.
     */
    public boolean hasLivingEntity() {
        return livingEntity != null;
    }

    /**
     * Gets the living entity associated with the event.
     *
     * @return The living entity, or null if not applicable.
     */
    public @NotNull LivingEntity getLivingEntity() {
        if (livingEntity == null)
            throw new GravesXEventNullPointerException(this, "livingEntity");
        return livingEntity;
    }

    /**
     * Gets the living entity victim associated with the event.
     *
     * @return The living entity victim, or null if not applicable.
     */
    public @NotNull String getLivingEntityVictim() {
        if (livingEntity == null)
            throw new GravesXEventNullPointerException(this, "livingEntity");
        return livingEntity.getName();
    }

    /**
     * Gets the living entity victim uuid associated with the event.
     *
     * @return The living entity victim uuid, or null if not applicable.
     */
    public @NotNull UUID getLivingEntityVictimId() {
        if (livingEntity == null)
            throw new GravesXEventNullPointerException(this, "livingEntity");
        return livingEntity.getUniqueId();
    }

    /**
     * Checks the living entity killer associated with the event.
     *
     * @return The living entity killer, or null if not applicable.
     */
    public boolean hasKiller() {
        return livingEntity != null && livingEntity.getKiller() != null;
    }

    /**
     * Gets the living entity killer associated with the event.
     *
     * @return The living entity killer, or null if not applicable.
     */
    public @NotNull LivingEntity getLivingEntityKiller() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GravesXEventNullPointerException(this, "livingEntity#getKiller()");
        return livingEntity.getKiller();
    }

    /**
     * Gets the living entity killers name associated with the event.
     *
     * @return The living entity killers name, or null if not applicable.
     */
    public @NotNull String getLivingEntityKillerName() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GravesXEventNullPointerException(this, "livingEntity#getKiller()");
        return livingEntity.getKiller().getName();
    }

    /**
     * Gets the living entity killers unique ID associated with the event.
     *
     * @return The living entity killers unique ID, or null if not applicable.
     */
    public @NotNull UUID getLivingEntityKillerUniqueId() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GravesXEventNullPointerException(this, "livingEntity#getKiller()");
        return livingEntity.getKiller().getUniqueId();
    }

    /**
     * Gets the living entity victim type associated with the event.
     *
     * @return The living entity victim type, or null if not applicable.
     */
    public @NotNull EntityType getLivingEntityVictimType() {
        if (livingEntity == null)
            throw new GravesXEventNullPointerException(this, "livingEntity");
        return livingEntity.getType();
    }

    /**
     * Gets the living entity killer type associated with the event.
     *
     * @return The living entity killer type, or null if not applicable.
     */
    public @NotNull EntityType getLivingEntityKillerType() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GravesXEventNullPointerException(this, "killer");
        return livingEntity.getKiller().getType();
    }

    public boolean hasPlayer() {
        return entity instanceof Player;
    }

    public @Nullable Player getPlayer() {
        if (entity instanceof Player)
            return ((Player) entity).getPlayer();
        throw new GravesXEventNullPointerException(this, "player");
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @Override public @NotNull HandlerList getHandlers() {
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
