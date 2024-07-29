package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveBreakEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Break Event")
@Description("Triggered when a grave block is broken by a player. Provides access to the grave, player, location, block type, and whether items should drop.")
@Examples({
        "on grave break:",
        "\tbroadcast \"%player% broke grave %event-grave% at location %event-location%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\"",
        "\tbroadcast \"Experience in grave: %event-grave's experience%\"",
        "\tbroadcast \"Block type: %event-blocktype%\""
})
public class EvtGraveBreak extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Break", EvtGraveBreak.class, GraveBreakEvent.class, "[grave] break[ing]");

        // Registering player values
        EventValues.registerEventValue(GraveBreakEvent.class, Player.class, new Getter<Player, GraveBreakEvent>() {
            @Override
            @Nullable
            public Player get(GraveBreakEvent e) {
                return e.getPlayer();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, String.class, new Getter<String, GraveBreakEvent>() {
            @Override
            @Nullable
            public String get(GraveBreakEvent e) {
                return e.getPlayer() != null ? e.getPlayer().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, String.class, new Getter<String, GraveBreakEvent>() {
            @Override
            @Nullable
            public String get(GraveBreakEvent e) {
                return e.getPlayer() != null ? e.getPlayer().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveBreakEvent.class, Grave.class, new Getter<Grave, GraveBreakEvent>() {
            @Override
            @Nullable
            public Grave get(GraveBreakEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Location.class, new Getter<Location, GraveBreakEvent>() {
            @Override
            @Nullable
            public Location get(GraveBreakEvent e) {
                return e.getBlock().getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Number.class, new Getter<Number, GraveBreakEvent>() {
            @Override
            @Nullable
            public Number get(GraveBreakEvent e) {
                return e.getGrave().getExperience();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, String.class, new Getter<String, GraveBreakEvent>() {
            @Override
            @Nullable
            public String get(GraveBreakEvent e) {
                return e.getBlock().getWorld().getName();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Number.class, new Getter<Number, GraveBreakEvent>() {
            @Override
            @Nullable
            public Number get(GraveBreakEvent e) {
                return e.getBlock().getLocation().getX();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Number.class, new Getter<Number, GraveBreakEvent>() {
            @Override
            @Nullable
            public Number get(GraveBreakEvent e) {
                return e.getBlock().getLocation().getY();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Number.class, new Getter<Number, GraveBreakEvent>() {
            @Override
            @Nullable
            public Number get(GraveBreakEvent e) {
                return e.getBlock().getLocation().getZ();
            }
        }, 0);

        // Registering block type value
        EventValues.registerEventValue(GraveBreakEvent.class, Material.class, new Getter<Material, GraveBreakEvent>() {
            @Override
            @Nullable
            public Material get(GraveBreakEvent e) {
                return e.getBlock().getType();
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveBreakEvent.class, String.class, new Getter<String, GraveBreakEvent>() {
            @Override
            @Nullable
            public String get(GraveBreakEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, String.class, new Getter<String, GraveBreakEvent>() {
            @Override
            @Nullable
            public String get(GraveBreakEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Number.class, new Getter<Number, GraveBreakEvent>() {
            @Override
            @Nullable
            public Number get(GraveBreakEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);
    }

    private Literal<Grave> grave;
    private Literal<Player> player;
    private Literal<Location> location;
    private Literal<Material> blockType;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, @NotNull ParseResult parseResult) {
        grave = (Literal<Grave>) args[0];
        player = (Literal<Player>) args[1];
        location = (Literal<Location>) args[2];
        blockType = (Literal<Material>) args[3];
        return true;
    }

    @Override
    public boolean check(@NotNull Event e) {
        if (e instanceof GraveBreakEvent) {
            GraveBreakEvent event = (GraveBreakEvent) e;
            if (grave != null && !grave.check(event, new Checker<Grave>() {
                @Override
                public boolean check(Grave g) {
                    return g.equals(event.getGrave());
                }
            })) {
                return false;
            }
            if (player != null && !player.check(event, new Checker<Player>() {
                @Override
                public boolean check(Player p) {
                    return p.equals(event.getPlayer());
                }
            })) {
                return false;
            }
            if (location != null && !location.check(event, new Checker<Location>() {
                @Override
                public boolean check(Location loc) {
                    return loc.equals(event.getBlock().getLocation());
                }
            })) {
                return false;
            }
            if (blockType != null && !blockType.check(event, new Checker<Material>() {
                @Override
                public boolean check(Material type) {
                    return type.equals(event.getBlock().getType());
                }
            })) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public @NotNull String toString(@Nullable Event e, boolean debug) {
        return "Grave break event " + (grave != null ? grave.toString(e, debug) : "") +
                (player != null ? " by player " + player.toString(e, debug) : "") +
                (location != null ? " at " + location.toString(e, debug) : "") +
                (blockType != null ? " with block type " + blockType.toString(e, debug) : "");
    }
}