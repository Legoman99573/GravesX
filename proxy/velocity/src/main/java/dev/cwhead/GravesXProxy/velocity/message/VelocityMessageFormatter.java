package dev.cwhead.GravesXProxy.velocity.message;

import dev.cwhead.GravesXCommon.MiniMessageTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Turns config/command text into Adventure components (MiniMessage or legacy '&' codes). */
public final class VelocityMessageFormatter {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private VelocityMessageFormatter() {}

    public static Component format(String text, boolean useMiniMessage) {
        if (useMiniMessage) {
            // legacy '&' codes are translated to MiniMessage tags so both styles work together
            return MINI.deserialize(MiniMessageTranslator.toMiniMessage(text));
        }
        return LEGACY.deserialize(text.replace('&', '§'));
    }
}
