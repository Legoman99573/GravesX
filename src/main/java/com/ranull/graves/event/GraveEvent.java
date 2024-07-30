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

    public Grave getGrave() {
        return grave;
    }

    public Entity getEntity() {
        return entity;
    }

    public LivingEntity getTargetEntity() {
        return targetEntity;
    }

    public EntityType getEntityType() {
        return targetEntity != null ? targetEntity.getType() : null;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(@Nullable Location location) {
        this.location = location;
    }

    @Nullable
    public InventoryView getInventoryView() {
        return inventoryView;
    }

    @Nullable
    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    @Nullable
    public BlockData.BlockType getBlockType() {
        return blockType;
    }

    public int getBlockExp() {
        return grave.getExperience();
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}