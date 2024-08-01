package com.ranull.graves.listener.integration.towny;

import com.palmergames.bukkit.towny.event.TownBlockTypeRegisterEvent;
import com.ranull.graves.integration.Towny;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for TownBlockTypeRegisterEvent to register the graveyard block type in Towny.
 */
public class TownBlockTypeRegisterListener implements Listener {
    private final Towny towny;

    /**
     * Constructs a new TownBlockTypeRegisterListener with the specified Towny instance.
     *
     * @param towny The Towny instance to use.
     */
    public TownBlockTypeRegisterListener(Towny towny) {
        this.towny = towny;
    }

    /**
     * Handles TownBlockTypeRegisterEvent. Registers the graveyard block type with Towny.
     *
     * @param event The TownBlockTypeRegisterEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTownBlockTypeRegister(TownBlockTypeRegisterEvent event) {
        towny.registerGraveyardBlockType();
    }
}