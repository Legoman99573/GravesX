package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents an event that occurs when a grave is created for an entity.
 * <p>
 * This event extends {@link GraveEntityEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
public class GraveCreateEvent extends GraveEntityEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Items intended to be stored in the grave.
     */
    private @Nullable Collection<ItemStack> graveItemStackList;

    /**
     * Items that were ignored (not stored in the grave).
     */
    private @Nullable Collection<ItemStack> ignoredItems;

    /**
     * Blocks that were ignored for this grave creation.
     */
    private @Nullable Collection<Block> ignoredBlocks;

    /**
     * Constructs a new {@code GraveCreateEvent} without any ignored items/blocks information.
     * <p>
     * This constructor is kept for backwards compatibility. It simply delegates to the
     * full constructor with {@code null} collections.
     * </p>
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    public GraveCreateEvent(@NotNull Entity entity, @NotNull Grave grave) {
        this(entity, grave, null, null, null);
    }

    /**
     * Constructs a new {@code GraveCreateEvent} with ignored items and blocks.
     *
     * @param entity             The entity for which the grave is being created.
     * @param grave              The grave being created.
     * @param graveItemStackList Items intended to be stored in the grave (may be {@code null} or empty).
     * @param ignoredItems       Items that were not stored in the grave (may be {@code null} or empty).
     * @param ignoredBlocks      Blocks that were ignored for this grave (may be {@code null} or empty).
     */
    public GraveCreateEvent(@NotNull Entity entity,
                            @NotNull Grave grave,
                            @Nullable Collection<ItemStack> graveItemStackList,
                            @Nullable Collection<ItemStack> ignoredItems,
                            @Nullable Collection<Block> ignoredBlocks) {

        super(grave, entity, grave.getLocationDeath(), null, null,
                (entity instanceof LivingEntity) ? (LivingEntity) entity : null, null);

        this.graveItemStackList = (graveItemStackList == null) ? null : new ArrayList<>(graveItemStackList);
        this.ignoredItems = (ignoredItems == null) ? null : new ArrayList<>(ignoredItems);
        this.ignoredBlocks = (ignoredBlocks == null) ? null : new ArrayList<>(ignoredBlocks);
    }

    /**
     * Backwards-compatible constructor (no grave items provided).
     *
     * @param entity        The entity for which the grave is being created.
     * @param grave         The grave being created.
     * @param ignoredItems  Items that were not stored in the grave (may be {@code null} or empty).
     * @param ignoredBlocks Blocks that were ignored for this grave (may be {@code null} or empty).
     */
    public GraveCreateEvent(@NotNull Entity entity,
                            @NotNull Grave grave,
                            @Nullable Collection<ItemStack> ignoredItems,
                            @Nullable Collection<Block> ignoredBlocks) {
        this(entity, grave, null, ignoredItems, ignoredBlocks);
    }

    /**
     * Gets the items intended to be stored in the grave.
     *
     * @return An unmodifiable collection of grave items, or an empty collection if none.
     */
    public @NotNull Collection<ItemStack> getGraveItemStackList() {
        return graveItemStackList == null
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(graveItemStackList);
    }

    /**
     * Sets the items intended to be stored in the grave.
     *
     * @param graveItemStackList Items intended to be stored in the grave (may be {@code null} or empty).
     */
    public void setGraveItemStackList(@Nullable Collection<ItemStack> graveItemStackList) {
        this.graveItemStackList = (graveItemStackList == null)
                ? null
                : new ArrayList<>(graveItemStackList);
    }

    /**
     * Gets the items that were ignored (not stored) when creating this grave.
     *
     * @return An unmodifiable collection of ignored items, or an empty collection if none.
     */
    public @NotNull Collection<ItemStack> getIgnoredItems() {
        return ignoredItems == null
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(ignoredItems);
    }

    /**
     * Sets the items that were ignored (not stored) when creating this grave.
     *
     * @param ignoredItems Items that were not stored in the grave (may be {@code null} or empty).
     */
    public void setIgnoredItems(@Nullable Collection<ItemStack> ignoredItems) {
        this.ignoredItems = (ignoredItems == null)
                ? null
                : new ArrayList<>(ignoredItems);
    }

    /**
     * Adds a single item to the ignored items list for this grave creation.
     * <p>
     * If the internal collection is {@code null}, it will be created.
     * </p>
     *
     * @param item The item to add to the ignored items list (may be {@code null}, in which case this call is a no-op).
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
     * Removes a single item from the ignored items list for this grave creation.
     *
     * @param item The item to remove from the ignored items list (may be {@code null}, in which case this call is a no-op).
     * @return {@code true} if the item was present and removed; {@code false} otherwise.
     */
    public boolean removeIgnoredItem(@Nullable ItemStack item) {
        if (item == null || this.ignoredItems == null) {
            return false;
        }
        return this.ignoredItems.remove(item);
    }

    /**
     * Gets the blocks that were ignored when creating this grave.
     *
     * @return An unmodifiable collection of ignored blocks, or an empty collection if none.
     */
    public @NotNull Collection<Block> getIgnoredBlocks() {
        return ignoredBlocks == null
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(ignoredBlocks);
    }

    /**
     * Sets the blocks that were ignored when creating this grave.
     *
     * @param ignoredBlocks Blocks that were ignored for this grave (may be {@code null} or empty).
     */
    public void setIgnoredBlocks(@Nullable Collection<Block> ignoredBlocks) {
        this.ignoredBlocks = (ignoredBlocks == null)
                ? null
                : new ArrayList<>(ignoredBlocks);
    }

    /**
     * Adds a single block to the ignored blocks list for this grave creation.
     * <p>
     * If the internal collection is {@code null}, it will be created.
     * </p>
     *
     * @param block The block to add to the ignored blocks list (may be {@code null}, in which case this call is a no-op).
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
     * Removes a single block from the ignored blocks list for this grave creation.
     *
     * @param block The block to remove from the ignored blocks list (may be {@code null}, in which case this call is a no-op).
     * @return {@code true} if the block was present and removed; {@code false} otherwise.
     */
    public boolean removeIgnoredBlock(@Nullable Block block) {
        if (block == null || this.ignoredBlocks == null) {
            return false;
        }
        return this.ignoredBlocks.remove(block);
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
