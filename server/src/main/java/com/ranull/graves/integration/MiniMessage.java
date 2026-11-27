package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.util.CustomModelDataUtil;
import dev.cwhead.GravesXCommon.MiniMessageTranslator;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing MiniMessage formatted strings.
 *
 * <p>Adventure and MiniMessage are provided natively by Paper.</p>
 */
public class MiniMessage {

    private static final TagResolver STANDARD_TAGS = buildStandardTags();

    private static final net.kyori.adventure.text.minimessage.MiniMessage MINI_MESSAGE =
            net.kyori.adventure.text.minimessage.MiniMessage.builder()
                    .strict(false)
                    .tags(STANDARD_TAGS)
                    .build();

    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .extractUrls()
                    .build();

    /**
     * Creates a MiniMessage utility instance.
     */
    public MiniMessage() {
    }

    /**
     * Parses a list of MiniMessage formatted strings into legacy text.
     *
     * @param strings the MiniMessage strings
     * @return the parsed legacy strings
     */
    public static List<String> parseString(List<String> strings) {
        List<String> parseStrings = new ArrayList<>(strings.size());

        for (String string : strings) {
            parseStrings.add(parseString(string));
        }

        return parseStrings;
    }

    /**
     * Parses a MiniMessage formatted string into legacy text.
     *
     * @param string the MiniMessage string
     * @return the parsed legacy string
     */
    public static String parseString(String string) {
        return LEGACY_COMPONENT_SERIALIZER.serialize(
                MINI_MESSAGE.deserialize(string)
        );
    }

    /**
     * Converts legacy color/format codes into MiniMessage format.
     *
     * @param legacyTexts legacy strings
     * @return converted MiniMessage strings
     */
    public static List<String> convertLegacyToMiniMessage(List<String> legacyTexts) {
        List<String> convertedTexts = new ArrayList<>(legacyTexts.size());

        for (String legacyText : legacyTexts) {
            convertedTexts.add(convertLegacyToMiniMessage(legacyText));
        }

        return convertedTexts;
    }

    /**
     * Converts legacy color/format codes into MiniMessage format.
     *
     * @param legacyText legacy string
     * @return converted MiniMessage string
     */
    public static String convertLegacyToMiniMessage(String legacyText) {
        return MiniMessageTranslator.toMiniMessage(legacyText);
    }

    /**
     * Converts legacy text directly into an Adventure Component.
     *
     * @param legacyText legacy text
     * @return Adventure Component
     */
    public static Component convertLegacyToComponent(String legacyText) {
        return MINI_MESSAGE.deserialize(MiniMessageTranslator.toMiniMessage(legacyText));
    }

    /**
     * Deserializes MiniMessage text into an Adventure Component without converting legacy codes.
     *
     * @param miniMessageText MiniMessage text
     * @return Adventure Component
     */
    public static Component parseComponent(String miniMessageText) {
        return MINI_MESSAGE.deserialize(miniMessageText);
    }

    /**
     * Formats a written book's metadata using Adventure Components.
     *
     * @param plugin    Graves plugin instance
     * @param grave     Grave instance
     * @param itemStack  item stack
     * @param title     book title
     * @param author    book author
     * @param pages     book pages
     * @param lore      book lore
     * @return updated ItemStack
     */
    public static ItemStack formatBookMeta(
            Graves plugin,
            Grave grave,
            ItemStack itemStack,
            Component title,
            Component author,
            List<Component> pages,
            List<Component> lore
    ) {
        if (itemStack == null || !(itemStack.getItemMeta() instanceof BookMeta bookMeta)) {
            return itemStack;
        }

        String titleString = LEGACY_COMPONENT_SERIALIZER.serialize(title);
        String authorString = LEGACY_COMPONENT_SERIALIZER.serialize(author);

        bookMeta.setTitle(titleString);
        bookMeta.setAuthor(authorString);
        List<String> serializedPages = pages.stream()
                .map(LEGACY_COMPONENT_SERIALIZER::serialize)
                .toList();
        bookMeta.setPages(serializedPages);

        List<String> serializedLore = lore.stream()
                .map(LEGACY_COMPONENT_SERIALIZER::serialize)
                .toList();
        bookMeta.setLore(serializedLore);

        int customModelData = plugin
                .getConfigManager()
                .getConfigSection("obituary.model-data", grave)
                .getInt("obituary.model-data", -1);

        CustomModelDataUtil.applyCustomModelData(bookMeta, customModelData);

        itemStack.setItemMeta(bookMeta);

        return itemStack;
    }

    /**
     * Sends a MiniMessage message to a player as a native Adventure Component.
     *
     * @param player  player receiving the message
     * @param message the MiniMessage text
     */
    public static void sendMessage(final Player player, final String message) {
        final Component component = MINI_MESSAGE.deserialize(message);

        if (player instanceof Audience audience) {
            audience.sendMessage(component);
            return;
        }

        player.sendMessage(LEGACY_COMPONENT_SERIALIZER.serialize(component));
    }

    /**
     * Gets the shared MiniMessage instance.
     *
     * @return MiniMessage instance
     */
    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    /**
     * Builds a {@link TagResolver} containing every standard tag the running server provides.
     *
     * @return a resolver covering all standard tags available on this server
     */
    private static TagResolver buildStandardTags() {
        TagResolver.Builder builder = TagResolver.builder().resolver(StandardTags.defaults());

        for (Method method : StandardTags.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0 || !TagResolver.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            try {
                builder.resolver((TagResolver) method.invoke(null));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // A resolver this server cannot provide is skipped; the others still load.
            }
        }

        return builder.build();
    }
}