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
        if (event.getClickType() == NPC.Interact.ClickType.RIGHT_CLICK) {
            NPC.Personal npcPersonal = (NPC.Personal) event.getNPC();

            if (npcPersonal.hasGlobal()) {
                NPC.Global npcGlobal = npcPersonal.getGlobal();

                if (npcGlobal.hasCustomData(plugin, "grave_uuid")) {
                    UUID uuid = UUIDUtil.getUUID(npcGlobal.getCustomData(plugin, "grave_uuid"));

                    if (uuid != null) {
                        Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);

                        if (grave != null) {
                            event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(),
                                    npcGlobal.getLocation(), grave));
                        }
                    }
                }
            }
        }
    }
}