package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents an event that occurs when a {@link Grave} is created for an entity or player.
 */
public class GravePreCreateEvent extends GraveEntityEvent {

    /**
     * Static handler list for Bukkit's event registration system.
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * UUID of the grave.
     */
    private @Nullable UUID graveUUID;

    /**
     * Death location of the grave.
     */
    private @Nullable Location deathLocation;

    /**
     * Type of the entity that owns this grave.
     */
    private @Nullable EntityType ownerType;

    /**
     * Internal owner name (usually player or entity name).
     */
    private @Nullable String ownerName;

    /**
     * Display name of the owner (for messages, holograms, etc.).
     */
    private @Nullable String ownerNameDisplay;

    /**
     * UUID of the grave owner.
     */
    private @Nullable UUID ownerUUID;

    /**
     * Permission list associated with this grave.
     */
    private @Nullable List<String> permissionList;

    /**
     * Yaw (horizontal rotation) at death.
     */
    private float yaw;

    /**
     * Pitch (vertical rotation) at death.
     */
    private float pitch;

    /**
     * Time-to-live of the grave in milliseconds.
     */
    private long timeAlive;

    /**
     * Owner skin texture (base64 or URL), if any.
     */
    private @Nullable String ownerTexture;

    /**
     * Owner skin texture signature, if any.
     */
    private @Nullable String ownerTextureSignature;

    /**
     * Experience stored in this grave.
     */
    private int experience;

    /**
     * Killer entity type, if any.
     */
    private @Nullable EntityType killerType;

    /**
     * Internal killer name.
     */
    private @Nullable String killerName;

    /**
     * Killer display name.
     */
    private @Nullable String killerNameDisplay;

    /**
     * Killer UUID, if any.
     */
    private @Nullable UUID killerUUID;

    /**
     * Whether protection is enabled for this grave.
     */
    private boolean protectionEnabled;

    /**
     * Duration of protection in milliseconds.
     */
    private long protectionTime;

    /**
     * Items intended to be stored in the grave.
     */
    private @Nullable Collection<ItemStack> graveItemStackList;

    /**
     * Items that were ignored (not stored) when creating this grave.
     */
    private @Nullable Collection<ItemStack> ignoredItems;

    /**
     * Blocks that were ignored for this grave creation.
     */
    private @Nullable Collection<Block> ignoredBlocks;

    /**
     * A human-readable death reason (e.g., "FALL", "VOID", "LAVA", or localized text from integrations).
     * May be null if unknown.
     */
    private @Nullable String deathReason;

    /**
     * Constructs a new {@code GravePreCreateEvent} without any ignored items/blocks information.
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    public GravePreCreateEvent(@NotNull Entity entity, @NotNull Grave grave) {
        this(entity, grave, null, null, null);
    }

    /**
     * Constructs a new {@code GravePreCreateEvent} with ignored items and blocks.
     *
     * @param entity             The entity for which the grave is being created.
     * @param grave              The grave being created.
     * @param graveItemStackList Items intended to be stored in the grave (may be {@code null} or empty).
     * @param ignoredItems       Items that were not stored in the grave (may be {@code null} or empty).
     * @param ignoredBlocks      Blocks that were ignored for this grave (may be {@code null} or empty).
     */
    public GravePreCreateEvent(@NotNull Entity entity,
                            @NotNull Grave grave,
                            @Nullable Collection<ItemStack> graveItemStackList,
                            @Nullable Collection<ItemStack> ignoredItems,
                            @Nullable Collection<Block> ignoredBlocks) {

        super(grave, entity, grave.getLocationDeath() != null ? grave.getLocationDeath() : entity.getLocation(), null, null,
                (entity instanceof LivingEntity) ? (LivingEntity) entity : null, null);

        this.graveItemStackList = (graveItemStackList == null) ? null : new ArrayList<>(graveItemStackList);
        this.ignoredItems = (ignoredItems == null) ? null : new ArrayList<>(ignoredItems);
        this.ignoredBlocks = (ignoredBlocks == null) ? null : new ArrayList<>(ignoredBlocks);
    }

    /**
     * Backwards-compatible constructor omitting the grave items collection.
     *
     * @param entity        The entity for which the grave is being created.
     * @param grave         The grave being created.
     * @param ignoredItems  Items that were not stored in the grave (may be {@code null} or empty).
     * @param ignoredBlocks Blocks that were ignored for this grave (may be {@code null} or empty).
     */
    public GravePreCreateEvent(@NotNull Entity entity,
                            @NotNull Grave grave,
                            @Nullable Collection<ItemStack> ignoredItems,
                            @Nullable Collection<Block> ignoredBlocks) {
        this(entity, grave, null, ignoredItems, ignoredBlocks);
    }

