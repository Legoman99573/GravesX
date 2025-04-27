package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import me.imdanix.text.MiniTranslator;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for parsing MiniMessage formatted strings into legacy text format.
 */
public final class MiniMessage {
    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage;
    private static BukkitAudiences audiences;
    private static LegacyComponentSerializer legacyComponentSerializer;



    /**
     * Initializes a new MiniMessage instance. Attempts to instantiate the MiniMessage parser.
     * If the MiniMessage class is not found, the instance will be null.
     */
    public MiniMessage() {
        audiences = BukkitAudiences.create(JavaPlugin.getPlugin(Graves.class));
        legacyComponentSerializer = LegacyComponentSerializer.legacySection();
        miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.builder()
                .strict(false)
                .tags(
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
                                .build()
                ).build();
    }

    /**
     * Parses a list of MiniMessage formatted strings into legacy text formats.
     *
     * @param strings The list of MiniMessage formatted strings to parse.
     * @return A list of legacy text representations of the MiniMessage formatted strings.
     *         If MiniMessage is not initialized, returns the original list of strings.
     */
    public static List<String> parseString(List<String> strings) {
        List<String> parseStrings = new ArrayList<>();
        for (String string : strings) {
            parseStrings.add(parseString(string));
        }
        return parseStrings;
    }

    /**
     * Parses a MiniMessage formatted string into a legacy text format.
     *
     * @param string The MiniMessage formatted string to parse.
     * @return The legacy text representation of the MiniMessage formatted string.
     *         If MiniMessage is not initialized, returns the original string.
     */
    public static String parseString(String string) {
        return (miniMessage != null) ?
                legacyComponentSerializer.serialize(miniMessage.deserialize(string)) :
                string;
    }

    /**
     * Attempts to convert legacy color codes to Kyori's Adventure Text MiniMessage format.
     * @param legacyTexts    The Legacy Text to be converted.
     * @return              All text to be converted to StringBuilder, that is required by net.kyori.adventure.text.minimessage
     */
    public static List<String> convertLegacyToMiniMessage(List<String> legacyTexts) {
        List<String> convertedTexts = new ArrayList<>();
        for (String legacyText : legacyTexts) {
            convertedTexts.add(convertLegacyToMiniMessage(legacyText));
        }
        return convertedTexts;
    }

    /**
     * Attempts to convert legacy color codes to Kyori's Adventure Text MiniMessage format.
     * @param legacyText    The Legacy Text to be converted.
     * @return              All text to be converted to StringBuilder, that is required by net.kyori.adventure.text.minimessage
     */
    public static String convertLegacyToMiniMessage(String legacyText) {
        String color = MiniTranslator.toMini(legacyText, MiniTranslator.Option.COLOR);
        String format = MiniTranslator.toMini(color, MiniTranslator.Option.FORMAT);
        String gradient = MiniTranslator.toMini(format, MiniTranslator.Option.GRADIENT);
        String close_color = MiniTranslator.toMini(gradient, MiniTranslator.Option.CLOSE_COLORS);
        String hex_colors = MiniTranslator.toMini(close_color, MiniTranslator.Option.HEX_COLOR_STANDALONE);
        String double_to_escape = MiniTranslator.toMini(hex_colors, MiniTranslator.Option.DOUBLE_TO_ESCAPE);
        String verbose_hex_color = MiniTranslator.toMini(double_to_escape, MiniTranslator.Option.VERBOSE_HEX_COLOR);
        return MiniTranslator.toMini(verbose_hex_color, MiniTranslator.Option.FAST_RESET);
    }

    /**
     * Attempts to convert legacy color codes to Kyori's Adventure Text MiniMessage format.
     * @param legacyText    The Legacy Text to be converted.
     * @return              All text to be converted to StringBuilder, that is required by net.kyori.adventure.text.minimessage
     */
    public static Component convertLegacyToComponent(String legacyText) {
        String miniMessageText = MiniTranslator.toMini(legacyText,
                MiniTranslator.Option.COLOR,
                MiniTranslator.Option.FORMAT,
                MiniTranslator.Option.GRADIENT,
                MiniTranslator.Option.CLOSE_COLORS,
                MiniTranslator.Option.HEX_COLOR_STANDALONE,
                MiniTranslator.Option.DOUBLE_TO_ESCAPE,
                MiniTranslator.Option.VERBOSE_HEX_COLOR,
                MiniTranslator.Option.FAST_RESET
        );
        return MiniMessage.miniMessage().deserialize(miniMessageText);
    }

    public static ItemStack formatBookMeta(ItemStack itemStack, Component title, Component author, List<Component> pages, List<Component> lore) {
        if (itemStack == null || !(itemStack.getItemMeta() instanceof BookMeta)) {
            return itemStack;
        }

        BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();

        if (bookMeta == null) {
            return itemStack;
        }

        LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

        String titleString = legacySerializer.serialize(title);
        String authorString = legacySerializer.serialize(author);

        bookMeta.setTitle(titleString);
        bookMeta.setAuthor(authorString);

        List<String> serializedPages = pages.stream()
                .map(legacySerializer::serialize)
                .collect(Collectors.toList());
        bookMeta.setPages(serializedPages);

        List<String> serializedLore = lore.stream()
                .map(legacySerializer::serialize)
                .collect(Collectors.toList());
        bookMeta.setLore(serializedLore);

        itemStack.setItemMeta(bookMeta);
        return itemStack;
    }


    public static void sendMessage(final Player player, final String message) {
        if (miniMessage != null && audiences != null) {
            String output = convertLegacyToMiniMessage(message);
            audiences.sender(player).sendMessage(miniMessage.deserialize(output));
            return;
        }
        player.sendMessage(message);
    }

    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    }
}