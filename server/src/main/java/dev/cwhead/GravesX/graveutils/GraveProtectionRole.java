package dev.cwhead.GravesX.graveutils;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * Utility class for determining a player's role relative to a grave.
 * <p>
 * Supports both Java and Bedrock players via Floodgate/Floodgate UUID normalization.
 * Provides methods to get the player's role as an enum for internal plugin logic or API usage.
 * </p>
 */
public class GraveProtectionRole {

    private final Graves plugin;

    /**
     * Constructs a new GraveProtectionRole utility with the plugin instance.
     *
     * @param plugin the main Graves plugin instance
     */
    public GraveProtectionRole(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Determines the role of a player for a given grave, using their normalized UUID.
     * <p>
     * This method handles Floodgate UUID normalization if required.
     * The role can be OWNER, KILLER, OTHER, MISSING_OWNER, or MISSING_OTHER.
     * </p>
     *
     * @param grave    the grave to check
     * @param playerId the normalized UUID of the player
     * @return the {@link GraveRole} representing the player's role relative to the grave
     */
    public GraveRole getRole(Grave grave, UUID playerId) {
        UUID ownerId = grave.getOwnerUUID();
        UUID killerId = grave.getKillerUUID();

        if (plugin.getIntegrationManager().hasFloodgate()) {
            var floodgate = plugin.getIntegrationManager().getFloodgate();
            if (ownerId != null && floodgate.isFloodgateId(ownerId)) {
                ownerId = floodgate.getCorrectUniqueId(ownerId);
            }
            if (killerId != null && floodgate.isFloodgateId(killerId)) {
                killerId = floodgate.getCorrectUniqueId(killerId);
            }
        }

        boolean isOwner = Objects.equals(ownerId, playerId);
        boolean isKiller = killerId != null && Objects.equals(killerId, playerId);

        if (isOwner) {
            return killerId == null ? GraveRole.MISSING_OWNER : GraveRole.OWNER;
        } else if (isKiller) {
            return GraveRole.KILLER;
        } else {
            return killerId == null ? GraveRole.MISSING_OTHER : GraveRole.OTHER;
        }
    }


    /**
     * Convenience method to determine a player's role relative to a grave.
     * <p>
     * Automatically normalizes the player's UUID if Floodgate is present.
     * </p>
     *
     * @param player the player to check
     * @param grave  the grave to check
     * @return the {@link GraveRole} representing the player's role relative to the grave
     */
    public GraveRole getRole(Player player, Grave grave) {
        UUID playerId = plugin.getIntegrationManager().hasFloodgate()
                ? plugin.getIntegrationManager().getFloodgate().getNormalizedUUID(player)
                : player.getUniqueId();
        return getRole(grave, playerId);
    }

    /**
     * Enum representing the role of a player relative to a grave.
     */
    public enum GraveRole {
        /**
         * The player is the owner of the grave.
         */
        OWNER,

        /**
         * The player killed the entity that created the grave.
         */
        KILLER,

        /**
         * The player is neither owner nor killer but exists while both IDs are present.
         */
        OTHER,

        /**
         * The player is the owner, but the killer is missing (null).
         */
        MISSING_OWNER,

        /**
         *  Neither owner nor killer exists (null), and the player is another player.
         */
        MISSING_OTHER
    }
}
