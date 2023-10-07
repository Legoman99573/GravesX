package com.ranull.graves.integration;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MiniMessage {
    private net.kyori.adventure.text.minimessage.MiniMessage miniMessage;

    public MiniMessage() {
        try {
            miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        } catch (NoSuchMethodError noSuchMethodError) {}
    }
    public String parseString(String string) {
        return (miniMessage != null) ? LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(string)) : string;
    }
}