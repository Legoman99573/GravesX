package dev.cwhead.GravesX.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an event that occurs when a grave is placed by a {@link org.bukkit.entity.LivingEntity} whether that be a {@link org.bukkit.entity.Player} or {@link org.bukkit.entity.Entity}.
 * <p>
 * This event extends {@link GraveEvent} and provides information about the grave
 * and the {@link org.bukkit.entity.LivingEntity} ({@link org.bukkit.entity.Player} or {@link org.bukkit.entity.Entity}) involved when the grave is placed.
 * </p>
 */
public class GraveBlockPlaceEvent extends GraveEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The Killer (if known).
     */
    private final @Nullable LivingEntity livingEntity;

    /**
     * Constructs a new GraveBlockPlaceEvent.
     *
     * @param grave        The grave associated with the event.
     * @param location     The location where the block is being placed.
     * @param blockType    The type of the block being placed.
     * @param block        The block being placed.
     * @param livingEntity The Killer
     */
    public GraveBlockPlaceEvent(@NotNull Grave grave, @NotNull Location location, @Nullable BlockData.BlockType blockType, @Nullable Block block, @Nullable LivingEntity livingEntity) {
        super(grave, location, blockType, block);
        this.livingEntity = livingEntity;
    }

    /**
     * @deprecated Use {@link GraveBlockPlaceEvent#GraveBlockPlaceEvent(Grave, Location, BlockData.BlockType, Block, LivingEntity)} instead.
     * Constructs a new GraveBlockPlaceEvent.
     *
     * @param grave     The grave associated with the event.
     * @param location  The location where the block is being placed.
     * @param blockType The type of the block being placed.
     */
    @Deprecated
    public GraveBlockPlaceEvent(@NotNull Grave grave, @NotNull Location location, @Nullable BlockData.BlockType blockType) {
        this(grave, location, blockType, null, null);
    }

    /**
     * Check if there is an entity
     *
     * @return true if any entity is associated with this event.
     */
    public boolean hasEntity() {
        return livingEntity != null;
    }

    /**
     * Gets Entity
     *
     * @return the associated entity (killer) or throws if absent.
     * @throws GravesXEventNullPointerException if no entity is present.
     */
    public @NotNull Entity getEntity() {
        if (livingEntity == null) {
            throw new GravesXEventNullPointerException(this, "livingEntity");
        }
        return livingEntity;
    }

    /**
     * Gets the killer
     *
     * @return the killer or throws if absent.
     * @throws GravesXEventNullPointerException if no {@link LivingEntity} or {@link LivingEntity#getKiller()} is present.
     */
    public @NotNull LivingEntity getKiller() {
        if (livingEntity == null) {
            throw new GravesXEventNullPointerException(this, "livingEntity");
        }
        if (livingEntity.getKiller() == null) {
            throw new GravesXEventNullPointerException(this, "killer");
        }
        return livingEntity.getKiller();
    }

    /**
     * Gets the killer name
     *
     * @return the killer name or throws if absent.
     * @throws GravesXEventNullPointerException if no killer is present.
     */
    public @NotNull String getKillerName() {
        if (livingEntity == null) {
            throw new GravesXEventNullPointerException(this, "livingEntity");
        }
        if (livingEntity.getKiller() == null) {
            throw new GravesXEventNullPointerException(this, "killer");
        }
        return livingEntity.getKiller().getName();
    }

    /**
     * Gets the Killers Unique ID
     *
     * @return the killer unique ID or throws if absent.
     * @throws GravesXEventNullPointerException if no killer is present.
     */
    public @NotNull UUID getKillerUniqueId() {
        if (livingEntity == null) {
            throw new GravesXEventNullPointerException(this, "livingEntity");
        }
        if (livingEntity.getKiller() == null) {
            throw new GravesXEventNullPointerException(this, "killer");
        }
        return livingEntity.getKiller().getUniqueId();
    }

    /**
     * Checks if there is a killer
     *
     * @return true if a killer is associated with this event.
     */
    public boolean hasKiller() {
        if (livingEntity == null) return false;
        return livingEntity.getKiller() != null;
    }

    /**
     * Check if there is a player.
     *
     * @return true if the killer is a Player and still references a valid Player object.
     */
    public boolean hasPlayer() {
        if (livingEntity instanceof Player) {
            return ((Player) livingEntity).getPlayer() != null;
        }
        return false;
    }

    /**
     * Gets the player
     *
     * @return the player (killer) or throws if absent.
     * @throws GravesXEventNullPointerException if the killer is not a player.
     */
    public Player getPlayer() {
        if (livingEntity instanceof Player) {
            return ((Player) livingEntity).getPlayer();
        }
        throw new GravesXEventNullPointerException(this, "player");
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
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}