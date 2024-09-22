package com.ranull.graves.integration;

/**
 * Utility class for parsing strings with MineDown formatting.
 */
public final class MineDown {
    /**
     * Parses a MineDown formatted string into a legacy text format.
     *
     * @param string The MineDown formatted string to parse.
     * @return The legacy text representation of the MineDown formatted string.
     */
    public String parseString(String string) {
        return new de.themoep.minedown.adventure.MineDown(string).message();
    }
}