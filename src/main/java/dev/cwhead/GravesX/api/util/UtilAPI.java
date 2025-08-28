package dev.cwhead.GravesX.api.util;

import com.ranull.graves.Graves;
import com.ranull.graves.util.*;
import dev.cwhead.GravesX.api.world.LocationAPI;
import dev.cwhead.GravesX.util.LibraryLoaderUtil;
import dev.cwhead.GravesX.util.MclogsUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

/**
 * General utilities (permissions, XP, colors, files, YAML, paste, etc.).
 */
public final class UtilAPI {
    private final Graves plugin;
    private final LocationAPI world;

    public UtilAPI(Graves plugin, LocationAPI world) {
        this.plugin = plugin;
        this.world = world;
    }

    /**
     * Encodes an object to a Base64 string using Base64Util.
     *
     * @param object The object to encode.
     * @return The Base64 encoded string, or null if encoding fails.
     */
    public String objectToBase64(@NotNull Object object) { return Base64Util.objectToBase64(object); }

    /**
     * Decodes a Base64 string to an object using Base64Util.
     *
     * @param base64String The Base64 string to decode.
     * @return The decoded object, or null if decoding fails.
     */
    public Object base64ToObject(@NotNull String base64String) { return Base64Util.base64ToObject(base64String); }

    /**
     * Loads a class with the specified name using ClassUtil.
     *
     * @param className The fully qualified name of the class to be loaded.
     */
    public void loadClass(@NotNull String className) { ClassUtil.loadClass(className); }

    /**
     * Gets the Color corresponding to the given color name using ColorUtil.
     *
     * @param colorName The name of the color as a string.
     * @return The Color corresponding to the given name, or null if no match is found.
     */
    public Color getColor(@NotNull String colorName) { return ColorUtil.getColor(colorName); }

    /**
     * Parses a hex color code to a Color using ColorUtil.
     *
     * @param hex The hex color code as a string (e.g., "#FF5733").
     * @return The Color corresponding to the hex color code, or null if the code is invalid.
     */
    public Color getColorFromHex(@NotNull String hex) { return ColorUtil.getColorFromHex(hex); }

    /**
     * Creates a Particle.DustOptions object using a hex color code.
     *
     * @param hexColor The hex color code as a string (e.g., "#FF5733").
     * @param size The size of the dust particle.
     * @return A Particle.DustOptions object with the specified color and size, or null if the color code is invalid.
     */
    public Particle.DustOptions dustFromHex(@NotNull String hexColor, float size) { return ColorUtil.createDustOptionsFromHex(hexColor, size); }

    /**
     * Checks if the specified entity has the given permission using EntityUtil.
     *
     * @param entity     The entity to check.
     * @param permission The permission to check for.
     * @return {@code true} if the entity has the specified permission,
     *         {@code true} if the method is not found,
     *         or {@code false} if an exception occurs.
     */
    public boolean hasPermission(@NotNull Entity entity, @NotNull String permission) {
        return EntityUtil.hasPermission(entity, permission);
    }

    /**
     * Gets the total experience of the specified player using ExperienceUtil.
     *
     * @param player The player to get the experience from.
     * @return The total experience of the player.
     */
    public int playerTotalXp(@NotNull Player player) { return ExperienceUtil.getPlayerExperience(player); }

    /**
     * Gets the experience required to reach a specific level using ExperienceUtil.
     *
     * @param level The level to get the experience for.
     * @return The experience required to reach the specified level.
     */
    public int xpAtLevel(int level) { return ExperienceUtil.getExperienceAtLevel(level); }

    /**
     * Calculates the level from a given amount of experience using ExperienceUtil.
     *
     * @param experience The experience to calculate the level from.
     * @return The level corresponding to the given experience.
     */
    public long levelFromXp(long experience) { return ExperienceUtil.getLevelFromExperience(experience); }

    /**
     * Calculates the drop percentage of experience using ExperienceUtil.
     *
     * @param experience The total experience.
     * @param percent    The percentage to drop.
     * @return The experience drop amount.
     */
    public int dropPercent(int experience, float percent) { return ExperienceUtil.getDropPercent(experience, percent); }

    /**
     * Gets the amount of experience a player will drop upon death based on a percentage.
     *
     * @param player          The player to get the drop experience from.
     * @param expStorePercent The percentage of experience to drop.
     * @return The amount of experience to drop.
     */
    @Deprecated
    public int playerDropXp(@NotNull Player player, float expStorePercent) { return ExperienceUtil.getPlayerDropExperience(player, expStorePercent); }

    /**
     * Moves a file to a new location with a new name using FileUtil.
     *
     * @param file The file to be moved.
     * @param name The new name for the file.
     */
    public void moveFile(@NotNull File file, @NotNull String name) { FileUtil.moveFile(file, name); }

    /**
     * Copies a file to a new location with a new name using FileUtil.
     *
     * @param file The file to be copied.
     * @param name The new name for the copied file.
     */
    @Deprecated
    public void copyFile(@NotNull File file, @NotNull String name) { FileUtil.copyFile(file, name); }

