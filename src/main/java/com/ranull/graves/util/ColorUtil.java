package com.ranull.graves.util;

import org.bukkit.Color;
import org.bukkit.Particle;

import java.util.Locale;

/**
 * Utility class for handling color operations, including particle dust colors.
 */
public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Gets the {@link Color} corresponding to the given color name.
     *
     * @param colorName The name of the color as a string.
     * @return The {@link Color} corresponding to the given name, or {@code null} if no match is found.
     */
    public static Color getColor(String colorName) {
        if (colorName == null || colorName.isEmpty()) return null;

        String s = colorName;
        if (s.startsWith("#")) {
            Color hex = getColorFromHex(s);
            if (hex != null) return hex;
            s = "RED"; // fallback if bad hex
        }

        return switch (s.toUpperCase(Locale.ROOT)) {
            case "AQUA"    -> Color.AQUA;
            case "BLACK"   -> Color.BLACK;
            case "BLUE"    -> Color.BLUE;
            case "FUCHSIA" -> Color.FUCHSIA;
            case "GRAY"    -> Color.GRAY;
            case "GREEN"   -> Color.GREEN;
            case "LIME"    -> Color.LIME;
            case "MAROON"  -> Color.MAROON;
            case "NAVY"    -> Color.NAVY;
            case "OLIVE"   -> Color.OLIVE;
            case "ORANGE"  -> Color.ORANGE;
            case "PURPLE"  -> Color.PURPLE;
            case "RED"     -> Color.RED;
            case "SILVER"  -> Color.SILVER;
            case "TEAL"    -> Color.TEAL;
            case "WHITE"   -> Color.WHITE;
            case "YELLOW"  -> Color.YELLOW;
            default        -> null;
        };
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
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') return null;

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
        return (color == null) ? null : new Particle.DustOptions(color, size);
    }
}