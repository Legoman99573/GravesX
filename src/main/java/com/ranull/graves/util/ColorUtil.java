package com.ranull.graves.util;

import org.bukkit.Color;

/**
 * Utility class for handling color operations.
 */
public final class ColorUtil {

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
}