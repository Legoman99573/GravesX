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
        if (!isRightClick(event)) {
            return;
        }

        handleNpcInteraction(event);
    }

    /**
     * Checks if the interaction is a right-click.
     *
     * @param event The NpcInteractEvent.
     * @return True if the interaction is a right-click, false otherwise.
     */
    private boolean isRightClick(NpcInteractEvent event) {
        return event.getInteractionType() == ActionTrigger.RIGHT_CLICK;
    }

    /**
     * Handles the interaction with the NPC. If the NPC is associated with a grave,
     * the event is cancelled and the grave is opened for the player.
     *
     * @param event The NpcInteractEvent.
     */
    private void handleNpcInteraction(NpcInteractEvent event) {
        Npc npc = event.getNpc();

        UUID uuid = getGraveUuidFromNpc(npc);
        if (uuid == null) {
            return;
        }

        openGraveIfExists(event, npc, uuid);
    }

    /**
     * Extracts the grave UUID from the NPC.
     *
     * This assumes that the NPC's data ID is set to the grave UUID string when
     * the NPC is created. Adjust this method if you store it differently
     * (e.g. in the name, display name, or a custom scheme).
     *
     * @param npc The FancyNpcs NPC.
     * @return The parsed UUID, or null if it cannot be parsed.
     */
    private UUID getGraveUuidFromNpc(Npc npc) {
        String id = npc.getData().getId(); // e.g. "550e8400-e29b-41d4-a716-446655440000"
        return UUIDUtil.getUUID(id);
    }

    /**
     * Opens the grave if it exists in the cache.
     *
     * @param event The NpcInteractEvent.
     * @param npc   The FancyNpcs NPC.
     * @param uuid  The UUID of the grave.
     */
    private void openGraveIfExists(NpcInteractEvent event, Npc npc, UUID uuid) {
        Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);

        if (grave != null) {
            event.setCancelled(
                    plugin.getGraveManager().openGrave(
                            event.getPlayer(),
                            npc.getData().getLocation(),
                            grave
                    )
            );
        }
    }
}
