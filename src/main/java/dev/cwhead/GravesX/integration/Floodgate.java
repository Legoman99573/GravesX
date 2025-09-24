package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Integration class for the Floodgate API.
 * <p>
 * Handles detection and interaction with Bedrock players
 * connecting via Geyser and Floodgate on Java servers.
 */
public class Floodgate {

    private final Graves plugin;

    /** Reference to the Floodgate API. */
    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    public Floodgate(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the given player is a Bedrock (Floodgate) player.
     *
     * @param player the player to check
     * @return true if the player is a Bedrock client, false otherwise
     */
    public boolean isBedrockPlayer(Player player) {
        return floodgateApi.isFloodgatePlayer(player.getUniqueId());
    }

    /**
     * Checks if the given player is identified by a Floodgate-specific UUID.
     *
     * @param player the player to check
     * @return true if the player's UUID is a Floodgate ID, false otherwise
     */
    public boolean isFloodgateId(Player player) {
        return isFloodgateId(player.getUniqueId());
    }

    /**
     * Checks if the given UUID is a Floodgate-specific ID.
     *
     * @param uuid the UUID to check
     * @return true if the UUID belongs to a Bedrock player, false otherwise
     */
    public boolean isFloodgateId(UUID uuid) {
        return floodgateApi.isFloodgateId(uuid);
    }

    /**
     * Retrieves the Floodgate player instance for the given player.
     *
     * @param player the Bukkit player
     * @return the Floodgate player instance, or null if not a Bedrock player
     */
    public FloodgatePlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Retrieves the Floodgate player instance for the given UUID.
     *
     * @param uuid the UUID of the player
     * @return the Floodgate player instance, or null if not found
     */
    public FloodgatePlayer getPlayer(UUID uuid) {
        return floodgateApi.getPlayer(uuid);
    }

    /**
     * Gets a normalized UUID for comparison across Java and Bedrock players.
     * <p>
     * If the player is a Bedrock client, this returns the Java-style UUID
     * Floodgate maps them to. Otherwise, returns their Bukkit UUID.
     *
     * @param player the player
     * @return a UUID that can be used for consistent ownership checks
     */
    public UUID getNormalizedUUID(Player player) {
        if (isBedrockPlayer(player)) {
            plugin.debugMessage(player + " detected as bedrock player", 3);
            return Optional.ofNullable(getPlayer(player.getUniqueId()))
                    .map(FloodgatePlayer::getCorrectUniqueId)
                    .orElse(player.getUniqueId());
        }
        plugin.debugMessage(player + " detected as java player", 3);
        return player.getUniqueId();
    }

    /**
     * Gets the Java-style username for a player.
     * <p>
     * For Bedrock players, this will return the username Floodgate assigns for use in Java context.
     * For Java players, this returns the actual Bukkit name.
     *
     * <strong>⚠️ DEPRECATED: Do not use usernames for identity comparisons.</strong><br>
     * Usernames are not unique or stable across both Java and Bedrock. Use {@link #getNormalizedUUID(Player)} instead.
     *
     * @param player the player
     * @return the correct Java-style username for display purposes only
     * @deprecated Use UUIDs for any identity-related logic. This method should only be used for display/UI.
     */
    @Deprecated
    public String getCorrectUsername(Player player) {
        if (isBedrockPlayer(player)) {
            return Optional.ofNullable(getPlayer(player.getUniqueId()))
                    .map(FloodgatePlayer::getJavaUsername)
                    .orElse(player.getName());
        }
        return player.getName();
    }

    /**
     * Retrieves the normalized (Java-style) UUID for a given player.
     * <p>
     * If the player is a Bedrock client connecting through Floodgate,
     * this returns the UUID that Floodgate maps to for consistent identity tracking.
     * Otherwise, returns the player's original Bukkit UUID.
     *
     * @param player the player to normalize
     * @return the correct UUID representing the player in Java context
     */
    public UUID getCorrectUniqueId(Player player) {
        return getCorrectUniqueId(player.getUniqueId());
    }

    /**
     * Gets the Java-corrected unique UUID for the given UUID.
     * <p>
     * If the UUID belongs to a Floodgate (Bedrock) player, this will return the mapped Java UUID.
     * If not, the original UUID is returned.
     *
     * @param uuid the UUID to normalize
     * @return the Java UUID if Bedrock, or the original UUID
     */
    public UUID getCorrectUniqueId(UUID uuid) {
        if (isFloodgateId(uuid)) {
            FloodgatePlayer fgPlayer = getPlayer(uuid);
            if (fgPlayer != null) {
                return fgPlayer.getCorrectUniqueId();
            }
        }
        return uuid;
    }
}