package me.jay.GravesX.listener.integration.fancynpcs;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.UUIDUtil;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listens for FancyNpcs interaction events and cancels the event if the player
 * interacts with an NPC associated with a grave.
 */
public class NpcInteractListener implements Listener {

    private final Graves plugin;

    /**
     * Constructs a new NpcInteractListener with the specified Graves instance.
     *
     * @param plugin The Graves instance to use.
     */
    public NpcInteractListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles NPC interaction events. If the player right-clicks an NPC associated
     * with a grave, it cancels the event and opens the grave for the player.
     *
     * @param event The NpcInteractEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNpcInteraction(NpcInteractEvent event) {
        if (event.getInteractionType() != ActionTrigger.RIGHT_CLICK) {
            plugin.debugMessage("Interaction for FancyNpcs npc was not a right-click.", 2);
            return;
        }

        Npc npc = event.getNpc();
        if (npc.getData() == null) {
            plugin.debugMessage("NPC Data for FancyNpcs npc returned null.", 2);
            return;
        }

        String rawId = npc.getData().getName();
        if (rawId == null || rawId.isEmpty()) {
            plugin.debugMessage("NPC name/id for FancyNpcs npc returned null or empty.", 2);
            return;
        }

        UUID graveUuid = UUIDUtil.getUUID(rawId);
        if (graveUuid == null) {
            plugin.debugMessage("NPC UUID Data for FancyNpcs npc returned null.", 2);
            return;
        }

        Grave grave = plugin.getCacheManager().getGraveMap().get(graveUuid);

        if (grave != null) {
            if (grave.getUUID().equals(graveUuid)) {
                plugin.getGraveManager().openGrave(event.getPlayer(), npc.getData().getLocation(), grave);
                plugin.debugMessage("FancyNpcs npc is a grave. Opening GUI for " + event.getPlayer().getDisplayName() + ".", 2);
                event.setCancelled(true);
            } else {
                plugin.debugMessage("FancyNpcs npc UUID did not match Grave UUID.", 2);
            }
        } else {
            plugin.debugMessage("FancyNpcs npc is not a grave. Ignoring...", 2);
        }
    }

}