    /**
     * Checks if the given material is an air block.
     *
     * @param material The material to check.
     * @return True if the material is air, false otherwise.
     */
    public boolean isAir(@NotNull Material material) { return MaterialUtil.isAir(material); }

    /**
     * Checks if the given material is lava.
     *
     * @param material The material to check.
     * @return True if the material is lava, false otherwise.
     */
    public boolean isLava(@NotNull Material material) { return MaterialUtil.isLava(material); }

    /**
     * Checks if the given material is not solid and is safe (i.e., not lava).
     *
     * @param material The material to check.
     * @return True if the material is not solid and safe, false otherwise.
     */
    public boolean isSafeNotSolid(@NotNull Material material) { return MaterialUtil.isSafeNotSolid(material); }

    /**
     * Checks if the given material is solid and safe (i.e., not lava).
     *
     * @param material The material to check.
     * @return True if the material is solid and safe, false otherwise.
     */
    public boolean isSafeSolid(@NotNull Material material) { return MaterialUtil.isSafeSolid(material); }

    /**
     * Checks if the given material is water.
     *
     * @param material The material to check.
     * @return True if the material is water, false otherwise.
     */
    public boolean isWater(@NotNull Material material) { return MaterialUtil.isWater(material); }

    /**
     * Checks if the given material is a player head.
     *
     * @param material The material to check.
     * @return True if the material is a player head, false otherwise.
     */
    public boolean isPlayerHead(@NotNull Material material) { return MaterialUtil.isPlayerHead(material); }

    /**
     * Checks if the given material is a player head.
     *
     * @param material The material to check via string.
     * @return True if the material is a player head, false otherwise.
     */
    public boolean isPlayerHead(@NotNull String material) { return MaterialUtil.isPlayerHead(material); }

    /**
     * Posts the given log content to mclo.gs and returns the URL of the posted log.
     *
     * @param content The log content to be posted.
     * @return The URL of the posted log, or null if the post was unsuccessful.
     */
    public String postLog(@NotNull String content) { return MclogsUtil.postLogToMclogs(content); }

    /**
     * Gets the highest integer value associated with a specific permission prefix for the player.
     *
     * @param player     The player whose permissions are being checked.
     * @param permission The permission prefix to search for.
     * @return The highest integer value found for the specified permission prefix. Returns 0 if no such permission is found.
     */
    public int highestInt(@NotNull Player player, @Nullable String permission) { return PermissionUtil.getHighestInt(player, permission); }

    /**
     * Gets the highest double value associated with a specific permission prefix for the player.
     *
     * @param player     The player whose permissions are being checked.
     * @param permission The permission prefix to search for.
     * @return The highest double value found for the specified permission prefix. Returns 0 if no such permission is found.
     */
    public double highestDouble(@NotNull Player player, String permission) { return PermissionUtil.getHighestDouble(player, permission); }

    /**
     * Triggers the main hand swing animation for the specified player.
     *
     * @param player The player whose main hand swing animation is to be triggered.
     */
    public void swingMainHand(@NotNull Player player) { ReflectionUtil.swingMainHand(player); }

    /**
     * Copies resources from the plugin's JAR file to the specified output path.
     *
     * @param inputPath  The path inside the JAR file to copy from.
     * @param outputPath The path on the file system to copy to.
     */
    public void copyResources(@NotNull String inputPath, @NotNull String outputPath) {
        ResourceUtil.copyResources(inputPath, outputPath, plugin);
    }

    /**
     * Copies resources from the plugin's JAR file to the specified output path, with an option to overwrite existing files.
     *
     * @param inputPath  The path inside the JAR file to copy from.
     * @param outputPath The path on the file system to copy to.
     * @param overwrite  Whether to overwrite existing files.
     */
    public void copyResources(@NotNull String inputPath, @NotNull String outputPath, boolean overwrite) {
        ResourceUtil.copyResources(inputPath, outputPath, overwrite, plugin);
    }

    /** Convenience to get/update libraries. */
    public LibraryLoaderUtil libraryLoader() { return new LibraryLoaderUtil(plugin); }

    /** Shortcuts used during creation (texture/signature). */
    public String skinSignature(@NotNull Entity entity) { return SkinSignatureUtil.getSignature(entity); }
    public String skinTexture(@NotNull Entity entity) { return SkinTextureUtil.getTexture(entity); }

    /**
     * Converts a string to a UUID.
     *
     * @param string The string to convert to a UUID.
     * @return The UUID if the string is a valid UUID format, otherwise null.
     */
    public UUID uuidOf(@NotNull String string) { return UUIDUtil.getUUID(string); }

    /**
     * Gets the latest version of a resource from SpigotMC.
     *
     * @param resourceId The ID of the resource on SpigotMC.
     * @return The latest version of the resource as a String, or null if an error occurs.
     */
    public String latestSpigotVersion(int resourceId) { return UpdateUtil.getLatestVersion(resourceId); }

    /**
     * Checks if a given file is a valid YAML file.
     *
     * @param file The file to check.
     * @return True if the file is a valid YAML file, otherwise false.
     */
    public boolean isValidYaml(@NotNull File file) { return YAMLUtil.isValidYAML(file); }
}