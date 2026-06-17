package dev.cwhead.GravesX.api.grave;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXIllegalArgumentException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Objects;

/**
 * API for managing existing graves.
 */
public class GraveManagementAPI {
    private final Graves plugin;

    public GraveManagementAPI(Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
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

    /**
     * Gets all loaded grave UUIDs.
     *
     * @return a list of all loaded grave UUIDs.
     */
    public @NotNull List<UUID> getGraveUUIDs() {
        return getGraveUUIDs((UUID) null);
    }

    /**
     * Gets loaded grave UUIDs owned by the specified player.
     *
     * @param targetPlayer the player whose grave UUIDs should be returned; if {@code null}, all grave UUIDs are returned.
     * @return a list of loaded grave UUIDs.
     */
    public @NotNull List<UUID> getGraveUUIDs(@Nullable Player targetPlayer) {
        return getGraveUUIDs(targetPlayer != null ? targetPlayer.getUniqueId() : null);
    }

    /**
     * Gets loaded grave UUIDs owned by the specified owner UUID.
     *
     * @param ownerUUID the owner UUID whose grave UUIDs should be returned; if {@code null}, all grave UUIDs are returned.
     * @return a list of loaded grave UUIDs.
     */
    public @NotNull List<UUID> getGraveUUIDs(@Nullable UUID ownerUUID) {
        List<UUID> graveUUIDs = new ArrayList<>();

        for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
            if (ownerUUID == null || ownerUUID.equals(grave.getOwnerUUID())) {
                graveUUIDs.add(grave.getUUID());
            }
        }

        return graveUUIDs;
    }

    /**
     * Gets loaded graves owned by the specified player.
     *
     * @param targetPlayer the player whose graves should be returned; if {@code null}, all graves are returned.
     * @return a list of loaded graves.
     */
    public @NotNull List<Grave> getGraves(@Nullable Player targetPlayer) {
        return getGraves(targetPlayer != null ? targetPlayer.getUniqueId() : null);
    }

    /**
     * Gets loaded graves owned by the specified owner UUID.
     *
     * @param ownerUUID the owner UUID whose graves should be returned; if {@code null}, all graves are returned.
     * @return a list of loaded graves.
     */
    public @NotNull List<Grave> getGraves(@Nullable UUID ownerUUID) {
        List<Grave> graves = new ArrayList<>();

        for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
            if (ownerUUID == null || ownerUUID.equals(grave.getOwnerUUID())) {
                graves.add(grave);
            }
        }

