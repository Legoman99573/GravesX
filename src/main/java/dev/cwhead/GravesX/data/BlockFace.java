package dev.cwhead.GravesX.data;

import java.util.Locale;

/**
 * Serializable direction enum for {@link org.bukkit.block.BlockFace}.
 */
public enum BlockFace {
    /**
     * Block Facing North x:0, y:0, z:-1
     */
    NORTH(0, 0, -1),

    /**
     * Block Facing East x:1, y:0, z:0
     */
    EAST(1, 0, 0),
    /**
     * Block Facing South x:0, y:0, z:1
     */
    SOUTH(0, 0, 1),

    /**
     * Block Facing West x:-1, y:0, z:0
     */
    WEST(-1, 0, 0),

    /**
     * Block Facing Up x:0, y:1, z:0
     */
    UP(0, 1, 0),

    /**
     * Block Facing Down x:0, y:0, z:1
     */
    DOWN(0, -1, 0),

    /**
     * Block Facing North East x:1, y:0, z:-1
     */
    NORTH_EAST(1, 0, -1),

    /**
     * Block Facing South East x:1, y:0, z:1
     */
    SOUTH_EAST(1, 0, 1),

    /**
     * Block Facing South West x:-1, y:0, z:1
     */
    SOUTH_WEST(-1, 0, 1),

    /**
     * Block Facing North West x:-1, y:0, z:-1
     */
    NORTH_WEST(-1, 0, -1),

    /**
     * Block Facing Self x:0, y:0, z:0
     */
    SELF(0, 0, 0);

    /**
     * Value for X-axis
     */
    private final int modX;

    /**
     * Value for Y-axis
     */
    private final int modY;

    /**
     * Value for Z-axis
     */
    private final int modZ;

    /**
     * Constructs a new {@link BlockFace}
     * @param modX the X position of BlockFace
     * @param modY the Y position of BlockFace
     * @param modZ the Z position of BlockFace
     */
    BlockFace(int modX, int modY, int modZ) {
        this.modX = modX;
        this.modY = modY;
        this.modZ = modZ;
    }

    /**
     * Gets the X-axis modifier.
     *
     * @return the X-axis
     */
    public int getModX() {
        return modX;
    }

    /**
     * Gets the Y-axis modifier.
     *
     * @return the Y-axis
     */
    public int getModY() {
        return modY;
    }

    /**
     * Gets the Z-axis modifier.
     *
     * @return the Z-axis
     */
    public int getModZ() {
        return modZ;
    }

    /**
     * Checks if BlockFace is Cardinal
     *
     * @return true if this is one of the 4 horizontal cardinals (N/E/S/W).
     */
    public boolean isCardinal() {
        return this == NORTH
                || this == EAST
                || this == SOUTH
                || this == WEST;
    }

    /**
     * Checks if BlockFace is Veritcal
     *
     * @return true if this is strictly vertical (UP/DOWN).
     */
    public boolean isVertical() {
        return this == UP
                || this == DOWN;
    }

    /**
     * Checks if BlockFace is Diagonal.
     *
     * @return true if this is one of the 4 horizontal diagonals.
     */
    public boolean isDiagonal() {
        return this == NORTH_EAST
                || this == SOUTH_EAST
                || this == SOUTH_WEST
                || this == NORTH_WEST;
    }

    /**
     * Gets the opposite direction of a BlockFace
     *
     * @return the opposite direction (SELF returns SELF).
     */
    public BlockFace getOpposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case EAST:
                return WEST;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case NORTH_EAST:
                return SOUTH_WEST;
            case SOUTH_EAST:
                return NORTH_WEST;
            case SOUTH_WEST:
                return NORTH_EAST;
            case NORTH_WEST:
                return SOUTH_EAST;
            default:
                return SELF;
        }
    }

    /**
     * Rotate this face around the Y-axis by 90° steps (clockwise looking down from +Y).
     * Only affects horizontal cardinals/diagonals. Others return unchanged.
     *
     * @param quarterTurns number of clockwise 90° turns (can be negative)
     * @return rotated BlockFace
     */
    public BlockFace rotateY(int quarterTurns) {
        int turns = ((quarterTurns % 4) + 4) % 4; // normalize to [0..3]
        BlockFace face = this;
        for (int i = 0; i < turns; i++) {
            switch (face) {
                case NORTH:
                    face = EAST;
                    break;
                case EAST:
                    face = SOUTH;
                    break;
                case SOUTH:
                    face = WEST;
                    break;
                case WEST:
                    face = NORTH;
                    break;

                case NORTH_EAST:
                    face = SOUTH_EAST;
                    break;
                case SOUTH_EAST:
                    face = SOUTH_WEST;
                    break;
                case SOUTH_WEST:
                    face = NORTH_WEST;
                    break;
                case NORTH_WEST:
                    face = NORTH_EAST;
                    break;

                default: // UP, DOWN, SELF
                    return face;
            }
        }
        return face;
    }

    /**
     * Safe parser. Accepts null/unknown and returns SELF instead of throwing.
     * @param name enum name (case-insensitive)
     * @return parsed face or SELF
     */
    public static BlockFace fromString(String name) {
        if (name == null) return SELF;
        try {
            return BlockFace.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SELF;
        }
    }

    /**
     * Convert from Bukkit's org.bukkit.block.BlockFace to this enum.
     * @param bukkitFace Bukkit BlockFace (nullable)
     * @return equivalent BlockFace, or SELF if null/unknown
     */
    public static BlockFace fromBukkit(org.bukkit.block.BlockFace bukkitFace) {
        if (bukkitFace == null) return SELF;
        return fromString(bukkitFace.name());
    }

    /**
     * Convert to Bukkit's org.bukkit.block.BlockFace (falls back to SELF if not present there).
     * @return Bukkit BlockFace
     */
    public org.bukkit.block.BlockFace toBukkit() {
        try {
            return org.bukkit.block.BlockFace.valueOf(this.name());
        } catch (IllegalArgumentException ex) {
            return org.bukkit.block.BlockFace.SELF;
        }
    }
}
