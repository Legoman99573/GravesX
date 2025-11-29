package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an entity (usually a player) takes an item from a grave.
 * Extends {@link GraveEntityEvent} to provide entity/grave context.
 */
public class GraveItemTakeEvent extends GraveEntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull ItemStack item;
    private final @NotNull InventoryAction action;

    /**
     * Constructs a new GraveItemTakeEvent.
     *
     * @param grave  The grave inventory being interacted with
     * @param player The player taking the item
     * @param item   The item being taken
     * @param action The inventory action performed
     */
    public GraveItemTakeEvent(@NotNull Grave grave,
                              @NotNull Player player,
                              @NotNull ItemStack item,
                              @NotNull InventoryAction action) {
        super(grave, player, grave.getLocationDeath(), null, null, player, null);
        this.item = item;
        this.action = action;
    }

    /**
     * Gets the item being taken from the grave.
     *
     * @return The {@link ItemStack} being taken
     */
    public @NotNull ItemStack getItem() {
        return item;
    }

    /**
     * Gets the inventory action performed.
     *
     * @return The {@link InventoryAction} taken by the entity
     */
    public @NotNull InventoryAction getAction() {
        return action;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
