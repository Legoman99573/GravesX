package com.ranull.graves.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
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

/**
 * Represents an abstract event involving a grave in the game.
 */
public abstract class GraveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private final Entity entity;
    private Location location;
    private final InventoryView inventoryView;
    private final LivingEntity livingEntity;
    private final LivingEntity targetEntity;
    private final BlockData.BlockType blockType;
    private final Player player;
    private boolean isCancelled;

    /**
     * Constructs a new GraveEvent.
     *
     * @param grave          the grave involved in this event
     * @param entity         the entity involved in this event, may be null
     * @param location       the location of the event, may be null
     * @param inventoryView  the inventory view involved in this event, may be null
     * @param livingEntity   the living entity involved in this event, may be null
     * @param blockType      the type of block involved in this event, may be null
     * @param targetEntity   the target entity involved in this event, may be null
     * @param player         the player involved in this event, may be null
     */
    public GraveEvent(Grave grave, @Nullable Entity entity, @Nullable Location location, @Nullable InventoryView inventoryView, @Nullable LivingEntity livingEntity, @Nullable BlockData.BlockType blockType, @Nullable LivingEntity targetEntity, @Nullable Player player) {
        this.grave = grave;
        this.entity = entity;
        this.location = location;
        this.inventoryView = inventoryView;
        this.livingEntity = livingEntity;
        this.blockType = blockType;
        this.targetEntity = targetEntity;
        this.player = player;
        this.isCancelled = false;
    }

    /**
     * Gets the grave involved in this event.
     *
     * @return the grave
     */
    public Grave getGrave() {
        return grave;
    }

    /**
     * Gets the entity involved in this event.
     *
     * @return the entity, may be null
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Gets the target entity involved in this event.
     *
     * @return the target entity, may be null
     */
    public LivingEntity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Gets the type of the target entity involved in this event.
     *
     * @return the entity type, or null if the target entity is null
     */
    public EntityType getEntityType() {
        return targetEntity != null ? targetEntity.getType() : null;
    }

    /**
     * Gets the location of this event.
     *
     * @return the location, may be null
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of this event.
     *
     * @param location the new location, may be null
     */
    public void setLocation(@Nullable Location location) {
        this.location = location;
    }

    /**
     * Gets the inventory view involved in this event.
     *
     * @return the inventory view, may be null
     */
    @Nullable
    public InventoryView getInventoryView() {
        return inventoryView;
    }

    /**
     * Gets the living entity involved in this event.
     *
     * @return the living entity, may be null
     */
    @Nullable
    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    /**
     * Gets the type of block involved in this event.
     *
     * @return the block type, may be null
     */
    @Nullable
    public BlockData.BlockType getBlockType() {
        return blockType;
    }

    /**
     * Gets the experience points of the block involved in this event.
     *
     * @return the experience points
     */
    public int getBlockExp() {
        return grave.getExperience();
    }

    /**
     * Gets the player involved in this event.
     *
     * @return the player, may be null
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Checks if this event is cancelled.
     *
     * @return true if this event is cancelled, false otherwise
     */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Sets the cancellation state of this event.
     *
     * @param cancel true to cancel the event, false to uncancel
     */
    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return the handler list
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event type.
     *
     * @return the handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
