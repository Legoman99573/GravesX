package com.ranull.graves.integration;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility class for parsing MiniMessage formatted strings into legacy text format.
 */
public final class MiniMessage {
    private net.kyori.adventure.text.minimessage.MiniMessage miniMessage;

    /**
     * Initializes a new MiniMessage instance. Attempts to instantiate the MiniMessage parser.
     * If the MiniMessage class is not found, the instance will be null.
     */
    public MiniMessage() {
        try {
            miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        } catch (NoSuchMethodError noSuchMethodError) {
            // MiniMessage class not found or method not available
        }
    }

    /**
     * Parses a MiniMessage formatted string into a legacy text format.
     *
     * @param string The MiniMessage formatted string to parse.
     * @return The legacy text representation of the MiniMessage formatted string.
     *         If MiniMessage is not initialized, returns the original string.
     */
    public String parseString(String string) {  //TODO Properly handle stuff like hover text, clickable links, etc. PR appreciated if someone can make this work.
        return (miniMessage != null) ?
                LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(string)) :
                string;
    }
}