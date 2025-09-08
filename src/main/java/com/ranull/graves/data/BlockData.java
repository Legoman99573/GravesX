package com.ranull.graves.data;

import dev.cwhead.GravesX.data.BlockFace;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents block data associated with a grave.
 * <p>
 * In addition to the replacement info, this class can snapshot the original
 * block (material and data string) and, if the block is a player head, its owner/texture/rotation.
 * </p>
 */
public class BlockData implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The location of the grave in the world.
     * <p>
     * This {@link Location} represents the coordinates where the grave is situated.
     * </p>
     */
    private final Location location;

    /**
     * The unique identifier for the grave.
     * <p>
     * This {@link UUID} uniquely identifies the grave entity.
     * </p>
     */
    private final UUID graveUUID;

    /**
     * The material used to replace the grave block.
     * <p>
     * This {@link String} specifies the material to replace the block at the grave's location.
     * </p>
     */
    private final String replaceMaterial;

    /**
     * The data associated with the replacement material.
     * <p>
     * This {@link String} provides additional data or properties for the replacement material, if applicable.
     * For 1.13+, prefer a Bukkit BlockData string (e.g., "minecraft:oak_log[axis=y]").
     * </p>
     */
    private final String replaceData;

    /**
     * Original block material at {@link #location} before the grave replaced it (e.g., "CHEST", "PLAYER_HEAD").
     */
    private final String originalMaterial;

    /**
     * Original block data string.
     * <ul>
     *   <li>On 1.13+, this is the full {@code org.bukkit.block.data.BlockData#getAsString()}.</li>
     *   <li>On legacy (1.8–1.12), this will be "LEGACY:{MATERIAL}:{data}" where {@code data} is the durability/data value.</li>
     * </ul>
     */
    private final String originalBlockData;

    /**
     * If the original block is a player head, this is the head's facing/rotation as a {@link BlockFace} name.
     * For floor heads (rotatable), it's the rotation; for wall heads, the facing; otherwise {@code "SELF"}.
     */
    private final String skullRotationFace;

    /**
     * If the original block is a player head, this is the owner UUID (may be null).
     */
    private final UUID skullOwnerUuid;

    /**
     * If the original block is a player head, this is the owner name (may be null).
     */
    private final String skullOwnerName;

    /**
     * If the original block is a player head, this is the raw base64 "textures" property (may be null).
     */
    private final String skullTexturesValue;

    /**
     * If the original block is a player head, this is the signature for the "textures" property (may be null).
     */
    private final String skullTexturesSignature;

    /**
     * Enum representing the type of block.
     */
    public enum BlockType {
        DEATH,
        NORMAL
    }

    /**
     * Constructs a new BlockData instance.
     *
     * @param location            The location of the block.
     * @param graveUUID           The UUID of the associated grave.
     * @param replaceMaterial     The material to replace the block with.
     * @param replaceData         The data to replace the block with.
     * @param originalMaterial    The original material at the location (nullable).
     * @param originalBlockData   The original block data string (nullable).
     * @param skullRotationFace   Rotation/facing if original is a skull (nullable).
     * @param skullOwnerUuid      Owner UUID if original is a skull (nullable).
     * @param skullOwnerName      Owner name if original is a skull (nullable).
     * @param skullTexturesValue  Base64 textures if original is a skull (nullable).
     * @param skullTexturesSignature Signature for textures if original is a skull (nullable).
     */
    public BlockData(Location location,
                     UUID graveUUID,
                     String replaceMaterial,
                     String replaceData,
                     String originalMaterial,
                     String originalBlockData,
                     String skullRotationFace,
                     UUID skullOwnerUuid,
                     String skullOwnerName,
                     String skullTexturesValue,
                     String skullTexturesSignature) {
        this.location = location;
        this.graveUUID = graveUUID;
        this.replaceMaterial = replaceMaterial;
        this.replaceData = replaceData;
        this.originalMaterial = originalMaterial;
        this.originalBlockData = originalBlockData;
        this.skullRotationFace = skullRotationFace;
        this.skullOwnerUuid = skullOwnerUuid;
        this.skullOwnerName = skullOwnerName;
        this.skullTexturesValue = skullTexturesValue;
        this.skullTexturesSignature = skullTexturesSignature;
    }

    /**
     * Backwards-compatible constructor (without original/skull fields).
     *
     * @param location        The location of the block.
     * @param graveUUID       The UUID of the associated grave.
     * @param replaceMaterial The material to replace the block with.
     * @param replaceData     The data to replace the block with.
     */
    public BlockData(Location location, UUID graveUUID, String replaceMaterial, String replaceData) {
        this(location, graveUUID, replaceMaterial, replaceData,
                null, null, null, null, null, null, null);
    }

    /**
     * Gets the location of the block.
     */
    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    /**
     * Gets the UUID of the associated grave.
     */
    public UUID getGraveUUID() {
        return graveUUID;
    }

    /**
     * Gets the material to replace the block with.
     */
    public String getReplaceMaterial() {
        return replaceMaterial;
    }

    /**
     * Gets the data to replace the block with.
     */
    public String getReplaceData() {
        return replaceData;
    }

    /**
     * Gets the original material (e.g., "CHEST", "PLAYER_HEAD"), if captured.
     */
    public String getOriginalMaterial() {
        return originalMaterial;
    }

    /**
     * Gets the original block data string.
     * <ul>
     *   <li>1.13+: full BlockData string (e.g., "minecraft:oak_sign[facing=north,rotation=8]").</li>
     *   <li>Legacy: "LEGACY:{MATERIAL}:{data}".</li>
     * </ul>
     */
    public String getOriginalBlockData() {
        return originalBlockData;
    }

    /**
     * Gets the skull rotation/facing (BlockFace name) if the original block was a player head.
     */
    public String getSkullRotationFace() {
        return skullRotationFace;
    }

    /**
     * Gets the skull owner UUID if the original block was a player head.
     */
    public UUID getSkullOwnerUuid() {
        return skullOwnerUuid;
    }

    /**
     *  Gets the skull owner name if the original block was a player head.
     */
    public String getSkullOwnerName() {
        return skullOwnerName;
    }

    /**
     * Gets the raw base64 "textures" value if the original block was a player head.
     */
    public String getSkullTexturesValue() {
        return skullTexturesValue;
    }

    /**
     * Gets the signature for the "textures" value if the original block was a player head.
     */
    public String getSkullTexturesSignature() {
        return skullTexturesSignature;
    }

    /**
     * Builds a {@link BlockData} by snapshotting the current block at the given location.
     * <p>
     * Call on the main/region thread. Works on both 1.13+ and legacy servers (for legacy, uses reflection).
     * </p>
     *
     * @param graveUUID       The associated grave UUID.
     * @param block           The placed block to snapshot.
     * @param replaceMaterial The material you intend to place for the grave.
     * @param replaceData     The replacement block data string you intend to use.
     * @return a filled {@link BlockData} with original block info (and skull details if applicable).
     */
    public static BlockData fromBlock(Block block, UUID graveUUID, String replaceMaterial, String replaceData) {
        return fromBlock(graveUUID, block, replaceMaterial, replaceData);
    }

    /**
     * Builds a {@link BlockData} by snapshotting the current block at the given location.
     * <p>
     * Call on the main/region thread. Works on both 1.13+ and legacy servers (for legacy, uses reflection).
     * </p>
     *
     * @param graveUUID       The associated grave UUID.
     * @param block           The placed block to snapshot.
     * @param replaceMaterial The material you intend to place for the grave.
     * @param replaceData     The replacement block data string you intend to use.
     * @return a filled {@link BlockData} with original block info (and skull details if applicable).
     */
    public static BlockData fromBlock(UUID graveUUID, Block block, String replaceMaterial, String replaceData) {
        Objects.requireNonNull(block, "block");
        final Location loc = block.getLocation();
        final World world = Objects.requireNonNull(loc.getWorld(), "world");
        final Material mat = block.getType();

        final String origMaterial = mat.name();

        final String origDataString = serializeOriginalBlockData(block);

        SkullSnapshot skull = null;
        if (mat == Material.PLAYER_HEAD || mat == Material.PLAYER_WALL_HEAD || mat.name().equals("SKULL")) {
            skull = readSkullSnapshot(block);
        }

        return new BlockData(
                new Location(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                graveUUID,
                replaceMaterial,
                replaceData,
                origMaterial,
                origDataString,
                skull != null ? skull.rotationFace : null,
                skull != null ? skull.ownerUuid : null,
                skull != null ? skull.ownerName : null,
                skull != null ? skull.texturesValue : null,
                skull != null ? skull.texturesSignature : null
        );
    }

    private static String serializeOriginalBlockData(Block block) {
        try {
            org.bukkit.block.data.BlockData bd = block.getBlockData();
            try {
                Method m = bd.getClass().getMethod("getAsString");
                Object s = m.invoke(bd);
                if (s instanceof String) return (String) s;
            } catch (NoSuchMethodException ignore) {
                try {
                    Method m2 = bd.getClass().getMethod("getAsString", boolean.class);
                    Object s2 = m2.invoke(bd, false);
                    if (s2 instanceof String) return (String) s2;
                } catch (Throwable ignore2) { /* fall through */ }
            }
        } catch (Throwable ignore) {
            // Not 1.13+ API or unavailable
        }

        String mat = block.getType().name();
        String dataPart = "0";
        try {
            Method getData = Block.class.getMethod("getData");
            Object d = getData.invoke(block);
            if (d instanceof Byte) dataPart = Byte.toString((Byte) d);
            else if (d != null) dataPart = d.toString();
        } catch (Throwable ignore) {
            // no legacy getData; keep "0"
        }
        return "LEGACY:" + mat + ":" + dataPart;
    }

    private static SkullSnapshot readSkullSnapshot(Block block) {
        try {
            if (!(block.getState() instanceof Skull)) return null;
            Skull skull = (Skull) block.getState();

            String face = "SELF";
            try {
                BlockFace rot = BlockFace.fromBukkit(skull.getRotation());
                if (rot != null) face = rot.name();
            } catch (Throwable ignore) {
                /* older impls may differ */
            }

            Field f = ((Object) skull).getClass().getDeclaredField("profile");
            f.setAccessible(true);
            Object gameProfile = f.get(skull);
            if (gameProfile == null) return new SkullSnapshot(face, null, null, null, null);

            Class<?> gpClass = gameProfile.getClass();
            Method getId = gpClass.getMethod("getId");
            Method getName = gpClass.getMethod("getName");
            Method getProperties = gpClass.getMethod("getProperties");

            UUID id = (UUID) getId.invoke(gameProfile);
            String name = (String) getName.invoke(gameProfile);
            Object props = getProperties.invoke(gameProfile); // PropertyMap

            Method get = props.getClass().getMethod("get", Object.class);
            Object collection = get.invoke(props, "textures");
            String value = null;
            String sig = null;

            if (collection instanceof Collection) {
                Collection<?> values = (Collection<?>) collection;
                if (!values.isEmpty()) {
                    Object prop = values.iterator().next();
                    Method getValue = prop.getClass().getMethod("getValue");
                    value = (String) getValue.invoke(prop);
                    try {
                        Method getSignature = prop.getClass().getMethod("getSignature");
                        sig = (String) getSignature.invoke(prop);
                    } catch (NoSuchMethodException ignored) {
                        // ignored
                    }
                }
            }

            if (id == null || name == null) {
                try {
                    Method getOwningPlayer = Skull.class.getMethod("getOwningPlayer");
                    Object owning = getOwningPlayer.invoke(skull);
                    if (owning != null) {
                        if (id == null) {
                            Method getUniqueId = owning.getClass().getMethod("getUniqueId");
                            id = (UUID) getUniqueId.invoke(owning);
                        }
                        if (name == null) {
                            getName = owning.getClass().getMethod("getName");
                            name = (String) getName.invoke(owning);
                        }
                    }
                } catch (Throwable ignored) {
                    try {
                        Method getOwner = Skull.class.getMethod("getOwner");
                        String n = (String) getOwner.invoke(skull);
                        if (name == null) name = n;
                    } catch (Throwable ignored2) {}
                }
            }

            return new SkullSnapshot(face, id, name, value, sig);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class SkullSnapshot {
        final String rotationFace;
        final UUID ownerUuid;
        final String ownerName;
        final String texturesValue;
        final String texturesSignature;

        SkullSnapshot(String rotationFace, UUID ownerUuid, String ownerName, String texturesValue, String texturesSignature) {
            this.rotationFace = rotationFace;
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.texturesValue = texturesValue;
            this.texturesSignature = texturesSignature;
        }
    }
}