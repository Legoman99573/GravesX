package dev.cwhead.GravesX.event;

import com.ranull.graves.data.BlockData;
import dev.cwhead.GravesX.event.interfaces.Addon;
import dev.cwhead.GravesX.exception.GraveEventMethodNotSupportedException;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
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
    private final Grave grave;

    /**
     * The entity associated with the event.
     * <p>
     * This {@link Entity} represents the entity that is involved in the event.
     * </p>
     */
    private final Entity entity;

    /**
     * The location related to the event.
     * <p>
     * This {@link Location} represents the position in the world where the event is taking place or is relevant.
     * </p>
     */
    private Location location;

    /**
     * The inventory view associated with the event.
     * <p>
     * This {@link InventoryView} represents the view of the inventory related to the event, such as a player's inventory or a chest.
     * </p>
     */
    private final InventoryView inventoryView;

    /**
     * The living entity involved in the event.
     * <p>
     * This {@link LivingEntity} represents the living entity that is part of the event.
     * </p>
     */
    private final LivingEntity livingEntity;

    /**
     * The target living entity of the event.
     * <p>
     * This {@link LivingEntity} represents the living entity that is the target or affected by the event.
     * </p>
     */
    private final LivingEntity targetEntity;

    /**
     * The type of block data associated with the event.
     * <p>
     * This {@link BlockData.BlockType} represents the type of block data relevant to the event.
     * </p>
     */
    private final BlockData.BlockType blockType;

    /**
     * The block associated with the event.
     * <p>
     * This {@link Block} represents the specific block involved in the event.
     * </p>
     */
    private final Block block;

    /**
     * The player associated with the event.
     * <p>
     * This {@link Player} represents the player involved in or affected by the event.
     * </p>
     */
    private final Player player;

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
     * @param grave           The grave associated with the event.
     * @param entity          The entity involved in the event, if any.
     * @param location        The location of the event.
     * @param inventoryView   The inventory view associated with the event, if any.
     * @param livingEntity    The living entity associated with the event, if any.
     * @param blockType       The type of block involved in the event, if any.
     * @param block           The block involved in the event, if any.
     * @param targetEntity    The entity targeted by the event, if any.
     * @param player          The player involved in the event, if any.
     */
    public GraveEvent(@NotNull Grave grave, @Nullable Entity entity, @Nullable Location location, @Nullable InventoryView inventoryView, @Nullable LivingEntity livingEntity, @Nullable BlockData.BlockType blockType, @Nullable Block block, @Nullable LivingEntity targetEntity, @Nullable Player player) {
        this.grave = grave;
        this.entity = entity;
        this.location = location;
        this.inventoryView = inventoryView;
        this.livingEntity = livingEntity;
        this.blockType = blockType;
        this.block = block;
        this.targetEntity = targetEntity;
        this.player = player;
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
     * Gets the entity involved in the event.
     *
     * @return The entity involved in the event, or null if not applicable.
     */
    public @NotNull Entity getEntity() {
        if (entity == null)
            throw new GraveEventMethodNotSupportedException("This event does not involve an Entity.");
        return entity;
    }

    /**
     * Gets the entity name in the event.
     *
     * @return The entity name involved in the event, or null if not found.
     */
    public @NotNull String getEntityName() {
        if (entity == null)
            throw new GraveEventMethodNotSupportedException("This event does not involve an Entity name.");
        return entity.getName();
    }

    /**
     * Gets the entity custom name in the event.
     *
     * @return The entity custom name involved in the event, or null if not found.
     */
    public @NotNull String getEntityCustomName() {
        if (entity == null)
            throw new GraveEventMethodNotSupportedException("This event does not involve an Entity custom name.");
        String custom = entity.getCustomName();
        if (custom == null)
            throw new GraveEventMethodNotSupportedException("Entity has no custom name.");
        return custom;
    }

    /**
     * Gets the entity unique ID involved in the event.
     *
     * @return The  entity unique ID involved in the event, or null if not applicable.
     */
    @Nullable
    public UUID getEntityUniqueId() {
        return entity != null ? entity.getUniqueId() : null;
    }

    /**
     * Gets the entity targeted by the event.
     *
     * @return The target entity, or null if not applicable.
     */
    public LivingEntity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Gets the type of the target entity.
     *
     * @return The type of the target entity, or null if not applicable.
     */
    public EntityType getEntityType() {
        return targetEntity != null ? targetEntity.getType() : null;
    }

    /**
     * Checks if there is a location.
     *
     * @return The location of the event.
     */
    public boolean hasLocation() { return location != null; }

    /**
     * Gets the location of the event.
     *
     * @return The location of the event.
     */
    public @NotNull Location getLocation() {
        if (location == null)
            throw new GraveEventMethodNotSupportedException("This event does not have a Location.");
        return location;
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
     * Checks inventory view associated with the event.
     *
     * @return The inventory view, or null if not applicable.
     */
    public boolean hasInventoryView() { return inventoryView != null; }

    /**
     * Gets the inventory view associated with the event.
     *
     * @return The inventory view, or null if not applicable.
     */
    public @NotNull InventoryView getInventoryView() {
        if (inventoryView == null)
            throw new GraveEventMethodNotSupportedException("This event does not support InventoryView access.");
        return inventoryView;
    }

    /**
     * Checks the living entity associated with the event.
     *
     * @return The living entity, or null if not applicable.
     */
    public boolean hasLivingEntity() { return livingEntity != null; }

    /**
     * Gets the living entity associated with the event.
     *
     * @return The living entity, or null if not applicable.
     */
    public @NotNull LivingEntity getLivingEntity() {
        if (livingEntity == null)
            throw new GraveEventMethodNotSupportedException("This event has no LivingEntity.");
        return livingEntity;
    }

    /**
     * Gets the living entity victim associated with the event.
     *
     * @return The living entity victim, or null if not applicable.
     */
    public @NotNull String getLivingEntityVictim() {
        if (livingEntity == null)
            throw new GraveEventMethodNotSupportedException("This event has no victim LivingEntity.");
        return livingEntity.getName();
    }

    /**
     * Gets the living entity victim uuid associated with the event.
     *
     * @return The living entity victim uuid, or null if not applicable.
     */
    public @NotNull UUID getLivingEntityVictimId() {
        if (livingEntity == null)
            throw new GraveEventMethodNotSupportedException("This event has no victim UUID.");
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
            throw new GraveEventMethodNotSupportedException("This event has no killer.");
        return livingEntity.getKiller();
    }

    /**
     * Gets the living entity killers name associated with the event.
     *
     * @return The living entity killers name, or null if not applicable.
     */
    public @NotNull String getLivingEntityKillerName() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GraveEventMethodNotSupportedException("This event has no killer name.");
        return livingEntity.getKiller().getName();
    }

    /**
     * Gets the living entity killers unique ID associated with the event.
     *
     * @return The living entity killers unique ID, or null if not applicable.
     */
    public @NotNull UUID getLivingEntityKillerUniqueId() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GraveEventMethodNotSupportedException("This event has no killer unique ID.");
        return livingEntity.getKiller().getUniqueId();
    }

    /**
     * Gets the living entity victim type associated with the event.
     *
     * @return The living entity victim type, or null if not applicable.
     */
    public @NotNull EntityType getLivingEntityVictimType() {
        if (livingEntity == null)
            throw new GraveEventMethodNotSupportedException("This event has no victim type.");
        return livingEntity.getType();
    }

    /**
     * Gets the living entity killer type associated with the event.
     *
     * @return The living entity killer type, or null if not applicable.
     */
    public @NotNull EntityType getLivingEntityKillerType() {
        if (livingEntity == null || livingEntity.getKiller() == null)
            throw new GraveEventMethodNotSupportedException("This event has no killer type.");
        return livingEntity.getKiller().getType();
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
        if (blockType == null)
            throw new GraveEventMethodNotSupportedException("This event has no BlockType.");
        return blockType;
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
        if (block == null)
            throw new GraveEventMethodNotSupportedException("This event has no Block.");
        return block;
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
     * Checks the player involved in the event.
     *
     * @return The player involved in the event, or null if not applicable.
     */
    public boolean hasPlayer() { return player != null; }

    /**
     * Gets the player involved in the event.
     *
     * @return The player involved in the event, or null if not applicable.
     */
    public @NotNull Player getPlayer() {
        if (player == null)
            throw new GraveEventMethodNotSupportedException("This event does not involve a Player.");
        return player;
    }

    /**
     * Gets the player name involved in the event.
     *
     * @return The player name involved in the event, or null if not applicable.
     */
    public @NotNull String getPlayerName() {
        if (player == null)
            throw new GraveEventMethodNotSupportedException("This event has no Player name.");
        return player.getName();
    }

    /**
     * Gets the player unique ID involved in the event.
     *
     * @return The player unique ID involved in the event, or null if not applicable.
     */
    public @NotNull UUID getPlayerUniqueId() {
        if (player == null)
            throw new GraveEventMethodNotSupportedException("This event has no Player unique ID.");
        return player.getUniqueId();
    }

    /**
     * Gets the player display name involved in the event.
     *
     * @return The player display name involved in the event, or null if not applicable.
     */
    public @NotNull String getPlayerDisplayName() {
        if (player == null)
            throw new GraveEventMethodNotSupportedException("This event has no Player display name.");
        return player.getDisplayName();
    }

    /**
     * Checks whether the event is cancelled.
     *
     * @return True if the event is cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Sets whether the event is cancelled.
     *
     * @param cancel True to cancel the event, false otherwise.
     */
    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    /**
     * Checks if the event is an addon hook.
     *
     * @return {@code true} if this is an addon, {@code false} otherwise.
     */
    @Override
    public boolean isAddon() {
        return isAddon;
    }

    /**
     * Sets the addon status for the current event.
     *
     * @param addon {@code true} to mark as an addon, {@code false} otherwise.
     */
    @Override
    public void setAddon(boolean addon) {
        isAddon = addon;
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