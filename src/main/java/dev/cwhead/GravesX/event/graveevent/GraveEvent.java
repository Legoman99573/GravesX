package dev.cwhead.GravesX.event.graveevent;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.interfaces.Addon;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * The base class for all grave-related events.
 * <p>
 * This class provides common properties for grave events, such as the grave itself,
 * the location of the event, the entity involved, and additional information like
 * inventory views and blocks. This class is cancellable, allowing event listeners
 * to prevent the event from proceeding.
 * </p>
 */
public abstract class GraveEvent extends Event implements Cancellable, Addon {
    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The grave associated with the event.
     * <p>
     * This {@link Grave} represents the specific grave entity involved in the event.
     * </p>
     */
    public final Grave grave;

    /**
     * The location related to the event.
     * <p>
     * This {@link Location} represents the position in the world where the event is taking place or is relevant.
     * </p>
     */
    public @Nullable Location location;

    /**
     * The type of block data associated with the event.
     * <p>
     * This {@link BlockData.BlockType} represents the type of block data relevant to the event.
     * </p>
     */
    public final @Nullable BlockData.BlockType blockType;

    /**
     * The block associated with the event.
     * <p>
     * This {@link Block} represents the specific block involved in the event.
     * </p>
     */
    public final @Nullable Block block;

    /**
     * Indicates whether the event has been cancelled.
     * <p>
     * This {@code boolean} flag is used to determine if the event should be cancelled or not.
     * </p>
     */
    private boolean isCancelled;

    /**
     * Indicates whether items should be dropped during the event.
     * <p>
     * This {@code boolean} flag determines if items should be dropped as a result of the event.
     * </p>
     */
    private boolean dropItems;

    /**
     * Indicates whether the event is an Addon
     * <p>
     * This {@code boolean} flag is used to determine if the event should be an addon or not.
     * </p>
     */
    private boolean isAddon;

    /**
     * Constructs a new {@code GraveEvent}.
     *
     * @param grave     The grave associated with the event.
     * @param location  The location of the event.
     * @param blockType The type of block involved in the event, if any.
     * @param block     The block involved in the event, if any.
     */
    protected GraveEvent(@NotNull Grave grave, @Nullable Location location, @Nullable BlockData.BlockType blockType, @Nullable Block block) {
        this.grave = Objects.requireNonNull(grave, "grave");
        this.location = location;
        this.blockType = blockType;
        this.block = block;
        this.isCancelled = false;
        this.dropItems = true;
        this.isAddon = false;
    }

    /**
     * Gets the grave associated with the event.
     *
     * @return The grave associated with the event.
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the grave experience associated with the event.
     *
     * @return The grave experience associated with the event.
     */
    public int getGraveExperience() {
        return grave.getExperience();
    }

    /**
     * Gets the grave UUID associated with the event.
     *
     * @return The grave UUID associated with the event.
     */
    public UUID getGraveUUID() {
        return grave.getUUID();
    }

    /**
     * Gets the grave owner display name associated with the event.
     *
     * @return The grave owner display name associated with the event.
     */
    public String getGraveOwnerDisplayName() {
        return grave.getOwnerDisplayName();
    }

    /**
     * Gets the grave owner name associated with the event.
     *
     * @return The grave owner name associated with the event.
     */
    public String getGraveOwnerName() {
        return grave.getOwnerName();
    }

    /**
     * Gets the grave owner unique ID associated with the event.
     *
     * @return The grave owner unique ID associated with the event.
     */
    public UUID getGraveOwnerUniqueId() {
        return grave.getOwnerUUID();
    }

    /**
     * Gets the grave owner name display associated with the event.
     *
     * @return The grave owner name display associated with the event.
     */
    public String getGraveOwnerNameDisplay() {
        return grave.getOwnerNameDisplay();
    }

    /**
     * Gets the grave owner texture associated with the event.
     *
     * @return The grave owner texture display associated with the event.
     */
    public String getGraveOwnerTexture() {
        return grave.getOwnerTexture();
    }

    /**
     * Gets the grave owner texture signature associated with the event.
     *
     * @return The grave owner texture signature associated with the event.
     */
    public String getGraveOwnerTextureSignature() {
        return grave.getOwnerTextureSignature();
    }

    /**
     * Checks if there is a location.
     *
     * @return The location of the event.
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * Gets the location of the event.
     *
     * @return The location of the event.
     */
    public @NotNull Location getLocation() {
        if (location != null) {
            return location;
        }
        throw new GravesXEventNullPointerException(this, "location");
    }

    /**
     * Sets the location of the event.
     *
     * @param location The new location of the event.
     */
    public void setLocation(@NotNull Location location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    /**
     * Checks the type of block involved in the event.
     *
     * @return The block type, or null if not applicable.
     */
    public boolean hasBlockType() { return blockType != null; }

    /**
     * Gets the type of block involved in the event.
     *
     * @return The block type, or null if not applicable.
     */
    public @NotNull BlockData.BlockType getBlockType() {
        if (blockType != null) {
            return blockType;
        }
        throw new GravesXEventNullPointerException(this, "blockType");
    }

    /**
     * Checks the block involved in the event.
     *
     * @return The block involved in the event, or null if not applicable.
     */
    public boolean hasBlock() { return block != null; }

    /**
     * Gets the block involved in the event.
     *
     * @return The block involved in the event, or null if not applicable.
     */
    public @NotNull Block getBlock() {
        if (block != null) {
            return block;
        }
        throw new GravesXEventNullPointerException(this, "block");
    }

    /**
     * Gets the experience points associated with the grave.
     *
     * @return The experience points.
     */
    public int getBlockExp() {
        return grave.getExperience();
    }

    /**
     * Sets the experience points associated with the grave.
     */
    public void setBlockExp(int experience) {
        grave.setExperience(experience);
    }

    /**
     * Checks whether items should drop upon breaking the grave block.
     *
     * @return True if items should drop, false otherwise.
     */
    public boolean isDropItems() {
        return this.dropItems;
    }

    /**
     * Sets whether items should drop upon breaking the grave block.
     *
     * @param dropItems True if items should drop, false otherwise.
     */
    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    /**
     * Checks whether the event is cancelled.
     *
     * @return True if the event is cancelled, false otherwise.
     */
    @Override public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Sets whether the event is cancelled.
     *
     * @param cancel True to cancel the event, false otherwise.
     */
    @Override public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    /**
     * Checks if the event is an addon hook.
     *
     * @return {@code true} if this is an addon, {@code false} otherwise.
     */
    @Override public boolean isAddon() {
        return isAddon;
    }

    /**
     * Sets the addon status for the current event.
     *
     * @param addon {@code true} to mark as an addon, {@code false} otherwise.
     */
    @Override public void setAddon(boolean addon) {
        isAddon = addon;
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