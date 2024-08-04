package com.ranull.graves.type;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a graveyard in the game, containing information about its name, world,
 * type, grave locations, and other attributes.
 */
public class Graveyard {
    private final String name;
    private final World world;
    private final Graveyard.Type type;
    private final Map<Location, BlockFace> graveLocationMap;
    private Location spawnLocation;
    private String title;
    private String description;
    private boolean isPublic;
    private Location location;
    private static final Gson gson = new Gson();

    /**
     * Constructs a new Graveyard with the specified name, world, and type.
     *
     * @param name  The name of the graveyard.
     * @param world The world the graveyard is located in.
     * @param type  The type of the graveyard.
     */
    public Graveyard(String name, World world, Graveyard.Type type) {
        this.name = name;
        this.world = world;
        this.type = type;
        this.graveLocationMap = new HashMap<>();
    }

    /**
     * Gets the unique key for the graveyard.
     *
     * @return The key of the graveyard.
     */
    public String getKey() {
        return type.name().toLowerCase() + "|" + world.getName() + "|" + name;
    }

    /**
     * Gets the name of the graveyard.
     *
     * @return The name of the graveyard.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the world the graveyard is located in.
     *
     * @return The world of the graveyard.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Gets the type of the graveyard.
     *
     * @return The type of the graveyard.
     */
    public Graveyard.Type getType() {
        return type;
    }

    /**
     * Adds a grave location to the graveyard.
     *
     * @param location The location of the grave.
     * @param blockFace The direction the grave is facing.
     */
    public void addGraveLocation(Location location, BlockFace blockFace) {
        graveLocationMap.put(location, blockFace);
    }

    /**
     * Removes a grave location from the graveyard.
     *
     * @param location The location to remove.
     */
    public void removeGraveLocation(Location location) {
        graveLocationMap.remove(location);
    }

    /**
     * Checks if the graveyard contains the specified grave location.
     *
     * @param location The location to check.
     * @return True if the location exists in the graveyard, otherwise false.
     */
    public boolean hasGraveLocation(Location location) {
        return graveLocationMap.containsKey(location);
    }

    /**
     * Gets the map of grave locations in the graveyard.
     *
     * @return The map of grave locations.
     */
    public Map<Location, BlockFace> getGraveLocationMap() {
        return graveLocationMap;
    }

    /**
     * Gets the spawn location of the graveyard.
     *
     * @return The spawn location.
     */
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Sets the spawn location for the graveyard.
     *
     * @param location The spawn location to set.
     */
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }

    /**
     * Gets the title of the graveyard.
     *
     * @return The title of the graveyard.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title for the graveyard.
     *
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the description of the graveyard.
     *
     * @return The description of the graveyard.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for the graveyard.
     *
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if the graveyard is public.
     *
     * @return True if the graveyard is public, otherwise false.
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * Sets the public status of the graveyard.
     *
     * @param isPublic True to make the graveyard public, otherwise false.
     */
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the location associated with the graveyard location.
     *
     * @return the location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location associated with the graveyard location.
     *
     * @param location the location to set.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Sets the location by deserializing a string representation of the location.
     *
     * @param serializedLocation the serialized string representing the location.
     */
    public void setLocation(String serializedLocation) {
        this.location = deserializeLocation(serializedLocation);
    }

    /**
     * Deserializes a string representation of a location.
     *
     * @param serializedLocation the serialized string representing the location.
     * @return the deserialized location graveyard.
     */
    public static Location deserializeLocation(String serializedLocation) {
        try {
            JsonObject json = gson.fromJson(serializedLocation, JsonObject.class);
            World world = Bukkit.getWorld(json.get("world").getAsString());
            int x = json.get("x").getAsInt();
            int y = json.get("y").getAsInt();
            int z = json.get("z").getAsInt();
            float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0f;
            float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0.0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid serialized location: " + serializedLocation, e);
        }
    }

    public static String serializeLocation(Location location) {
        if (location == null) {
            return "{}";
        }
        JsonObject json = new JsonObject();
        json.addProperty("world", location.getWorld().getName());
        json.addProperty("x", location.getBlockX());
        json.addProperty("y", location.getBlockY());
        json.addProperty("z", location.getBlockZ());
        json.addProperty("yaw", location.getYaw());
        json.addProperty("pitch", location.getPitch());
        return gson.toJson(json);
    }

    /**
     * Enum for defining the priority levels for different graveyard types.
     */
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

    /**
     * Enum for defining the different types of graveyards.
     */
    public enum Type {
        WORLDGUARD,
        TOWNY,
        FACTIONS
    }
}