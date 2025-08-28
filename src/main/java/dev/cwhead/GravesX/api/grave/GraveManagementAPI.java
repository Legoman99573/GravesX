package dev.cwhead.GravesX.api.grave;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * API for managing existing graves.
 */
public final class GraveManagementAPI {
    private final Graves plugin;

    public GraveManagementAPI(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Removes the specified grave from the grave manager.
     *
     * @param grave the grave to be removed
     */
    public void removeGrave(@NotNull Grave grave) {
        plugin.getGraveManager().removeGrave(grave);
    }

    /**
     * Breaks the specified grave, triggering its removal and handling any related events.
     *
     * @param grave the grave to be broken
     */
    public void breakGrave(@NotNull Grave grave) {
        plugin.getGraveManager().breakGrave(grave);
    }

    /**
     * Breaks the specified grave at a given location.
     *
     * @param location the location where the grave is located
     * @param grave    the grave to be broken
     */
    public void breakGrave(@NotNull Location location, @NotNull Grave grave) {
        plugin.getGraveManager().breakGrave(location, grave);
    }

    /**
     * Automatically loots the specified grave for the given entity at the given location.
     *
     * @param entity   the entity that will loot the grave
     * @param location the location of the grave
     * @param grave    the grave to be looted
     */
    public void autoLootGrave(@NotNull Entity entity, @NotNull Location location, @NotNull Grave grave) {
        plugin.getGraveManager().autoLootGrave(entity, location, grave);
    }

    /**
     * Marks the specified grave as abandoned, preventing further interaction.
     *
     * @param grave the grave to be abandoned
     */
    public void abandonGrave(@NotNull Grave grave) {
        plugin.getGraveManager().abandonGrave(grave);
    }

    /**
     * Drops the items stored in the specified grave at the given location.
     *
     * @param location the location where the items will be dropped
     * @param grave    the grave whose items are to be dropped
     */
    public void dropGraveItems(@NotNull Location location, @NotNull Grave grave) {
        plugin.getGraveManager().dropGraveItems(location, grave);
    }

    /**
     * Removes the oldest grave associated with the specified living entity.
     *
     * @param livingEntity the entity whose oldest grave will be removed
     */
    public void removeOldestGrave(@NotNull LivingEntity livingEntity) {
        plugin.getGraveManager().removeOldestGrave(livingEntity);
    }

    /**
     * Determines if the specified location is near a grave.
     * <p>
     * This method serves as an overload to allow optional parameters such as a player
     * or a block to be included in the proximity check.
     *
     * @param location the location to check for nearby graves (required).
     * @param player   the player to consider in the proximity check (optional; nullable).
     * @param block    the block to consider in the proximity check (optional; nullable).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     */
    public boolean isNearGrave(@NotNull Location location, @Nullable Player player, @Nullable Block block) {
        return plugin.getGraveManager().isNearGrave(location, player, block);
    }

    /**
     * Determines if the specified location is near a grave.
     * <p>
     * This variant of the method omits the player and block parameters.
     *
     * @param location the location to check for nearby graves (required).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     */
    public boolean isNearGrave(@NotNull Location location) {
        return isNearGrave(location, null, null);
    }

    /**
     * Determines if the specified location is near a grave, considering a specific player.
     * <p>
     * This variant of the method includes the player parameter but omits the block parameter.
     *
     * @param location the location to check for nearby graves (required).
     * @param player   the player to consider in the proximity check (required).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     */
    public boolean isNearGrave(@NotNull Location location, @NotNull Player player) {
        return isNearGrave(location, player, null);
    }

    /**
     * Determines if the specified location is near a grave, considering a specific block.
     * <p>
     * This variant of the method includes the block parameter but omits the player parameter.
     *
     * @param location the location to check for nearby graves (required).
     * @param block    the block to consider in the proximity check (required).
     * @return {@code true} if the location is near a grave, otherwise {@code false}.
     */
    public boolean isNearGrave(@NotNull Location location, @NotNull Block block) {
        return isNearGrave(location, null, block);
    }

    /**
     * Gets the grave type
     *
     * @param uuid the uuid of the grave
     */
    public Grave getGrave(@NotNull UUID uuid) {
        return new Grave(uuid);
    }

    /**
     * @deprecated Use {@link #isGrave(Grave, Location)} instead for precise location checking.
     * This code is added for debugging purposes.
     *
     * Checks if the specified location is a grave's location.
     *
     * @param grave the grave to check. This always returns true for the provided grave's death location.
     *              For more precise checking, use {@link #isGrave(Grave, Location)} with a specific location.
     * @return true if the location matches the grave's death location, false otherwise.
     */
    @Deprecated
    public boolean isGrave(@NotNull Grave grave) {
        return isGrave(grave, grave.getLocationDeath());
    }

    /**
     * Checks if a given location matches the death location of a specific grave.
     *
     * @param grave    the grave to check
     * @param location the location to compare with the grave's death location
     * @return true if the location matches the grave's death location, false otherwise.
     */
    public boolean isGrave(@NotNull Grave grave, @NotNull Location location) {
        return location.equals(grave.getLocationDeath());
    }

    /**
     * Returns the total number of graves for all players.
     * <p>
     * This method calls {@link #getGraveAmount(Player)} with a {@code null} argument
     * to count graves without filtering by any specific player.
     *
     * @return the total count of graves for all players.
     */
    public long getGraveAmount() {
        return getGraveAmount(null);
    }

    /**
     * Returns the number of graves associated with a specified player.
     * <p>
     * If {@code targetPlayer} is provided, only graves owned by this player will be counted.
     * If {@code targetPlayer} is {@code null}, all graves are counted.
     *
     * @param targetPlayer the player whose graves should be counted; if {@code null},
     *                     counts graves for all players.
     * @return the number of graves associated with {@code targetPlayer}, or the total
     *         count of all graves if {@code targetPlayer} is {@code null}.
     */
    public long getGraveAmount(@Nullable Player targetPlayer) {
        List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());
        if (targetPlayer == null) return graveList.size();
        UUID playerUUID = targetPlayer.getUniqueId();
        long count = 0;
        for (Grave g : graveList) {
            if (playerUUID.equals(g.getOwnerUUID())) count++;
        }
        return count;
    }
}
