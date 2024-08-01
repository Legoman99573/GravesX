package com.ranull.graves.listener.integration.playernpc;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.PlayerNPC;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.UUIDUtil;
import dev.sergiferry.playernpc.api.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listens for NPC interaction events and cancels the event if the player interacts with an NPC associated with a grave.
 */
public class NPCInteractListener implements Listener {
    private final Graves plugin;
    private final PlayerNPC playerNPC;

    /**
     * Constructs a new NPCInteractListener with the specified Graves and PlayerNPC instances.
     *
     * @param plugin    The Graves instance to use.
     * @param playerNPC The PlayerNPC instance to use.
     */
    public NPCInteractListener(Graves plugin, PlayerNPC playerNPC) {
        this.plugin = plugin;
        this.playerNPC = playerNPC;
    }

    /**
     * Handles NPC interaction events. If the player right-clicks an NPC associated with a grave, it cancels the event
     * and opens the grave for the player.
     *
     * @param event The NPC.Events.Interact event to handle.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNPCInteract(NPC.Events.Interact event) {
        if (isRightClick(event)) {
            handleNPCInteraction(event);
        }
    }

    /**
     * Checks if the interaction is a right-click.
     *
     * @param event The NPC.Events.Interact event.
     * @return True if the interaction is a right-click, false otherwise.
     */
    private boolean isRightClick(NPC.Events.Interact event) {
        return event.getClickType() == NPC.Interact.ClickType.RIGHT_CLICK;
    }

    /**
     * Handles the interaction with the NPC. If the NPC is associated with a grave, the event is cancelled
     * and the grave is opened for the player.
     *
     * @param event The NPC.Events.Interact event.
     */
    private void handleNPCInteraction(NPC.Events.Interact event) {
        NPC.Personal npcPersonal = (NPC.Personal) event.getNPC();

        if (npcPersonal.hasGlobal()) {
            NPC.Global npcGlobal = npcPersonal.getGlobal();

            if (npcGlobal.hasCustomData(plugin, "grave_uuid")) {
                UUID uuid = UUIDUtil.getUUID(npcGlobal.getCustomData(plugin, "grave_uuid"));

                if (uuid != null) {
                    openGraveIfExists(event, npcGlobal, uuid);
                }
            }
        }
    }

    /**
     * Opens the grave if it exists in the cache.
     *
     * @param event    The NPC.Events.Interact event.
     * @param npcGlobal The global NPC instance.
     * @param uuid     The UUID of the grave.
     */
    private void openGraveIfExists(NPC.Events.Interact event, NPC.Global npcGlobal, UUID uuid) {
        Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);

        if (grave != null) {
            event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), npcGlobal.getLocation(), grave));
        }
    }
}