package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Graveyard;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The GraveyardManager class is responsible for managing graveyards.
 */
public final class GraveyardManager {
    private final Graves plugin;
    private final Map<String, Graveyard> graveyardMap;
    private final Map<UUID, Graveyard> modifyingGraveyardMap;

    /**
     * Initializes a new instance of the GraveyardManager class.
     *
     * @param plugin The plugin instance.
     */
    public GraveyardManager(Graves plugin) {
        this.plugin = plugin;
        this.graveyardMap = new HashMap<>();
        this.modifyingGraveyardMap = new HashMap<>();
    }

    /**
     * Unloads the graveyard manager, stopping any players modifying graveyards.
     */
    public void unload() {
        for (Map.Entry<UUID, Graveyard> entry : modifyingGraveyardMap.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());

            if (player != null) {
                stopModifyingGraveyard(player);
            }
        }
    }

    /**
     * Gets a graveyard by its key.
     *
     * @param key The key of the graveyard.
     * @return The graveyard, or null if not found.
     */
    public Graveyard getGraveyardByKey(String key) {
        return graveyardMap.get(key);
    }

    /**
     * Creates a new graveyard.
     *
     * @param location The spawn location of the graveyard.
     * @param name     The name of the graveyard.
     * @param world    The world the graveyard is in.
     * @param type     The type of the graveyard.
     * @return The created graveyard.
     */
    public Graveyard createGraveyard(Location location, String name, World world, Graveyard.Type type) {
        Graveyard graveyard = new Graveyard(name, world, type);
        graveyard.setSpawnLocation(location);
        graveyardMap.put(graveyard.getKey(), graveyard);
        return graveyard;
    }

    /**
     * Adds a location to a graveyard.
     *
     * @param player    The player adding the location.
     * @param location  The location to add.
     * @param graveyard The graveyard to add the location to.
     */
    public void addLocationInGraveyard(Player player, Location location, Graveyard graveyard) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!graveyard.hasGraveLocation(location)) {
                BlockFace blockFace = BlockFaceUtil.getYawBlockFace(player.getLocation().getYaw()).getOppositeFace();
                graveyard.addGraveLocation(location, blockFace);
                previewLocation(player, location, blockFace);
                player.sendMessage("set block in graveyard");
            }
        });
    }

    /**
     * Removes a location from a graveyard.
     *
     * @param player    The player removing the location.
     * @param location  The location to remove.
     * @param graveyard The graveyard to remove the location from.
     */
    public void removeLocationInGraveyard(Player player, Location location, Graveyard graveyard) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (graveyard.hasGraveLocation(location)) {
                graveyard.removeGraveLocation(location);
                refreshLocation(player, location);
                player.sendMessage("remove block in graveyard");
            }
        });
    }

    /**
     * Gets the free spaces in a graveyard.
     *
     * @param graveyard The graveyard to get the free spaces for.
     * @return A map of free locations and their block faces.
     */
    public Map<Location, BlockFace> getGraveyardFreeSpaces(Graveyard graveyard) {
        Map<Location, BlockFace> locationMap = new HashMap<>(graveyard.getGraveLocationMap());
        locationMap.entrySet().removeAll(getGraveyardUsedSpaces(graveyard).entrySet());
        return locationMap;
    }

    /**
     * Gets the used spaces in a graveyard.
     *
     * @param graveyard The graveyard to get the used spaces for.
     * @return A map of used locations and their block faces.
     */
    public Map<Location, BlockFace> getGraveyardUsedSpaces(Graveyard graveyard) {
        Map<Location, BlockFace> locationMap = new HashMap<>();

        for (Map.Entry<Location, BlockFace> entry : graveyard.getGraveLocationMap().entrySet()) {
            if (plugin.getBlockManager().getGraveFromBlock(entry.getKey().getBlock()) != null) {
                locationMap.put(entry.getKey(), entry.getValue());
            }
        }

        return locationMap;
    }

    /**
     * Checks if a player is modifying a graveyard.
     *
     * @param player The player to check.
     * @return True if the player is modifying a graveyard, otherwise false.
     */
    public boolean isModifyingGraveyard(Player player) {
        return modifyingGraveyardMap.containsKey(player.getUniqueId());
    }

    /**
     * Gets the graveyard a player is modifying.
     *
     * @param player The player to get the graveyard for.
     * @return The graveyard the player is modifying, or null if not modifying any graveyard.
     */
    public Graveyard getModifyingGraveyard(Player player) {
        return modifyingGraveyardMap.get(player.getUniqueId());
    }

    /**
     * Starts modifying a graveyard for a player.
     *
     * @param player    The player starting to modify the graveyard.
     * @param graveyard The graveyard to modify.
     */
    public void startModifyingGraveyard(Player player, Graveyard graveyard) {
        if (isModifyingGraveyard(player)) {
            stopModifyingGraveyard(player);
        }

        modifyingGraveyardMap.put(player.getUniqueId(), graveyard);

        for (Map.Entry<Location, BlockFace> entry : graveyard.getGraveLocationMap().entrySet()) {
            previewLocation(player, entry.getKey(), entry.getValue());
        }

        player.sendMessage("starting modifying graveyard " + graveyard.getName());
    }

    /**
     * Stops modifying a graveyard for a player.
     *
     * @param player The player stopping the modification.
     */
    public void stopModifyingGraveyard(Player player) {
        Graveyard graveyard = getModifyingGraveyard(player);

        if (graveyard != null) {
            modifyingGraveyardMap.remove(player.getUniqueId());

            for (Location location : graveyard.getGraveLocationMap().keySet()) {
                refreshLocation(player, location);
            }

            player.sendMessage("stop modifying graveyard " + graveyard.getName());
            // TODO: Save graveyard changes
        }
    }

    /**
     * Checks if a location is inside a graveyard.
     *
     * @param location  The location to check.
     * @param graveyard The graveyard to check against.
     * @return True if the location is inside the graveyard, otherwise false.
     */
    public boolean isLocationInGraveyard(Location location, Graveyard graveyard) {
        switch (graveyard.getType()) {
            case WORLDGUARD:
                return plugin.getIntegrationManager().getWorldGuard() != null
                        && plugin.getIntegrationManager().getWorldGuard().isInsideRegion(location, graveyard.getName());
            case TOWNY:
                return plugin.getIntegrationManager().hasTowny()
                        && plugin.getIntegrationManager().getTowny().isInsidePlot(location, graveyard.getName());
            default:
                return false;
        }
    }

    /**
     * Gets the closest graveyard to a location for an entity.
     *
     * @param location The location to get the closest graveyard for.
     * @param entity   The entity to check.
     * @return The closest graveyard, or null if none found.
     */
    public Graveyard getClosestGraveyard(Location location, Entity entity) {
        Map<Location, Graveyard> locationGraveyardMap = new HashMap<>();

        for (Graveyard graveyard : graveyardMap.values()) {
            if (graveyard.getSpawnLocation() != null) {
                switch (graveyard.getType()) {
                    case WORLDGUARD:
                        if (graveyard.isPublic() || (!(entity instanceof Player)
                                || (plugin.getIntegrationManager().getWorldGuard() != null
                                && plugin.getIntegrationManager().getWorldGuard()
                                .isMember(graveyard.getName(), (Player) entity)))) {
                            locationGraveyardMap.put(graveyard.getSpawnLocation(), graveyard);
                        }

                        break;
                    case TOWNY:
                        if (graveyard.isPublic() || (!(entity instanceof Player)
                                || (plugin.getIntegrationManager().hasTowny()
                                && plugin.getIntegrationManager().getTowny()
                                .isResident(graveyard.getName(), (Player) entity)))) {
                            locationGraveyardMap.put(graveyard.getSpawnLocation(), graveyard);
                        }

                        break;
                }
            }
        }

        return !locationGraveyardMap.isEmpty() ? locationGraveyardMap.get(LocationUtil
                .getClosestLocation(location, new ArrayList<>(locationGraveyardMap.keySet()))) : null;
    }

    /**
     * Previews a location for a player.
     *
     * @param player    The player to preview the location for.
     * @param location  The location to preview.
     * @param blockFace The block face direction.
     */
    private void previewLocation(Player player, Location location, BlockFace blockFace) {
        if (plugin.getIntegrationManager().hasProtocolLib()) {
            plugin.getIntegrationManager().getProtocolLib().setBlock(location.getBlock(), Material.PLAYER_HEAD, player);
        }
    }

    /**
     * Refreshes a location for a player.
     *
     * @param player   The player to refresh the location for.
     * @param location The location to refresh.
     */
    private void refreshLocation(Player player, Location location) {
        if (plugin.getIntegrationManager().hasProtocolLib()) {
            plugin.getIntegrationManager().getProtocolLib().refreshBlock(location.getBlock(), player);
        }
    }
}