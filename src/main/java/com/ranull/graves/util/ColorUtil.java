package com.ranull.graves.util;

import org.bukkit.Color;
import org.bukkit.Particle;

/**
 * Utility class for handling color operations, including particle dust colors.
 */
public final class ColorUtil {

    // Private constructor to prevent instantiation
    private ColorUtil() {}

    /**
     * Gets the {@link Color} corresponding to the given color name.
     *
     * @param colorName The name of the color as a string.
     * @return The {@link Color} corresponding to the given name, or {@code null} if no match is found.
     */
    public static Color getColor(String colorName) {
        switch (colorName.toUpperCase()) {
            case "AQUA":
                return Color.AQUA;
            case "BLACK":
                return Color.BLACK;
            case "BLUE":
                return Color.BLUE;
            case "FUCHSIA":
                return Color.FUCHSIA;
            case "GRAY":
                return Color.GRAY;
            case "GREEN":
                return Color.GREEN;
            case "LIME":
                return Color.LIME;
            case "MAROON":
                return Color.MAROON;
            case "NAVY":
                return Color.NAVY;
            case "OLIVE":
                return Color.OLIVE;
            case "ORANGE":
                return Color.ORANGE;
            case "PURPLE":
                return Color.PURPLE;
            case "RED":
                return Color.RED;
            case "SILVER":
                return Color.SILVER;
            case "TEAL":
                return Color.TEAL;
            case "WHITE":
                return Color.WHITE;
            case "YELLOW":
                return Color.YELLOW;
            default:
                return null;
        }
    }

    /**
     * Parses a hex color code to a {@link Color} for use with particle dust options.
     * <p>
     * Minecraft 1.16+ supports hex color codes for particle dust. This method parses a hex color code
     * in the format #RRGGBB and returns the corresponding {@link Color}.
     * </p>
     *
     * @param hex The hex color code as a string (e.g., "#FF5733").
     * @return The {@link Color} corresponding to the hex color code, or {@code null} if the code is invalid.
     */
    public static Color getColorFromHex(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return null; // Invalid hex format
        }

        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return Color.fromRGB(r, g, b);
        } catch (NumberFormatException e) {
            return null; // Invalid hex code
        }
    }

    /**
     * Creates a {@link Particle.DustOptions} object using a hex color code.
     *
     * @param hexColor The hex color code as a string (e.g., "#FF5733").
     * @param size The size of the dust particle.
     * @return A {@link Particle.DustOptions} object with the specified color and size, or {@code null} if the color code is invalid.
     */
    public static Particle.DustOptions createDustOptionsFromHex(String hexColor, float size) {
        Color color = getColorFromHex(hexColor);
        if (color == null) {
            return null; // Invalid color code
        }
        return new Particle.DustOptions(color, size);
    }
}