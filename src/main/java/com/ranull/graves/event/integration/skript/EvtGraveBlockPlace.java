package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveBlockPlaceEvent;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Block Place Event")
@Description("Triggered when a block is placed for a grave. Provides access to the grave, block type, and location.")
@Examples({
        "on grave block place:",
        "\tbroadcast \"Block type %event-block-type% was placed for grave %event-grave% at location %event-location% by entity %event-entity%\""
})
public class EvtGraveBlockPlace extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Block Place", EvtGraveBlockPlace.class, GraveBlockPlaceEvent.class, "[grave] block place[ing]");

        // Registering event values
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Grave.class, new Getter<Grave, GraveBlockPlaceEvent>() {
            @Override
            public Grave get(GraveBlockPlaceEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Location.class, new Getter<Location, GraveBlockPlaceEvent>() {
            @Override
            public Location get(GraveBlockPlaceEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveBlockPlaceEvent.class, BlockData.BlockType.class, new Getter<BlockData.BlockType, GraveBlockPlaceEvent>() {
            @Override
            public BlockData.BlockType get(GraveBlockPlaceEvent e) {
                return e.getBlockType();
            }
        }, 0);
    }

    private Literal<Grave> grave;
    private Literal<Location> location;
    private Literal<BlockData.BlockType> blockType;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        //blockType = (Literal<BlockData.BlockType>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
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
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave block place event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "") +
                (blockType != null ? " with block type " + blockType.toString(e, debug) : "");
    }
}