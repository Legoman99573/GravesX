package com.ranull.graves.listener.integration.citizensnpcs;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.CitizensNPC;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.UUIDUtil;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listener class for handling NPC interactions with Citizens2.
 */
public class CitizensNPCInteractListener implements Listener {
    private final Graves plugin;
    private final CitizensNPC citizensNPC;

    /**
     * Constructs a new CitizensNPCInteractListener with the specified Graves and CitizensNPC instances.
     *
     * @param plugin      The Graves instance to use.
     * @param citizensNPC The CitizensNPC instance to use.
     */
    public CitizensNPCInteractListener(Graves plugin, CitizensNPC citizensNPC) {
        this.plugin = plugin;
        this.citizensNPC = citizensNPC;
    }

    /**
     * Handles NPC left-click interaction events. If the player left-clicks an NPC associated with a grave,
     * it cancels the event and opens the grave for the player.
     *
     * @param event The NPCLeftClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNPCLeftClick(NPCLeftClickEvent event) {
        handleNPCInteraction(event.getNPC(), event.getClicker());
    }

    /**
     * Handles NPC right-click interaction events. If the player right-clicks an NPC associated with a grave,
     * it cancels the event and opens the grave for the player.
     *
     * @param event The NPCRightClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNPCRightClick(NPCRightClickEvent event) {
        handleNPCInteraction(event.getNPC(), event.getClicker());
    }

    /**
     * Handles the interaction with the NPC. If the NPC is associated with a grave,
     * cancels the event and opens the grave for the player.
     *
     * @param npc     The NPC being interacted with.
     * @param player  The player interacting with the NPC.
     */
    private void handleNPCInteraction(NPC npc, Player player) {
        if (npc.data().has("grave_uuid")) {
            UUID uuid = UUIDUtil.getUUID(npc.data().get("grave_uuid").toString());

            if (uuid != null) {
                Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);

                if (grave != null) {
                    plugin.getGraveManager().openGrave(player, npc.getStoredLocation(), grave);
                }
            }
        }
    }
}