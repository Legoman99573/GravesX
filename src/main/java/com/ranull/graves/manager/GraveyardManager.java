package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Graveyard;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class GraveyardManager {
    private final Graves plugin;
    private final Map<String, Graveyard> graveyardMap;
    private final Map<UUID, Graveyard> modifyingGraveyardMap;

    public GraveyardManager(Graves plugin) {
        this.plugin = plugin;
        this.graveyardMap = new HashMap<>();
        this.modifyingGraveyardMap = new HashMap<>();
    }

    public void unload() throws InvocationTargetException {
        for (Map.Entry<UUID, Graveyard> entry : modifyingGraveyardMap.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());

            if (player != null) {
                stopModifyingGraveyard(player);
            }
        }
    }

    public Graveyard getGraveyardByKey(String key) {
        return graveyardMap.get(key);
    }

    public Graveyard createGraveyard(Location location, String name, World world, Graveyard.Type type) {
        Graveyard graveyard = new Graveyard(name, world, type);
        graveyard.setSpawnLocation(location);
        graveyardMap.put(graveyard.getKey(), graveyard);
        return graveyard;
    }

    public void addLocationInGraveyard(Player player, Location location, Graveyard graveyard) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!graveyard.hasGraveLocation(location)) {
                BlockFace blockFace = BlockFaceUtil.getYawBlockFace(player.getLocation().getYaw()).getOppositeFace();
                graveyard.addGraveLocation(location, blockFace);
                plugin.getDataManager().updateGraveyardLocationData(graveyard);
                previewLocation(player, location, blockFace);
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "set block " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in " + graveyard.getName());
            }
        });
    }

    public void removeLocationInGraveyard(Player player, Location location, Graveyard graveyard) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (graveyard.hasGraveLocation(location)) {
                graveyard.removeGraveLocation(location);
                plugin.getDataManager().updateGraveyardLocationData(graveyard);
                refreshLocation(player, location);
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "remove block " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in "+ graveyard.getName());
            }
        });
    }

    public Map<Location, BlockFace> getGraveyardFreeSpaces(Graveyard graveyard) {
        Map<Location, BlockFace> locationMap = new HashMap<>(graveyard.getGraveLocationMap());
        locationMap.entrySet().removeAll(getGraveyardUsedSpaces(graveyard).entrySet());
        return locationMap;
    }

    public Map<Location, BlockFace> getGraveyardUsedSpaces(Graveyard graveyard) {
        Map<Location, BlockFace> locationMap = new HashMap<>();

        for (Map.Entry<Location, BlockFace> entry : graveyard.getGraveLocationMap().entrySet()) {
            if (plugin.getBlockManager().getGraveFromBlock(entry.getKey().getBlock()) != null || plugin.getDataManager().hasGraveAtLocation(entry.getKey())) {
                locationMap.put(entry.getKey(), entry.getValue());
            }
        }

        return locationMap;
    }

    public boolean isModifyingGraveyard(Player player) {
        return modifyingGraveyardMap.containsKey(player.getUniqueId());
    }

    public Graveyard getModifyingGraveyard(Player player) {
        return modifyingGraveyardMap.get(player.getUniqueId());
    }

    public void startModifyingGraveyard(Player player, Graveyard graveyard) throws InvocationTargetException {
        if (isModifyingGraveyard(player)) {
            stopModifyingGraveyard(player);
        }

        modifyingGraveyardMap.put(player.getUniqueId(), graveyard);

        for (Map.Entry<Location, BlockFace> entry : graveyard.getGraveLocationMap().entrySet()) {
            previewLocation(player, entry.getKey(), entry.getValue());
        }

        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Started modifying graveyard " + graveyard.getName());
    }

    public void stopModifyingGraveyard(Player player) throws InvocationTargetException {
        Graveyard graveyard = getModifyingGraveyard(player);

        if (graveyard != null) {
            modifyingGraveyardMap.remove(player.getUniqueId());

            CacheManager cacheManager = plugin.getCacheManager();
            Location cachedLocation = cacheManager.getRightClickedBlock(player.getName());

            if (cachedLocation != null) {
                BlockFace blockFace = BlockFace.SELF; // or determine the correct BlockFace

                graveyard.addGraveLocation(cachedLocation, blockFace);
                cacheManager.removeRightClickedBlock(player.getName(), cachedLocation);

                player.sendMessage(ChatColor.GREEN + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.GREEN + "Added location to graveyard " + graveyard.getName());
            }

            for (Location location : graveyard.getGraveLocationMap().keySet()) {
                refreshLocation(player, location);
            }

            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Stopped modifying graveyard " + graveyard.getName());

            // Serialize locations
            String serializedLocations = Graveyard.serializeLocations(graveyard.getGraveLocationMap());
            plugin.getLogger().info("Serialized locations before saving: " + serializedLocations);

            plugin.getDataManager().saveGraveyard(graveyard, serializedLocations);
        }
    }

    public Graveyard getGraveyardByName(String graveyardName) {
        return plugin.getDataManager().getGraveyardByName(graveyardName);
    }

    public void deleteGraveyard(Player player, Graveyard graveyard) {
        modifyingGraveyardMap.remove(player.getUniqueId());

        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Deleted graveyard " + graveyard.getName());

        plugin.getDataManager().deleteGraveyard(graveyard);
    }

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

    private void previewLocation(Player player, Location location, BlockFace blockFace) {
        try {
            if (plugin.getIntegrationManager().hasProtocolLib()) {
                plugin.getIntegrationManager().getProtocolLib().setBlock(location.getBlock(), Material.PLAYER_HEAD, player);
            }
        } catch (InvocationTargetException e) {
            plugin.getLogger().severe("Failed to preview location: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    private void refreshLocation(Player player, Location location) {
        try {
            if (plugin.getIntegrationManager().hasProtocolLib()) {
                plugin.getIntegrationManager().getProtocolLib().refreshBlock(location.getBlock(), player);
            }
        } catch (InvocationTargetException e) {
            plugin.getLogger().severe("Failed to refresh location: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    public List<Graveyard> getAllGraveyardList() {
        return new ArrayList<>(graveyardMap.values());
    }

    public Graveyard[] getAllGraveyardArray() {
        List<Graveyard> graveyardsList = new ArrayList<>(graveyardMap.values());
        return graveyardsList.toArray(new Graveyard[0]);
    }
}
