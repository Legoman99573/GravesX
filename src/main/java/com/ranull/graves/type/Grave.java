package com.ranull.graves.type;

import com.ranull.graves.data.LocationData;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a grave in the game, storing information about the player or entity
 * that the grave belongs to, including inventory, location, and various other attributes.
 */
public class Grave implements InventoryHolder, Serializable {
    private final UUID uuid;
    private transient Inventory inventory;
    private Map<EquipmentSlot, ItemStack> equipmentMap;
    private List<String> permissionList;
    private LocationData locationDeath;
    private float yaw;
    private float pitch;
    private EntityType ownerType;
    private String ownerName;
    private String ownerNameDisplay;
    private UUID ownerUUID;
    private String ownerTexture;
    private String ownerTextureSignature;
    private EntityType killerType;
    private String killerName;
    private String killerNameDisplay;
    private UUID killerUUID;
    private int experience;
    private boolean protection;
    private long timeAlive;
    private long timeCreation;
    private long timeProtection;
    private Location location;

    /**
     * Constructs a new Grave with the specified UUID.
     *
     * @param uuid The UUID of the grave.
     */
    public Grave(UUID uuid) {
        this.uuid = uuid;
        this.timeCreation = System.currentTimeMillis();
    }

    /**
     * Gets the inventory associated with this grave.
     *
     * @return The inventory of the grave.
     */
    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the inventory for this grave.
     *
     * @param inventory The inventory to set.
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets the equipment map for this grave.
     *
     * @return The equipment map.
     */
    public Map<EquipmentSlot, ItemStack> getEquipmentMap() {
        return equipmentMap;
    }

    /**
     * Sets the equipment map for this grave.
     *
     * @param equipmentMap The equipment map to set.
     */
    public void setEquipmentMap(Map<EquipmentSlot, ItemStack> equipmentMap) {
        this.equipmentMap = equipmentMap;
    }

    /**
     * Gets a list of item stacks in the inventory.
     *
     * @return A list of item stacks.
     */
    public List<ItemStack> getInventoryItemStack() {
        return inventory != null ? Arrays.asList(inventory.getContents()) : new ArrayList<>();
    }

    /**
     * Gets the UUID of the grave.
     *
     * @return The UUID of the grave.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the permission list associated with this grave.
     *
     * @return The permission list.
     */
    public List<String> getPermissionList() {
        return permissionList;
    }

    /**
     * Sets the permission list for this grave.
     *
     * @param permissionList The permission list to set.
     */
    public void setPermissionList(List<String> permissionList) {
        this.permissionList = permissionList;
    }

    /**
     * Gets the death location of the grave.
     *
     * @return The death location.
     */
    public Location getLocationDeath() {
        return locationDeath != null ? locationDeath.getLocation() : null;
    }

    /**
     * Sets the death location for the grave.
     *
     * @param locationDeath The death location to set.
     */
    public void setLocationDeath(Location locationDeath) {
        this.locationDeath = new LocationData(locationDeath);
    }

    /**
     * Gets the yaw (rotation) of the grave.
     *
     * @return The yaw.
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Sets the yaw (rotation) of the grave.
     *
     * @param yaw The yaw to set.
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * Gets the pitch (vertical rotation) of the grave.
     *
     * @return The pitch.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Sets the pitch (vertical rotation) of the grave.
     *
     * @param pitch The pitch to set.
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * Gets the entity type of the grave's owner.
     *
     * @return The owner entity type.
     */
    public EntityType getOwnerType() {
        return ownerType;
    }

    /**
     * Sets the entity type of the grave's owner.
     *
     * @param ownerType The owner entity type to set.
     */
    public void setOwnerType(EntityType ownerType) {
        this.ownerType = ownerType;
    }

    /**
     * Gets the name of the grave's owner.
     *
     * @return The owner name.
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Sets the name of the grave's owner.
     *
     * @param ownerName The owner name to set.
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * Gets the display name of the grave's owner.
     *
     * @return The owner display name.
     */
    public String getOwnerNameDisplay() {
        return ownerNameDisplay;
    }

    /**
     * Sets the display name of the grave's owner.
     *
     * @param ownerNameDisplay The owner display name to set.
     */
    public void setOwnerNameDisplay(String ownerNameDisplay) {
        this.ownerNameDisplay = ownerNameDisplay;
    }

    /**
     * Gets the UUID of the grave's owner.
     *
     * @return The owner UUID.
     */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Sets the UUID of the grave's owner.
     *
     * @param ownerUUID The owner UUID to set.
     */
    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    /**
     * Gets the texture of the grave's owner.
     *
     * @return The owner texture.
     */
    public String getOwnerTexture() {
        return ownerTexture;
    }

    /**
     * Sets the texture of the grave's owner.
     *
     * @param ownerTexture The owner texture to set.
     */
    public void setOwnerTexture(String ownerTexture) {
        this.ownerTexture = ownerTexture;
    }

    /**
     * Gets the texture signature of the grave's owner.
     *
     * @return The owner texture signature.
     */
    public String getOwnerTextureSignature() {
        return ownerTextureSignature;
    }

    /**
     * Sets the texture signature of the grave's owner.
     *
     * @param ownerTextureSignature The owner texture signature to set.
     */
    public void setOwnerTextureSignature(String ownerTextureSignature) {
        this.ownerTextureSignature = ownerTextureSignature;
    }

    /**
     * Gets the entity type of the grave's killer.
     *
     * @return The killer entity type.
     */
    public EntityType getKillerType() {
        return killerType;
    }

