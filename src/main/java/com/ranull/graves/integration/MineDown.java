package com.ranull.graves.integration;

/**
 * Utility class for parsing strings with MineDown formatting.
 */
public class MineDown {

    public MineDown() {}

    /**
     * Parses a MineDown formatted string into a legacy text format.
     *
     * @param string The MineDown formatted string to parse.
     * @return The legacy text representation of the MineDown formatted string.
     */
    public static String parseString(String string) {
        return new de.themoep.minedown.adventure.MineDown(string).message();
    }
}