    /**
     * Gets the underlying {@link Grave} associated with this event.
     *
     * @return the {@link Grave} associated with this event, never {@code null}.
     */
    @Override
    public @NotNull Grave getGrave() {
        return super.getGrave();
    }

    /**
     * Gets the UUID of the grave (local snapshot).
     *
     * @return the grave UUID, or {@code null} if not set.
     */
    public @Nullable UUID getGraveUUID() {
        return graveUUID;
    }

    /**
     * Sets the UUID of the grave (local snapshot).
     *
     * @param uuid the grave UUID, or {@code null} to clear it.
     */
    public void setGraveUUID(@Nullable UUID uuid) {
        this.graveUUID = uuid;
    }

    /**
     * Gets the death location for this grave (local snapshot).
     *
     * @return the current death location, or {@code null} if not set.
     */
    public @Nullable Location getLocationDeath() {
        return deathLocation;
    }

    /**
     * Sets the death location for this grave (local snapshot).
     * <p>
     * The creator is responsible for applying this value back to the {@link Grave}.
     * </p>
     *
     * @param location the new death location, or {@code null} to clear it.
     */
    public void setDeathLocation(@Nullable Location location) {
        this.deathLocation = location;
    }

    /**
     * Gets the type of the entity that owns this grave (local snapshot).
     *
     * @return the owner entity type, or {@code null} if not set.
     */
    public @Nullable EntityType getOwnerType() {
        return ownerType;
    }

    /**
     * Sets the type of the entity that owns this grave (local snapshot).
     *
     * @param type the owner entity type, or {@code null} to clear it.
     */
    public void setOwnerType(@Nullable EntityType type) {
        this.ownerType = type;
    }

    /**
     * Gets the internal owner name (local snapshot).
     *
     * @return the owner name, or {@code null} if not set.
     */
    public @Nullable String getOwnerName() {
        return ownerName;
    }

    /**
     * Sets the internal owner name (local snapshot).
     *
     * @param name the owner name, or {@code null} to clear it.
     */
    public void setOwnerName(@Nullable String name) {
        this.ownerName = name;
    }

    /**
     * Gets the display name for the grave owner (local snapshot).
     *
     * @return the owner display name, or {@code null} if not set.
     */
    public @Nullable String getOwnerNameDisplay() {
        return ownerNameDisplay;
    }

    /**
     * Sets the display name for the grave owner (local snapshot).
     *
     * @param displayName the owner display name, or {@code null} to clear it.
     */
    public void setOwnerNameDisplay(@Nullable String displayName) {
        this.ownerNameDisplay = displayName;
    }

