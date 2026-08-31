package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.util.CustomModelDataUtil;
import me.imdanix.text.MiniTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing MiniMessage formatted strings.
 *
 * <p>Adventure and MiniMessage are provided natively by Paper.</p>
 */
public class MiniMessage {

    private static final TagResolver STANDARD_TAGS =
            TagResolver.builder()
                    .resolver(StandardTags.defaults())
                    .resolver(StandardTags.hoverEvent())
                    .resolver(StandardTags.clickEvent())
                    .resolver(StandardTags.insertion())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.newline())
                    .resolver(StandardTags.score())
                    .resolver(StandardTags.nbt())
                    .resolver(StandardTags.font())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.keybind())
                    .resolver(StandardTags.selector())
                    .resolver(StandardTags.transition())
                    .resolver(StandardTags.translatable())
                    .resolver(StandardTags.translatableFallback())
                    .resolver(StandardTags.reset())
                    .resolver(StandardTags.pride())
                    .resolver(StandardTags.shadowColor())
                    .build();

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
        String color = MiniTranslator.toMini(
                legacyText,
                MiniTranslator.Option.COLOR
        );

        String format = MiniTranslator.toMini(
                color,
                MiniTranslator.Option.FORMAT
        );

        String gradient = MiniTranslator.toMini(
                format,
                MiniTranslator.Option.GRADIENT
        );

        String closeColor = MiniTranslator.toMini(
                gradient,
                MiniTranslator.Option.CLOSE_COLORS
        );

        String hexColors = MiniTranslator.toMini(
                closeColor,
                MiniTranslator.Option.HEX_COLOR_STANDALONE
        );

        String doubleToEscape = MiniTranslator.toMini(
                hexColors,
                MiniTranslator.Option.DOUBLE_TO_ESCAPE
        );

        String verboseHexColor = MiniTranslator.toMini(
                doubleToEscape,
                MiniTranslator.Option.VERBOSE_HEX_COLOR
        );

        return MiniTranslator.toMini(
                verboseHexColor,
                MiniTranslator.Option.FAST_RESET
        );
    }

    /**
     * Converts legacy text directly into an Adventure Component.
     *
     * @param legacyText legacy text
     * @return Adventure Component
     */
    public static Component convertLegacyToComponent(String legacyText) {
        String miniMessageText = MiniTranslator.toMini(
                legacyText,
                MiniTranslator.Option.COLOR,
                MiniTranslator.Option.FORMAT,
                MiniTranslator.Option.GRADIENT,
                MiniTranslator.Option.CLOSE_COLORS,
                MiniTranslator.Option.HEX_COLOR_STANDALONE,
                MiniTranslator.Option.DOUBLE_TO_ESCAPE,
                MiniTranslator.Option.VERBOSE_HEX_COLOR,
                MiniTranslator.Option.FAST_RESET
        );

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
     * Sends a formatted message to a player using Paper's native Adventure API.
     *
     * @param player  player receiving the message
     * @param message legacy-formatted message
     */
    public static void sendMessage(final Player player, final String message) {
        String output = convertLegacyToMiniMessage(message);
        player.sendMessage(LEGACY_COMPONENT_SERIALIZER.serialize(MINI_MESSAGE.deserialize(output)));
    }

    /**
     * Gets the shared MiniMessage instance.
     *
     * @return MiniMessage instance
     */
    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }
}