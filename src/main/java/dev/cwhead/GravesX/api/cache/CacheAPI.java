package dev.cwhead.GravesX.api.cache;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.CacheManager;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Backend-independent access to GravesX data and temporary interaction state.
 */
public final class CacheAPI {
    private final Graves plugin;

    public CacheAPI(@NotNull Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    private CacheManager manager() {
        CacheManager manager = plugin.getCacheManager();
        if (manager == null)
            throw new IllegalStateException("GravesX cache is not initialized");

        return manager;
    }

    public @NotNull CacheMode getMode() {
        return switch (manager().getCacheType()) {
            case NORMAL -> CacheMode.NORMAL;
            case DISK -> CacheMode.DISK;
            case DATABASE -> CacheMode.DATABASE;
        };
    }

    public boolean isPersistentCacheEnabled() {
        return !manager().isCacheDisabled();
    }

    public boolean isDebugEnabled() { return manager().isDebugEnabled(); }

    public @Nullable Grave getGrave(@NotNull UUID graveId) {
        return manager().getGrave(Objects.requireNonNull(graveId, "graveId"));
    }

    public @Nullable Grave getGrave(@NotNull Location location) {
        return manager().getGrave(Objects.requireNonNull(location, "location"));
    }

    public @Nullable Grave getGrave(@NotNull Block block) {
        return getGrave(Objects.requireNonNull(block, "block").getLocation());
    }

    public @NotNull Set<UUID> getGraveIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(manager().getGraveMap().keySet()));
    }

    /**
     * Returns an iterable that resolves the current manager when iteration starts.
     *
     * @return lazily fetched graves
     */
    public @NotNull Iterable<Grave> graves() {
        return () -> manager().graves().iterator();
    }

    /**
     * Persists all supported grave fields and refreshes an existing cache entry.
     *
     * @param grave changed grave
     * @return false if the source row no longer exists; never inserts a missing grave
     *
     * @throws RuntimeException if storage fails
     */
    public boolean saveGrave(@NotNull Grave grave) {
        manager();
        return plugin.getDataManager().saveGrave(Objects.requireNonNull(grave, "grave"));
    }

    public @Nullable EntityData getEntityData(@NotNull UUID entityId) {
        return manager().getEntityData(Objects.requireNonNull(entityId, "entityId"));
    }

    public @Nullable Location getLastLocation(@NotNull UUID entityId) {
        return manager().getLastLocationMap().get(Objects.requireNonNull(entityId, "entityId"));
    }

    public void setLastLocation(@NotNull UUID entityId, @NotNull Location location) {
        manager().getLastLocationMap().put(Objects.requireNonNull(entityId, "entityId"), Objects.requireNonNull(location, "location"));
    }

    public void removeLastLocation(@NotNull UUID entityId) {
        manager().getLastLocationMap().remove(Objects.requireNonNull(entityId, "entityId"));
    }

    public @Nullable List<ItemStack> getRemovedItems(@NotNull UUID entityId) {
        List<ItemStack> items = manager().getRemovedItemStackMap().get(Objects.requireNonNull(entityId, "entityId"));
        return items == null ? null : new ArrayList<>(items);
    }

    public void setRemovedItems(@NotNull UUID entityId, @NotNull List<ItemStack> items) {
        manager().getRemovedItemStackMap().put(Objects.requireNonNull(entityId, "entityId"),
                new ArrayList<>(Objects.requireNonNull(items, "items")));
    }

    public void removeRemovedItems(@NotNull UUID entityId) {
        manager().getRemovedItemStackMap().remove(Objects.requireNonNull(entityId, "entityId"));
    }

    public @Nullable Location getRightClickedBlock(@NotNull String playerName) {
        return manager().getRightClickedBlock(Objects.requireNonNull(playerName, "playerName"));
    }

    public void setRightClickedBlock(@NotNull String playerName, @NotNull Location location) {
        manager().addRightClickedBlock(Objects.requireNonNull(playerName, "playerName"), Objects.requireNonNull(location, "location"));
    }

    public void removeRightClickedBlock(@NotNull String playerName, @NotNull Location location) {
        manager().removeRightClickedBlock(Objects.requireNonNull(playerName, "playerName"), Objects.requireNonNull(location, "location"));
    }

    public boolean startViewingGrave(@NotNull UUID graveId, @NotNull UUID viewerId) {
        return manager().startViewingGrave(Objects.requireNonNull(graveId, "graveId"), Objects.requireNonNull(viewerId, "viewerId"));
    }

    public void stopViewingGrave(@NotNull UUID graveId, @NotNull UUID viewerId) {
        manager().stopViewingGrave(Objects.requireNonNull(graveId, "graveId"), Objects.requireNonNull(viewerId, "viewerId"));
    }

    public boolean canAccessGrave(@NotNull UUID graveId, @NotNull UUID viewerId) {
        return manager().canAccessGrave(Objects.requireNonNull(graveId, "graveId"), Objects.requireNonNull(viewerId, "viewerId"));
    }

    public boolean isGraveBeingViewed(@NotNull UUID graveId) {
        return manager().isGraveBeingViewed(Objects.requireNonNull(graveId, "graveId"));
    }

    public @Nullable UUID getGraveViewer(@NotNull UUID graveId) {
        return manager().getGraveViewer(Objects.requireNonNull(graveId, "graveId"));
    }

    public void clearGraveViewer(@NotNull UUID graveId) {
        manager().clearGraveViewer(Objects.requireNonNull(graveId, "graveId"));
    }

    public void clearAllGraveViewersFor(@NotNull UUID viewerId) {
        manager().clearAllGraveViewersFor(Objects.requireNonNull(viewerId, "viewerId"));
    }
}
