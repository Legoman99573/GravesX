package com.ranull.graves.type;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.Map;

public class Graveyard {
    private final String name;
    private final World world;
    private final Type type;
    private final Map<Location, BlockFace> graveLocationMap;
    private Location spawnLocation;
    private String title;
    private String description;
    private boolean isPublic;
    private static final Gson gson = new Gson();

    public Graveyard(String name, World world, Type type) {
        this.name = name;
        this.world = world;
        this.type = type;
        this.graveLocationMap = new HashMap<>();
    }

    public String getKey() {
        return type.name().toLowerCase() + "|" + world.getName() + "|" + name;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public Type getType() {
        return type;
    }

    public void addGraveLocation(Location location, BlockFace blockFace) {
        graveLocationMap.put(location, blockFace);
    }

    public void removeGraveLocation(Location location) {
        graveLocationMap.remove(location);
    }

    public boolean hasGraveLocation(Location location) {
        return graveLocationMap.containsKey(location);
    }

    public Map<Location, BlockFace> getGraveLocationMap() {
        return graveLocationMap;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public static String serializeLocations(Map<Location, BlockFace> locations) {
        JsonArray jsonArray = new JsonArray();
        for (Map.Entry<Location, BlockFace> entry : locations.entrySet()) {
            JsonObject json = new JsonObject();
            Location location = entry.getKey();
            json.addProperty("world", location.getWorld().getName());
            json.addProperty("x", location.getBlockX());
            json.addProperty("y", location.getBlockY());
            json.addProperty("z", location.getBlockZ());
            json.addProperty("yaw", location.getYaw());
            json.addProperty("pitch", location.getPitch());
            jsonArray.add(json);
        }
        String serialized = gson.toJson(jsonArray);
        System.out.println("Serialized locations: " + serialized); // Add debug logging
        return serialized;
    }

    public static Map<Location, BlockFace> deserializeLocations(String serializedLocations) {
        Map<Location, BlockFace> locations = new HashMap<>();
        try {
            JsonArray jsonArray = gson.fromJson(serializedLocations, JsonArray.class);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject json = jsonArray.get(i).getAsJsonObject();
                World world = Bukkit.getWorld(json.get("world").getAsString());
                int x = json.get("x").getAsInt();
                int y = json.get("y").getAsInt();
                int z = json.get("z").getAsInt();
                float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0f;
                float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0.0f;
                Location location = new Location(world, x, y, z, yaw, pitch);
                locations.put(location, BlockFace.SELF); // Set default BlockFace or determine the correct one
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid serialized locations: " + serializedLocations, e);
        }
        return locations;
    }

    public enum PRIORITY {
        TOWNY_TOWN,
        TOWNY_NATION,
        TOWNY_PUBLIC,
        FACTIONS_FACTION,
        FACTIONS_ALLY,
        FACTIONS_PUBLIC,
        WORLDGUARD_OWNER,
        WORLDGUARD_MEMBER,
        WORLDGUARD_PUBLIC
    }

    public enum Type {
        WORLDGUARD,
        TOWNY,
        FACTIONS
    }
}