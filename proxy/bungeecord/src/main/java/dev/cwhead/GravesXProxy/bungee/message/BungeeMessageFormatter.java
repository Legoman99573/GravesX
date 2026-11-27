package dev.cwhead.GravesXProxy.bungee.message;

import dev.cwhead.GravesXCommon.MiniMessageTranslator;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Turns config/command text into BungeeCord components (MiniMessage subset or legacy '&' codes).
 */
public final class BungeeMessageFormatter {
    private BungeeMessageFormatter() {}

    public static BaseComponent[] format(String text, boolean useMiniMessage) {
        String legacy = useMiniMessage ? MiniMessageTranslator.toLegacy(text) : text;

        return TextComponent.fromLegacyText(legacy.replace('&', '§'));
    }
}
