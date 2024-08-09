package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ExperienceUtil;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Integration class for PlaceholderAPI to provide custom placeholders for the Graves plugin.
 * Extends PlaceholderExpansion and implements Relational for placeholder handling.
 */
public final class PlaceholderAPI extends PlaceholderExpansion implements Relational {
    private final Graves plugin;
    private final Map<String, Function<String, String>> placeholderHandlers;

    /**
     * Constructs a new PlaceholderAPI instance with the specified Graves plugin.
     *
     * @param plugin The main Graves plugin instance.
     */
    public PlaceholderAPI(Graves plugin) {
        this.plugin = plugin;
        this.placeholderHandlers = initializePlaceholderHandlers();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @NotNull
    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "graves";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Handles placeholder requests for the Graves plugin.
     *
     * @param player     The player requesting the placeholder (may be null).
     * @param identifier The identifier of the placeholder.
     * @return The value of the placeholder, or an empty string if not applicable.
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        identifier = identifier.toLowerCase();

        // Handle player-specific placeholder
        if (player != null && identifier.equals("count")) {
            return String.valueOf(plugin.getGraveManager().getGraveCount(player));
        }

        // Handle general placeholders
        Function<String, String> handler = placeholderHandlers.get(identifier);
        if (handler != null) {
            return handler.apply(identifier);
        }

        // Handle dynamic placeholders
        for (Map.Entry<String, Function<String, String>> entry : placeholderHandlers.entrySet()) {
            if (identifier.startsWith(entry.getKey())) {
                return entry.getValue().apply(identifier);
            }
        }

        return null;
    }

    /**
     * Handles relational placeholder requests (not yet implemented).
     *
     * @param playerOne  The first player.
     * @param playerTwo  The second player.
     * @param identifier The identifier of the placeholder.
     * @return The value of the placeholder, or null if not implemented.
     */
    @Override
    public String onPlaceholderRequest(Player playerOne, Player playerTwo, String identifier) {
        return onPlaceholderRequest(playerOne, identifier); // TODO
    }

    /**
     * Initializes the placeholder handlers.
     *
     * @return A map of placeholder handlers.
     */
    private Map<String, Function<String, String>> initializePlaceholderHandlers() {
        Map<String, Function<String, String>> handlers = new HashMap<>();

        handlers.put("author", id -> getAuthor());
        handlers.put("version", id -> getVersion());
        handlers.put("count_total", id -> String.valueOf(plugin.getCacheManager().getGraveMap().size()));

        handlers.put("owner_name_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("owner_name_", ""));
            return getOwnerDetail(uuid, Grave::getOwnerName);
        });
        handlers.put("owner_type_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("owner_type_", ""));
            return getOwnerDetail(uuid, grave -> {
                EntityType type = grave.getOwnerType();
                return type != null ? type.name() : "";
            });
        });
        handlers.put("owner_uuid_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("owner_uuid_", ""));
            return getOwnerDetail(uuid, grave -> {
                UUID ownerUUID = grave.getOwnerUUID();
                return ownerUUID != null ? ownerUUID.toString() : "";
            });
        });
        handlers.put("killer_name_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("killer_name_", ""));
            return getOwnerDetail(uuid, Grave::getKillerName);
        });
        handlers.put("killer_type_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("killer_type_", ""));
            return getOwnerDetail(uuid, grave -> {
                EntityType type = grave.getKillerType();
                return type != null ? type.name() : "";
            });
        });
        handlers.put("killer_uuid_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("killer_uuid_", ""));
            return getOwnerDetail(uuid, grave -> {
                UUID killerUUID = grave.getKillerUUID();
                return killerUUID != null ? killerUUID.toString() : "";
            });
        });
        handlers.put("item_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("item_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(grave.getItemAmount()));
        });
        handlers.put("experience_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("experience_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(grave.getExperience()));
        });
        handlers.put("level_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("level_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(ExperienceUtil.getLevelFromExperience(grave.getExperience())));
        });
        handlers.put("time_creation_formatted", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_creation_formatted", ""));
            return getOwnerDetail(uuid, grave -> StringUtil.getDateString(grave, grave.getTimeCreation(), plugin));
        });
        handlers.put("time_creation_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_creation_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(grave.getTimeCreation() / 1000));
        });
        handlers.put("time_alive_remaining_formatted_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_alive_remaining_formatted_", ""));
            return getOwnerDetail(uuid, grave -> StringUtil.getTimeString(grave, grave.getTimeAliveRemaining(), plugin));
        });
        handlers.put("time_alive_remaining_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_alive_remaining_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(grave.getTimeAliveRemaining() / 1000));
        });
        handlers.put("time_protection_remaining_formatted_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_protection_remaining_formatted_", ""));
            return getOwnerDetail(uuid, grave -> StringUtil.getTimeString(grave, grave.getTimeProtectionRemaining(), plugin));
        });
        handlers.put("time_protection_remaining_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_protection_remaining_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(grave.getTimeProtectionRemaining() / 1000));
        });
        handlers.put("time_lived_formatted_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_lived_formatted_", ""));
            return getOwnerDetail(uuid, grave -> StringUtil.getTimeString(grave, grave.getLivedTime(), plugin));
        });
        handlers.put("time_lived_", id -> {
            UUID uuid = UUIDUtil.getUUID(id.replace("time_lived_", ""));
            return getOwnerDetail(uuid, grave -> String.valueOf(grave.getLivedTime() / 1000));
        });

        return handlers;
    }

    private String getOwnerDetail(UUID uuid, Function<Grave, String> function) {
        if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
            Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
            return function.apply(grave);
        }
        return "";
    }
}