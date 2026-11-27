package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * Event triggered when a player opens a grave inventory.
 * <p>
 * Now extends {@link GraveEntityEvent}, so it supports full entity functionality.
 * Still only used for players opening graves, but can leverage entity accessors.
 * </p>
 */
public class GraveOpenEvent extends GraveEntityEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull InventoryView view;
    private boolean cancelled;

    /**
     * Constructs a new GraveOpenEvent for a player opening a grave.
     *
     * @param view  The inventory view being opened.
     * @param grave The grave being opened.
     * @param player The player opening the grave.
     */
    public GraveOpenEvent(@NotNull InventoryView view, @NotNull Grave grave, @NotNull Player player) {
        super(grave, player, grave.getLocationDeath(), null, null, null, null);
        this.view = view;
        this.cancelled = false;
    }

    /**
     * Gets the inventory view being opened.
     *
     * @return The inventory view.
     */
    public @NotNull InventoryView getView() {
        return view;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static handler list for Bukkit event registration.
     *
     * @return HandlerList
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Convenience getter for the player.
     *
     * @return The player opening the grave.
     */
    @NotNull
    public Player getPlayer() {
        return (Player) getEntity();
    }
}