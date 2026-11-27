package dev.cwhead.GravesX.util;

import dev.cwhead.GravesXCommon.MiniMessageTranslator;

import java.util.List;

/**
 * Converts legacy formatting codes into MiniMessage.
 *
 * @deprecated moved to the shared {@link MiniMessageTranslator} (module {@code gravesx-common}), which the
 *             proxy plugins use as well. This class only delegates to it and is kept so existing callers keep working.
 */
@Deprecated
public final class LegacyToMiniMessageUtil {
    private LegacyToMiniMessageUtil() {}

    /** @see MiniMessageTranslator#toMiniMessage(String) */
    public static String toMini(String text) {
        return MiniMessageTranslator.toMiniMessage(text);
    }

    /** @see MiniMessageTranslator#toMiniMessageAll(List) */
    public static List<String> toMiniAll(List<String> lines) {
        return MiniMessageTranslator.toMiniMessageAll(lines);
    }

    /** @see MiniMessageTranslator#toMiniMessage(String, boolean) */
    public static String toMini(String text, boolean standaloneHex) {
        return MiniMessageTranslator.toMiniMessage(text, standaloneHex);
    }
}
