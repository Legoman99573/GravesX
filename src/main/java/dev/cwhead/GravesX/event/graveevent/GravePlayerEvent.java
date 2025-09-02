package dev.cwhead.GravesX.event.graveevent;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.interfaces.Addon;
import dev.cwhead.GravesX.exception.GravesXEventMethodNotSupportedException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Player-based grave event.
 * <p>
 * This subclass provides accessors specific to {@link Player}, including player identity
 * and any {@link InventoryView} involved.
 * </p>
 */
public class GravePlayerEvent extends GraveEntityEvent implements Cancellable, Addon {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The inventory view associated with the event.
     * <p>
     * This {@link InventoryView} represents the view of the inventory related to the event, such as a player's inventory or a chest.
     * </p>
     */
    private final @Nullable InventoryView inventoryView;

    /**
     * The player associated with the event.
     * <p>
     * This {@link Player} represents the player involved in or affected by the event.
     * </p>
     */
    private final @NotNull Player player;

    /**
     * Constructs a new {@code GravePlayerEvent}.
     *
     * @param grave         The grave associated with the event.
     * @param player        The player involved in the event (non-null).
     * @param location      The location of the event.
     * @param blockType     The type of block involved in the event, if any.
     * @param block         The block involved in the event, if any.
     * @param inventoryView The inventory view associated with the event, if any.
     * @param livingEntity  The living entity associated with the event, if any (usually same as player).
     * @param targetEntity  The entity targeted by the event, if any.
     */
    public GravePlayerEvent(@NotNull Grave grave, @NotNull Player player, @Nullable Location location, @Nullable BlockData.BlockType blockType, @Nullable org.bukkit.block.Block block, @Nullable InventoryView inventoryView, @Nullable org.bukkit.entity.LivingEntity livingEntity, @Nullable org.bukkit.entity.LivingEntity targetEntity) {
        super(grave, player, location, blockType, block, livingEntity, targetEntity);
        this.player = Objects.requireNonNull(player, "player");
        this.inventoryView = inventoryView;
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
            throw new GravesXEventMethodNotSupportedException("This event does not support InventoryView access.");
        return inventoryView;
    }

    /**
     * Checks the player involved in the event.
     *
     * @return The player involved in the event, or null if not applicable.
     */
    public boolean hasPlayer() {
        return true;
    }

    /**
     * Gets the player involved in the event.
     *
     * @return The player involved in the event, or null if not applicable.
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Gets the player name involved in the event.
     *
     * @return The player name involved in the event, or null if not applicable.
     */
    public @NotNull String getPlayerName() {
        return player.getName();
    }

    /**
     * Gets the player unique ID involved in the event.
     *
     * @return The player unique ID involved in the event, or null if not applicable.
     */
    public @NotNull UUID getPlayerUniqueId() {
        return player.getUniqueId();
    }

    /**
     * Gets the player display name involved in the event.
     *
     * @return The player display name involved in the event, or null if not applicable.
     */
    public @NotNull String getPlayerDisplayName() {
        return player.getDisplayName();
    }

    @Override public boolean isEntityActuallyPlayer() {
        return true;
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
