package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Loads community translations and selects the player's Minecraft client locale. */
public final class LanguageManager {
    private static final String ENGLISH = "en-US";
    private static final Pattern CODE = Pattern.compile("[a-zA-Z]{2,8}[-_][a-zA-Z0-9]{2,8}");
    private final Graves plugin;
    private final Map<String, YamlConfiguration> translations = new HashMap<>();
    private YamlConfiguration english;

    public LanguageManager(Graves plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reloads every YAML file; absent translation keys fall back to English. */
    public void reload() {
        File folder = new File(plugin.getDataFolder(), "config/language");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create language folder: " + folder);
        }
        File defaultFile = new File(folder, ENGLISH + ".yml");
        if (!defaultFile.isFile()) {
            try (InputStream resource = plugin.getResource("config/language/en-US.yml")) {
                if (resource != null) Files.copy(resource, defaultFile.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Could not extract English translations: " + e.getMessage());
            }
        }
        translations.clear();
        File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml"));
        if (files != null) for (File file : files) {
            String code = file.getName().substring(0, file.getName().length() - 4);
            if (!CODE.matcher(code).matches()) continue;
            translations.put(normalize(code), YamlConfiguration.loadConfiguration(file));
        }
        english = translations.getOrDefault(normalize(ENGLISH), new YamlConfiguration());
        try (InputStream resource = plugin.getResource("config/language/en-US.yml")) {
            if (resource != null) {
                var defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8));
                english.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load bundled translations: " + e.getMessage());
        }
    }

    private static String normalize(String locale) {
        return locale.replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private YamlConfiguration select(@Nullable Player player) {
        String defaultLocale = plugin.getConfig().getString("settings.language.default", ENGLISH);
        YamlConfiguration fallback = translations.getOrDefault(normalize(defaultLocale), english);
        if (player == null) return fallback;
        // Player#getLocale is the supported Bukkit API for the client-selected locale.
        String locale = normalize(player.getLocale());
        YamlConfiguration exact = translations.get(locale);
        if (exact != null) return exact;
        // Prefer a matching language variant when the client's region is unavailable.
        String language = locale.split("-", 2)[0];
        String preferred = normalize(defaultLocale);
        if (preferred.startsWith(language + "-")) return fallback;
        return translations.entrySet().stream().filter(entry -> entry.getKey().startsWith(language + "-"))
                .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).findFirst().orElse(fallback);
    }

    /** Gets a localized message, preserving intentionally empty overrides. */
    public String get(@Nullable Player player, String path) {
        YamlConfiguration selected = select(player);
        if (selected.contains(path, true)) return selected.getString(path, "");
        if (english.contains(path, true)) return english.getString(path, "");
        return "";
    }

    /** Gets a localized message list, with English fallback. */
    public List<String> getList(@Nullable Player player, String path) {
        YamlConfiguration selected = select(player);
        if (selected.contains(path, true)) return selected.getStringList(path);
        return english.getStringList(path);
    }
}