    /**
     * Gets the UUID of the grave owner (local snapshot).
     *
     * @return the owner UUID, or {@code null} if not set.
     */
    public @Nullable UUID getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Sets the UUID of the grave owner (local snapshot).
     *
     * @param uuid the owner UUID, or {@code null} to clear it.
     */
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }

    /**
     * Gets an unmodifiable view of the permission list associated with this grave (local snapshot).
     *
     * @return a non-{@code null}, unmodifiable list of permission strings; empty if none.
     */
    public @NotNull List<String> getPermissionList() {
        return permissionList == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(permissionList);
    }

    /**
     * Sets the permission list associated with this grave (local snapshot).
     *
     * @param permissionList the new permission list, or {@code null} to clear it.
     */
    public void setPermissionList(@Nullable List<String> permissionList) {
        this.permissionList = (permissionList == null)
                ? null
                : new ArrayList<>(permissionList);
    }

    /**
     * Gets the yaw (horizontal rotation) of the grave owner at the time of death (local snapshot).
     *
     * @return the yaw value.
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Sets the yaw (horizontal rotation) of the grave owner at the time of death (local snapshot).
     *
     * @param yaw the new yaw value.
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * Gets the pitch (vertical rotation) of the grave owner at the time of death (local snapshot).
     *
     * @return the pitch value.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Sets the pitch (vertical rotation) of the grave owner at the time of death (local snapshot).
     *
     * @param pitch the new pitch value.
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * Gets the amount of time, in milliseconds, that the grave will remain before timing out (local snapshot).
     *
     * @return the time-to-live in milliseconds.
     */
    public long getTimeAlive() {
        return timeAlive;
    }

    /**
     * Sets the amount of time, in milliseconds, that the grave will remain before timing out (local snapshot).
     *
     * @param millis the time-to-live in milliseconds.
     */
    public void setTimeAlive(long millis) {
        this.timeAlive = millis;
    }

    /**
     * Gets the owner texture (e.g., skin texture URL or base64 string), if any (local snapshot).
     *
     * @return the owner texture, or {@code null} if not set.
     */
    public @Nullable String getOwnerTexture() {
        return ownerTexture;
    }

    /**
     * Sets the owner texture (e.g., skin texture URL or base64 string) (local snapshot).
     *
     * @param texture the owner texture, or {@code null} to clear it.
     */
    public void setOwnerTexture(@Nullable String texture) {
        this.ownerTexture = texture;
    }

    /**
     * Gets the owner texture signature, if any (local snapshot).
     *
     * @return the owner texture signature, or {@code null} if not set.
     */
    public @Nullable String getOwnerTextureSignature() {
        return ownerTextureSignature;
    }

    /**
     * Sets the owner texture signature (local snapshot).
     *
     * @param signature the texture signature, or {@code null} to clear it.
     */
    public void setOwnerTextureSignature(@Nullable String signature) {
        this.ownerTextureSignature = signature;
    }

    /**
     * Gets the amount of experience stored in this grave (local snapshot).
     *
     * @return the stored experience amount.
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Sets the amount of experience stored in this grave (local snapshot).
     *
     * @param experience the experience amount to store.
     */
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * Gets the type of the killer entity, if any (local snapshot).
     *
     * @return the killer entity type, or {@code null} if unknown or not set.
     */
    public @Nullable EntityType getKillerType() {
        return killerType;
    }

    /**
     * Sets the type of the killer entity (local snapshot).
     *
     * @param type the killer entity type, or {@code null} to clear it.
     */
    public void setKillerType(@Nullable EntityType type) {
        this.killerType = type;
    }

    /**
     * Gets the internal killer name (local snapshot).
     *
     * @return the killer name, or {@code null} if not set.
     */
    public @Nullable String getKillerName() {
        return killerName;
    }

    /**
     * Sets the internal killer name (local snapshot).
     *
     * @param name the killer name, or {@code null} to clear it.
     */
    public void setKillerName(@Nullable String name) {
        this.killerName = name;
    }

    /**
     * Gets the display name for the killer (local snapshot).
     *
     * @return the killer display name, or {@code null} if not set.
     */
    public @Nullable String getKillerNameDisplay() {
        return killerNameDisplay;
    }

    /**
     * Sets the display name for the killer (local snapshot).
     *
     * @param displayName the killer display name, or {@code null} to clear it.
     */
    public void setKillerNameDisplay(@Nullable String displayName) {
        this.killerNameDisplay = displayName;
    }

    /**
     * Gets the UUID of the killer, if any (local snapshot).
     *
     * @return the killer UUID, or {@code null} if unknown or not set.
     */
    public @Nullable UUID getKillerUUID() {
        return killerUUID;
    }

    /**
     * Sets the UUID of the killer (local snapshot).
     *
     * @param uuid the killer UUID, or {@code null} to clear it.
     */
    public void setKillerUUID(@Nullable UUID uuid) {
        this.killerUUID = uuid;
    }

    /**
     * Returns whether protection is enabled for this grave (local snapshot).
     *
     * @return {@code true} if protection is enabled; {@code false} otherwise.
     */
    public boolean getProtection() {
        return protectionEnabled;
    }

    /**
     * Sets whether protection is enabled for this grave (local snapshot).
     *
     * @param enabled {@code true} to enable protection; {@code false} to disable it.
     */
    public void setProtection(boolean enabled) {
        this.protectionEnabled = enabled;
    }

    /**
     * Gets the duration of protection in milliseconds (local snapshot).
     *
     * @return the protection time in milliseconds.
     */
    public long getTimeProtection() {
        return protectionTime;
    }

    /**
     * Sets the duration of protection in milliseconds (local snapshot).
     *
     * @param millis the protection time in milliseconds.
     */
    public void setTimeProtection(long millis) {
        this.protectionTime = millis;
    }

    /**
     * Gets the items intended to be stored in the grave (local snapshot).
     *
     * @return an unmodifiable collection of grave items, or an empty collection if none.
     */
    public @NotNull Collection<ItemStack> getGraveItemStackList() {
        return graveItemStackList == null
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(graveItemStackList);
    }

    /**
     * Sets the items intended to be stored in the grave (local snapshot).
     *
     * @param graveItemStackList items intended to be stored in the grave (may be {@code null} or empty).
     */
    public void setGraveItemStackList(@Nullable Collection<ItemStack> graveItemStackList) {
        this.graveItemStackList = (graveItemStackList == null)
                ? null
                : new ArrayList<>(graveItemStackList);
    }

    /**
     * Gets the items that were ignored (not stored) when creating this grave (local snapshot).
     *
     * @return an unmodifiable collection of ignored items, or an empty collection if none.
     */
    public @NotNull Collection<ItemStack> getIgnoredItems() {
        return ignoredItems == null
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(ignoredItems);
    }

    /**
     * Sets the items that were ignored (not stored) when creating this grave (local snapshot).
     *
     * @param ignoredItems items that were not stored in the grave (may be {@code null} or empty).
     */
    public void setIgnoredItems(@Nullable Collection<ItemStack> ignoredItems) {
        this.ignoredItems = (ignoredItems == null)
                ? null
                : new ArrayList<>(ignoredItems);
    }

    /**
     * Adds a single item to the ignored items list for this grave creation (local snapshot).
     *
     * @param item the item to add to the ignored items list, or {@code null} to do nothing.
     */
    public void addIgnoredItem(@Nullable ItemStack item) {
        if (item == null) {
            return;
        }
        if (this.ignoredItems == null) {
            this.ignoredItems = new ArrayList<>();
        }
        this.ignoredItems.add(item);
    }

    /**
     * Removes a single item from the ignored items list for this grave creation (local snapshot).
     *
     * @param item the item to remove from the ignored items list (may be {@code null}).
     * @return {@code true} if the item was present and removed; {@code false} otherwise.
     */
    public boolean removeIgnoredItem(@Nullable ItemStack item) {
        if (item == null || this.ignoredItems == null) {
            return false;
        }
        return this.ignoredItems.remove(item);
    }

    /**
     * Gets the blocks that were ignored when creating this grave (local snapshot).
     *
     * @return an unmodifiable collection of ignored blocks, or an empty collection if none.
     */
    public @NotNull Collection<Block> getIgnoredBlocks() {
        return ignoredBlocks == null
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(ignoredBlocks);
    }

    /**
     * Sets the blocks that were ignored for this grave (local snapshot).
     *
     * @param ignoredBlocks blocks that were ignored for this grave (may be {@code null} or empty).
     */
    public void setIgnoredBlocks(@Nullable Collection<Block> ignoredBlocks) {
        this.ignoredBlocks = (ignoredBlocks == null)
                ? null
                : new ArrayList<>(ignoredBlocks);
    }

    /**
     * Adds a single block to the ignored blocks list for this grave creation (local snapshot).
     *
     * @param block the block to add to the ignored blocks list, or {@code null} to do nothing.
     */
    public void addIgnoredBlock(@Nullable Block block) {
        if (block == null) {
            return;
        }
        if (this.ignoredBlocks == null) {
            this.ignoredBlocks = new ArrayList<>();
        }
        this.ignoredBlocks.add(block);
    }

    /**
     * Removes a single block from the ignored blocks list for this grave creation (local snapshot).
     *
     * @param block the block to remove from the ignored blocks list (may be {@code null}).
     * @return {@code true} if the block was present and removed; {@code false} otherwise.
     */
    public boolean removeIgnoredBlock(@Nullable Block block) {
        if (block == null || this.ignoredBlocks == null) {
            return false;
        }
        return this.ignoredBlocks.remove(block);
    }

    /**
     * Returns the human-readable death reason for this event snapshot.
     *
     * @return the stored death cause string.
     */
    public @Nullable String getDeathReason() {
        return deathReason;
    }

    /**
     * Returns the Bukkit damage cause enum from the stored death reason.
     *
     * @return the damage cause, or {@code null} if invalid/unset.
     */
    public @Nullable EntityDamageEvent.DamageCause getDeathCause() {
        if (deathReason == null || deathReason.isBlank()) {
            return null;
        }

        try {
            return EntityDamageEvent.DamageCause.valueOf(deathReason);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Sets a human-readable death reason directly.
     *
     * @param reason the reason text to store, or {@code null} to clear.
     */
    public void setDeathReason(@Nullable String reason) {
        this.deathReason = (reason != null && !reason.isBlank()) ? reason : null;
    }

    /**
     * Convenience setter that accepts a Bukkit damage event and extracts a basic reason.
     * Does not localize; callers can still override via {@link #setDeathReason(String)}.
     *
     * @param damageEvent the last damage event, or {@code null}.
     */
    public void setDeathCause(@Nullable String damageEvent) {
        this.deathReason = (!damageEvent.isEmpty())
                ? damageEvent
                : null;
    }

    /**
     * Gets the list of handlers for this event instance.
     *
     * @return the {@link HandlerList} for this event.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static list of handlers for this event type.
     *
     * @return the static {@link HandlerList} for {@link GravePreCreateEvent}.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