    /**
     * Sets the entity type of the grave's killer.
     *
     * @param killerType The killer entity type to set.
     */
    public void setKillerType(EntityType killerType) {
        this.killerType = killerType;
    }

    /**
     * Gets the name of the grave's killer.
     *
     * @return The killer name.
     */
    public String getKillerName() {
        return killerName;
    }

    /**
     * Sets the name of the grave's killer.
     *
     * @param killerName The killer name to set.
     */
    public void setKillerName(String killerName) {
        this.killerName = killerName;
    }

    /**
     * Gets the display name of the grave's killer.
     *
     * @return The killer display name.
     */
    public String getKillerNameDisplay() {
        return killerNameDisplay;
    }

    /**
     * Sets the display name of the grave's killer.
     *
     * @param killerNameDisplay The killer display name to set.
     */
    public void setKillerNameDisplay(String killerNameDisplay) {
        this.killerNameDisplay = killerNameDisplay;
    }

    /**
     * Gets the UUID of the grave's killer.
     *
     * @return The killer UUID.
     */
    public UUID getKillerUUID() {
        return killerUUID;
    }

    /**
     * Sets the UUID of the grave's killer.
     *
     * @param killerUUID The killer UUID to set.
     */
    public void setKillerUUID(UUID killerUUID) {
        this.killerUUID = killerUUID;
    }

    /**
     * Gets the experience points stored in the grave.
     *
     * @return The experience points.
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Sets the experience points for the grave.
     *
     * @param experience The experience points to set.
     */
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * Checks if the grave has protection enabled.
     *
     * @return True if protection is enabled, otherwise false.
     */
    public boolean getProtection() {
        return protection;
    }

    /**
     * Sets the protection status for the grave.
     *
     * @param protection True to enable protection, otherwise false.
     */
    public void setProtection(boolean protection) {
        this.protection = protection;
    }

    /**
     * Gets the time (in milliseconds) the grave is set to be alive.
     *
     * @return The time alive.
     */
    public long getTimeAlive() {
        return timeAlive;
    }

    /**
     * Sets the time (in milliseconds) the grave is set to be alive.
     *
     * @param aliveTime The time alive to set.
     */
    public void setTimeAlive(long aliveTime) {
        this.timeAlive = aliveTime;
    }

    /**
     * Gets the creation time (in milliseconds) of the grave.
     *
     * @return The creation time.
     */
    public long getTimeCreation() {
        return timeCreation;
    }

    /**
     * Sets the creation time (in milliseconds) of the grave.
     *
     * @param timeCreation The creation time to set.
     */
    public void setTimeCreation(long timeCreation) {
        this.timeCreation = timeCreation;
    }

    /**
     * Gets the protection time (in milliseconds) of the grave.
     *
     * @return The protection time.
     */
    public long getTimeProtection() {
        return timeProtection;
    }

    /**
     * Sets the protection time (in milliseconds) of the grave.
     *
     * @param timeProtection The protection time to set.
     */
    public void setTimeProtection(long timeProtection) {
        this.timeProtection = timeProtection;
    }

    /**
     * Sets the remaining time (in milliseconds) the grave is set to be alive.
     * A value of -1 indicates the grave should not expire.
     *
     * @param timeAlive The new remaining time alive.
     */
    public void setTimeAliveRemaining(long timeAlive) {
        this.timeAlive = timeAlive;
    }

    /**
     * Gets the remaining time (in milliseconds) the grave is set to be alive.
     *
     * @return The remaining time alive.
     */
    public long getTimeAliveRemaining() {
        if (timeAlive < 0) {
            return -1; // Indicates the grave is not set to expire
        } else {
            long elapsedTime = System.currentTimeMillis() - timeCreation;
            long timeAliveRemaining = timeAlive - elapsedTime;

            // Return 0 if the remaining time is less than or equal to 0
            return Math.max(timeAliveRemaining, 0);
        }
    }

    /**
     * Gets the remaining time (in milliseconds) the grave is protected.
     *
     * @return The remaining protection time.
     */
    public long getTimeProtectionRemaining() {
        if (timeProtection < 0) {
            return -1; // Indicates the grave is not set to expire protection
        } else {
            long elapsedTime = System.currentTimeMillis() - timeCreation;
            long timeProtectionRemaining = timeProtection - elapsedTime;

            return Math.max(timeProtectionRemaining, 0);
        }
    }

    /**
     * Gets the total lived time (in milliseconds) since the grave was created.
     *
     * @return The lived time.
     */
    public long getLivedTime() {
        return System.currentTimeMillis() - timeCreation;
    }

    /**
     * Gets the number of items in the grave's inventory.
     *
     * @return The number of items.
     */
    public int getItemAmount() {
        int counter = 0;

        if (inventory != null) {
            for (ItemStack itemStack : inventory.getContents()) {
                if (itemStack != null) {
                    counter++;
                }
            }
        }

        return counter;
    }

    public String getOwnerDisplayName() {
        return ownerNameDisplay;
    }

    public void getOwnerDisplayName(String ownerNameDisplay) {
        this.ownerNameDisplay = ownerNameDisplay;
    }

    public Location getLocation() {
        return location;
    }

    public void getLocation(Location location) {
        this.location = location;
    }

    /**
     * Enum for defining different storage modes for the grave.
     */
    public enum StorageMode {
        EXACT,
        COMPACT,
        CHESTSORT
    }
}