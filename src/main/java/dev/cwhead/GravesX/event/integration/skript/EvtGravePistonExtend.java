package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GravePistonExtendEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

@Name("Grave Piston Move Event")
@Description("Triggered when a player head is dropped at a grave site. Provides access to the grave and location.")
@Examples({
        "on grave piston extend:",
        "\tbroadcast \"Grave %event-grave% at location %event-location% extended by %event-piston-block%\"",
})
public class EvtGravePistonExtend extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Piston Move", EvtGravePistonExtend.class, GravePistonExtendEvent.class, "[grave] pisto(n|ns) ex(te|pa)n(d|ded|ding)");

        EventValues.registerEventValue(GravePistonExtendEvent.class, Grave.class, GravePistonExtendEvent::getGrave, 0);

        EventValues.registerEventValue(GravePistonExtendEvent.class, Location.class, GravePistonExtendEvent::getLocation, 0);

        EventValues.registerEventValue(GravePistonExtendEvent.class, Block.class, GravePistonExtendEvent::getPistonBlock, 0);

        EventValues.registerEventValue(GravePistonExtendEvent.class, BlockFace.class, GravePistonExtendEvent::getDirection, 0);

        EventValues.registerEventValue(GravePistonExtendEvent.class, List.class, GravePistonExtendEvent::getMovedBlocks, 0);
    }

    private Literal<Grave> grave;
    private Literal<Location> location;
    private Literal<Block> pistonBlock;
    private Literal<BlockFace> direction;
    private Literal<List<Block>> movedBlocks;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull[] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        //pistonBlock = (Literal<Block>) args[0];
        //direction = (Literal<BlockFace>) args[0];
        //movedBlocks = (Literal<List<Block>>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GravePistonExtendEvent)) return false;
        GravePistonExtendEvent event = (GravePistonExtendEvent) e;

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
        if (pistonBlock != null) {
            pistonBlock.check(event, new Predicate<Block>() {
                @Override
                public boolean test(Block pb) {
                    return pb.equals(event.getPistonBlock());
                }
            });
        }
        if (direction != null) {
            direction.check(event, new Predicate<BlockFace>() {
                @Override
                public boolean test(BlockFace d) {
                    return d.equals(event.getDirection());
                }
            });
        }
        if (movedBlocks != null) {
            movedBlocks.check(event, new Predicate<List<Block>>() {
                @Override
                public boolean test(List<Block> mb) {
                    return mb.equals(event.getMovedBlocks());
                }
            });
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave piston extend" +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "") +
                (pistonBlock != null ? " with piston block " + pistonBlock.toString(e, debug) : "") +
                (direction != null ? " with direction " + direction.toString(e, debug) : "") +
                (movedBlocks != null ? " with moved blocks " + movedBlocks.toString(e, debug) : "");
    }
}