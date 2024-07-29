package com.ranull.graves.listener.integration.citizensnpcs;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.CitizensNPC;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.UUIDUtil;
import net.citizensnpcs.api.event.NPCClickEvent;
import net.citizensnpcs.api.npc.NPC;
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
     * Constructs a new NPCInteractListener with the specified Graves and CitizensNPC instances.
     *
     * @param plugin       The Graves instance to use.
     * @param citizensNPC  The CitizensNPC instance to use.
     */
    public CitizensNPCInteractListener(Graves plugin, CitizensNPC citizensNPC) {
        this.plugin = plugin;
        this.citizensNPC = citizensNPC;
    }

    /**
     * Handles NPC interaction events. If the player right-clicks an NPC associated with a grave, it cancels the event
     * and opens the grave for the player.
     *
     * @param event The NPCClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNPCInteract(NPCClickEvent event) {
        NPC npc = event.getNPC();

        if (npc.data().has("grave_uuid")) {
            UUID uuid = UUIDUtil.getUUID(npc.data().get("grave_uuid").toString());

            if (uuid != null) {
                Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);

                if (grave != null) {
                    event.setCancelled(plugin.getGraveManager().openGrave(event.getClicker(), npc.getStoredLocation(), grave));
                }
            }
        }
    }
}