        return graves;
    }

    /**
     * Checks whether the specified grave is currently locked (being viewed) by any player.
     *
     * @param grave the grave to check
     * @return {@code true} if the grave is currently locked/in-use, otherwise {@code false}
     */
    public boolean isGraveLocked(@NotNull Grave grave) {
        UUID graveUUID = grave.getUUID();
        return plugin.getCacheManager().isGraveBeingViewed(graveUUID);
    }

    /**
     * Checks whether the specified grave is locked by someone other than the provided player.
     *
     * @param grave  the grave to check
     * @param player the player attempting access
     * @return {@code true} if the grave is locked by another player, otherwise {@code false}
     */
    public boolean isGraveLocked(@NotNull Grave grave, @NotNull Player player) {
        UUID graveUUID = grave.getUUID();
        return !plugin.getCacheManager().canAccessGrave(graveUUID, player.getUniqueId());
    }

    /**
     * Gets the UUID of the player currently viewing (locking) the specified grave.
     *
     * @param grave the grave to check
     * @return the viewer's UUID if the grave is locked, or {@code null} if not locked
     */
    public @Nullable UUID getGraveViewerUUID(@NotNull Grave grave) {
        UUID graveUUID = grave.getUUID();
        return plugin.getCacheManager().getGraveViewer(graveUUID);
    }

    /**
     * Attempts to get the UUID of the player currently viewing (locking) a grave.
     *
     * <p>If the grave is locked, the viewer UUID is written to {@code outViewer[0]}
     * and this method returns {@code true}. If the grave is not locked, this method
     * returns {@code false} and {@code outViewer} is unchanged.</p>
     *
     * @param grave     the grave to check
     * @param outViewer output array to receive the viewer UUID in {@code outViewer[0]} (length &gt;= 1)
     * @return {@code true} if the grave is locked, otherwise {@code false}
     * @throws GravesXIllegalArgumentException if {@code outViewer.length == 0}
     */
    public boolean tryGetGraveViewerUUID(@NotNull Grave grave, @NotNull UUID[] outViewer) {
        if (outViewer.length == 0) {
            throw new GravesXIllegalArgumentException("outViewer must have length >= 1");
        }

        UUID viewer = getGraveViewerUUID(grave);
        if (viewer == null) return false;

        outViewer[0] = viewer;
        return true;
    }

    /**
     * Updates the protection state and remaining protection time for an existing grave.
     *
     * <p>This intentionally lives in the grave management API so Skript support does not
     * modify grave internals directly.</p>
     *
     * @param grave the grave to update
     * @param protectedGrave whether the grave should be protected
     * @param timeProtection the remaining protection time in milliseconds; ignored when protection is disabled
     */
    public void setGraveProtection(@NotNull Grave grave, boolean protectedGrave, long timeProtection) {
        grave.setProtection(protectedGrave);
        grave.setTimeProtection(protectedGrave ? Math.max(timeProtection, 0L) : 0L);
    }

    /**
     * Clears protection from an existing grave.
     *
     * @param grave the grave to update
     */
    public void clearGraveProtection(@NotNull Grave grave) {
        setGraveProtection(grave, false, 0L);
    }


    /**
     * Gets the next empty inventory slot for the specified grave.
     *
     * @param grave the grave to inspect
     * @return the next empty slot index, or {@code -1} if no empty slot exists
     */
    public int getNextAvailableGraveSlot(@NotNull Grave grave) {
        Inventory inventory = grave.getInventory();
        if (inventory == null) {
            return -1;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isEmptyItem(inventory.getItem(slot))) {
                return slot;
            }
        }

        return -1;
    }

    /**
     * Checks whether the specified grave has at least one empty inventory slot.
     *
     * @param grave the grave to inspect
     * @return {@code true} if the grave has an empty slot, otherwise {@code false}
     */
    public boolean hasAvailableGraveSlot(@NotNull Grave grave) {
        return getNextAvailableGraveSlot(grave) >= 0;
    }

    /**
     * Adds an item to a specific grave inventory slot if that slot is empty.
     *
     * <p>Slot numbers are raw Bukkit inventory slot indexes. The first slot is {@code 0}.</p>
     *
     * @param grave the grave to update
     * @param itemStack the item to add
     * @param slot the target slot index
     * @return {@code true} if the item was added, otherwise {@code false}
     */
    public boolean addItemToGraveSlot(@NotNull Grave grave, @NotNull ItemStack itemStack, int slot) {
        Inventory inventory = grave.getInventory();
        if (!canUseItem(itemStack) || inventory == null || !isValidSlot(inventory, slot) || !isEmptyItem(inventory.getItem(slot))) {
            return false;
        }

        inventory.setItem(slot, itemStack.clone());
        return true;
    }

    /**
     * Sets an item in a specific grave inventory slot, replacing any existing item in that slot.
     *
     * <p>Slot numbers are raw Bukkit inventory slot indexes. The first slot is {@code 0}.</p>
     *
     * @param grave the grave to update
     * @param itemStack the item to set
     * @param slot the target slot index
     * @return {@code true} if the item was set, otherwise {@code false}
     */
    public boolean setItemInGraveSlot(@NotNull Grave grave, @NotNull ItemStack itemStack, int slot) {
        Inventory inventory = grave.getInventory();
        if (!canUseItem(itemStack) || inventory == null || !isValidSlot(inventory, slot)) {
            return false;
        }

        inventory.setItem(slot, itemStack.clone());
        return true;
    }

    /**
     * Adds an item to the next empty grave inventory slot.
     *
     * @param grave the grave to update
     * @param itemStack the item to add
     * @return the slot index that received the item, or {@code -1} if no slot was available
     */
    public int addItemToNextAvailableGraveSlot(@NotNull Grave grave, @NotNull ItemStack itemStack) {
        int slot = getNextAvailableGraveSlot(grave);
        if (slot < 0) {
            return -1;
        }

        return addItemToGraveSlot(grave, itemStack, slot) ? slot : -1;
    }

    private boolean isValidSlot(@NotNull Inventory inventory, int slot) {
        return slot >= 0 && slot < inventory.getSize();
    }

    private boolean canUseItem(@NotNull ItemStack itemStack) {
        return itemStack.getType() != Material.AIR && itemStack.getAmount() > 0;
    }

    private boolean isEmptyItem(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
    }

}