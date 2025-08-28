package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.event.GraveBlockPlaceEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Block Place Event")
@Description("Triggered when a block is placed for a grave. Provides access to the grave, block type, and location.")
@Examples({
        "on grave block place:",
        "\tbroadcast \"Block type %event-block-type% was placed for grave %event-grave% at location %event-location% by entity %event-entity%\""
})
public class EvtGraveBlockPlace extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Block Place", EvtGraveBlockPlace.class, GraveBlockPlaceEvent.class, "[grave] bloc(k|ks) plac(e|ing|ed)");

        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Grave.class, GraveBlockPlaceEvent::getGrave, 0);

        EventValues.registerEventValue(GraveBlockPlaceEvent.class, Location.class, GraveBlockPlaceEvent::getLocation, 0);

        EventValues.registerEventValue(GraveBlockPlaceEvent.class, BlockData.BlockType.class, GraveBlockPlaceEvent::getBlockType, 0);
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
            if (grave != null) {
                grave.check(event, new Predicate<Grave>() {
                    @Override
                    public boolean test(Grave g) {
                        return g.equals(event.getGrave());
                    }
                });
            }
            if (location != null) {
                location.check(event, new Predicate<Location>() {
                    @Override
                    public boolean test(Location l) {
                        return l.equals(event.getLocation());
                    }
                });
            }
            if (blockType != null) {
                blockType.check(event, new Predicate<BlockData.BlockType>() {
                    @Override
                    public boolean test(BlockData.BlockType b) {
                        return b.equals(event.getBlockType());
                    }
                });
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