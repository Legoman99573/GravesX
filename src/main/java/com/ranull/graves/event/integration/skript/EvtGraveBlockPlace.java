package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveBlockPlaceEvent;
import com.ranull.graves.type.Grave;
import com.ranull.graves.data.BlockData;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Block Place Event")
@Description("Triggered when a block associated with a grave is placed. Provides access to the grave, location, and block type.")
@Examples({
        "on grave block place:",
        "\tbroadcast \"A block of type %event-blocktype% was placed for grave %event-grave% at location %event-location%\""
})
public class EvtGraveBlockPlace extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Block Place", EvtGraveBlockPlace.class, GraveBlockPlaceEvent.class, "[grave] block plac(e|ing)");

        // Registering grave values
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Grave.class, new Getter<Grave, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public Grave get(GraveBlockPlaceEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Location.class, new Getter<Location, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public Location get(GraveBlockPlaceEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, BlockData.BlockType.class, new Getter<BlockData.BlockType, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public BlockData.BlockType get(GraveBlockPlaceEvent e) {
                return e.getBlockType();
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, String.class, new Getter<String, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public String get(GraveBlockPlaceEvent e) {
                return e.getLocation() != null ? e.getLocation().getWorld().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Number.class, new Getter<Number, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public Number get(GraveBlockPlaceEvent e) {
                return e.getLocation() != null ? e.getLocation().getX() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Number.class, new Getter<Number, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public Number get(GraveBlockPlaceEvent e) {
                return e.getLocation() != null ? e.getLocation().getY() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Number.class, new Getter<Number, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public Number get(GraveBlockPlaceEvent e) {
                return e.getLocation() != null ? e.getLocation().getZ() : null;
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, String.class, new Getter<String, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public String get(GraveBlockPlaceEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, String.class, new Getter<String, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public String get(GraveBlockPlaceEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Number.class, new Getter<Number, GraveBlockPlaceEvent>() {
            @Override
            @Nullable
            public Number get(GraveBlockPlaceEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);
    }

    private Literal<Grave> grave;
    private Literal<Location> location;
    private Literal<BlockData.BlockType> blockType;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        grave = (Literal<Grave>) args[0];
        location = (Literal<Location>) args[1];
        blockType = (Literal<BlockData.BlockType>) args[2];
        return true;
    }

    @Override
    public boolean check(@NotNull Event e) {
        if (e instanceof GraveBlockPlaceEvent) {
            GraveBlockPlaceEvent event = (GraveBlockPlaceEvent) e;
            if (grave != null && !grave.check(event, new Checker<Grave>() {
                @Override
                public boolean check(Grave g) {
                    return g.equals(event.getGrave());
                }
            })) {
                return false;
            }
            if (location != null && !location.check(event, new Checker<Location>() {
                @Override
                public boolean check(Location loc) {
                    return loc.equals(event.getLocation());
                }
            })) {
                return false;
            }
            if (blockType != null && !blockType.check(event, new Checker<BlockData.BlockType>() {
                @Override
                public boolean check(BlockData.BlockType type) {
                    return type.equals(event.getBlockType());
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
        return "Grave block place event " + (grave != null ? grave.toString(e, debug) : "") +
                (location != null ? " at " + location.toString(e, debug) : "") +
                (blockType != null ? " with block type " + blockType.toString(e, debug) : "");
    }
}