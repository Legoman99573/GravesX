package com.ranull.graves.util;

import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

/**
 * Utility class for handling BlockFace related operations.
 */
public final class BlockFaceUtil {

    private BlockFaceUtil() {}

    /**
     * Simplifies the given BlockFace to one of the four cardinal directions (NORTH, EAST, SOUTH, WEST).
     *
     * @param blockFace The BlockFace to simplify.
     * @return The simplified BlockFace.
     */
    public static BlockFace getSimpleBlockFace(BlockFace blockFace) {
        return switch (blockFace) {
            case EAST,
                 NORTH_EAST -> BlockFace.EAST;
            case SOUTH,
                 SOUTH_EAST -> BlockFace.SOUTH;
            case WEST,
                 SOUTH_WEST -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    /**
     * Gets the BlockFace corresponding to the yaw of the given LivingEntity.
     *
     * @param livingEntity The LivingEntity whose yaw is used.
     * @return The BlockFace corresponding to the yaw.
     */
    public static BlockFace getEntityYawBlockFace(LivingEntity livingEntity) {
        return getYawBlockFace(livingEntity.getLocation().getYaw());
    }

    /**
     * Gets the rotation corresponding to the given BlockFace.
     *
     * @param blockFace The BlockFace to convert.
     * @return The corresponding Rotation.
     */
    public static Rotation getBlockFaceRotation(BlockFace blockFace) {
        return switch (blockFace) {
            case SOUTH ->
                    Rotation.NONE;
            case SOUTH_WEST ->
                    Rotation.CLOCKWISE;
            case WEST ->
                    Rotation.CLOCKWISE_45;
            case NORTH_WEST ->
                    Rotation.CLOCKWISE_135;
            case EAST ->
                    Rotation.COUNTER_CLOCKWISE;
            case NORTH_EAST ->
                    Rotation.FLIPPED_45;
            case SOUTH_EAST ->
                    Rotation.COUNTER_CLOCKWISE_45;
            default -> Rotation.FLIPPED;
        };
    }

    /**
     * Converts a yaw angle to the corresponding BlockFace.
     *
     * @param yaw The yaw angle to convert.
     * @return The corresponding BlockFace.
     */
    public static BlockFace getYawBlockFace(float yaw) {
        float direction = yaw % 360;
        if (direction < 0) direction += 360;

        return switch (Math.round(direction / 45)) {
            case 1 -> BlockFace.SOUTH_WEST;
            case 2 -> BlockFace.WEST;
            case 3 -> BlockFace.NORTH_WEST;
            case 4 -> BlockFace.NORTH;
            case 5 -> BlockFace.NORTH_EAST;
            case 6 -> BlockFace.EAST;
            case 7 -> BlockFace.SOUTH_EAST;
            default -> BlockFace.SOUTH;
        };
    }

    /**
     * Converts a BlockFace to the corresponding yaw angle.
     *
     * @param blockFace The BlockFace to convert.
     * @return The corresponding yaw angle.
     */
    public static int getBlockFaceYaw(BlockFace blockFace) {
        return switch (blockFace) {
            case SOUTH -> 0;
            case SOUTH_WEST -> 45;
            case WEST -> 90;
            case NORTH_WEST -> 135;
            case EAST -> -90;
            case NORTH_EAST -> -135;
            case SOUTH_EAST -> -45;
            default -> 180;
        };
    }
}
