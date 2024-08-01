package com.ranull.graves.integration;

import com.griefdefender.api.Core;
import com.griefdefender.api.Registry;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.data.PlayerData;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionManager;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.registry.CatalogRegistryModule;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Integration with the GriefDefender plugin for managing permissions related to graves.
 */
public final class GriefDefender {
    private final Core core;
    private final Registry registry;
    private final PermissionManager permissionManager;
    private final Flag createFlag;
    private final Flag teleportFlag;

    /**
     * Constructs a GriefDefender instance and registers custom flags.
     */
    public GriefDefender() {
        core = com.griefdefender.api.GriefDefender.getCore();
        registry = com.griefdefender.api.GriefDefender.getRegistry();
        permissionManager = com.griefdefender.api.GriefDefender.getPermissionManager();
        createFlag = buildCreateFlag();
        teleportFlag = buildTeleportFlag();
        Optional<CatalogRegistryModule<Flag>> catalogRegistryModule = registry.getRegistryModuleFor(Flag.class);

        if (catalogRegistryModule.isPresent()) {
            catalogRegistryModule.get().registerCustomType(createFlag);
            catalogRegistryModule.get().registerCustomType(teleportFlag);
        }
    }

    /**
     * Builds the flag for creating graves.
     *
     * @return The creation flag.
     */
    private Flag buildCreateFlag() {
        return Flag.builder()
                .id("graves:graves-create")
                .name("graves-create")
                .permission("griefdefender.flag.graves.graves-create")
                .build();
    }

    /**
     * Builds the flag for teleporting to graves.
     *
     * @return The teleport flag.
     */
    private Flag buildTeleportFlag() {
        return Flag.builder()
                .id("graves:graves-teleport")
                .name("graves-teleport")
                .permission("griefdefender.flag.graves.graves-teleport")
                .build();
    }

    /**
     * Checks if a player has permission to create a grave at a specified location.
     *
     * @param player   The player whose permissions are being checked.
     * @param location The location where the grave would be created.
     * @return True if the player can create a grave, false otherwise.
     */
    public boolean canCreateGrave(Player player, Location location) {
        if (location.getWorld() != null) {
            PlayerData playerData = core.getPlayerData(location.getWorld().getUID(), player.getUniqueId());

            if (playerData != null) {
                Claim claim = core.getClaimAt(location);
                Set<Context> contextSet = new HashSet<>();
                contextSet.add(new Context("graves:graves_create", player.getName()));

                Tristate tristate = permissionManager.getActiveFlagPermissionValue(null, location, claim,
                        playerData.getUser(), createFlag, player, player, contextSet, null, true);

                return tristate == Tristate.TRUE;
            }
        }
        return false;
    }

    /*
    // Commented-out methods for potential future functionality with GriefDefender:

    public boolean canCreateGrave(Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                (RegionAssociable) null, createFlag);
    }

    public boolean canTeleport(Player player, Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player), teleportFlag);
    }

    public boolean canTeleport(Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                (RegionAssociable) null, teleportFlag);
    }
     */

    /**
     * Checks if an entity has permission to teleport to a specific location.
     *
     * @param entity   The entity whose teleportation permission is being checked.
     * @param location The location to which the entity wants to teleport.
     * @return True if the entity can teleport, false otherwise.
     */
    public boolean canTeleport(Entity entity, Location location) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            String worldName = location.getWorld().getName();

            // Check for world-specific teleport permission
            if (player.hasPermission("graves.teleport.world." + worldName)) {
                return true;
            }

            // Check for general teleport permission
            return player.hasPermission("graves.teleport");
        }
        return false;
    }
}