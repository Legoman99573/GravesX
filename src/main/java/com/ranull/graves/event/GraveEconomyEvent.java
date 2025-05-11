package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveEconomyEvent extends GraveEvent {
    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The amount of currency to be deducted from the player's balance in the event.
     * This value is represented as a {@link Double} and can either be a fixed amount or
     * a percentage of the player's balance, depending on the configuration.
     * <p>
     * The economy amount is used during the grave economy process to determine how much
     * money should be withdrawn from the player's account.
     * </p>
     */
    private double economyAmount;

    /**
     * Constructs a new {@code GraveEconomyEvent}.
     *
     * @param grave         The grave associated with the event.
     * @param player        The player associated with the event, if any.
     */
    public GraveEconomyEvent(Grave grave,
                             @NotNull Player player) {
        super(grave, null, null, null, null, null, null, null, player);
    }

    /**
     * Gets the amount of money or percentage to be deducted from the player's balance.
     * This value represents the economy amount that will be deducted as part of the
     * grave economy event, and can either be a fixed amount or a percentage of the
     * player's balance, depending on configuration.
     *
     * @return the amount (in currency) or percentage to be deducted from the player's balance.
     */
    public double getEconomyAmount() {
        return economyAmount;
    }

    /**
     * Sets the amount of money or percentage to be deducted from the player's balance.
     * This value defines how much money will be deducted, which could either be a
     * fixed amount or a percentage of the player's balance. The deduction amount
     * is applied during the grave economy process based on the configuration.
     *
     * @param economyAmount the new amount (in currency) or percentage to be deducted.
     */
    public void setEconomyAmount(double economyAmount) {
        this.economyAmount = economyAmount;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
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
}
