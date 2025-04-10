package com.ranull.graves.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import ch.njol.util.Checker;
import com.ranull.graves.event.GravePistonExtendEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Name("Grave Piston Move Event")
@Description("Triggered when a player head is dropped at a grave site. Provides access to the entity, grave, and location.")
@Examples({
        "on grave piston extend:",
        "\tbroadcast \"Grave %event-grave% at location %event-location% extended by %event-piston-block%\"",
})
public class EvtGravePistonExtend extends SkriptEvent {
    static {
        Skript.registerEvent("Grave Piston Move", EvtGravePistonExtend.class, GravePistonExtendEvent.class, "[grave] pisto(n|ns) ex(te|pa)n(d|ded|ding)");

        // Registering event values
        EventValues.registerEventValue(GravePistonExtendEvent.class, Entity.class, new Getter<Entity, GravePistonExtendEvent>() {
            @Override
            public Entity get(GravePistonExtendEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GravePistonExtendEvent.class, Grave.class, new Getter<Grave, GravePistonExtendEvent>() {
            @Override
            public Grave get(GravePistonExtendEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GravePistonExtendEvent.class, Location.class, new Getter<Location, GravePistonExtendEvent>() {
            @Override
            public Location get(GravePistonExtendEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GravePistonExtendEvent.class, Block.class, new Getter<Block, GravePistonExtendEvent>() {
            @Override
            public Block get(GravePistonExtendEvent e) {
                return e.getPistonBlock();
            }
        }, 0);
        EventValues.registerEventValue(GravePistonExtendEvent.class, BlockFace.class, new Getter<BlockFace, GravePistonExtendEvent>() {
            @Override
            public BlockFace get(GravePistonExtendEvent e) {
                return e.getDirection();
            }
        }, 0);
        EventValues.registerEventValue(GravePistonExtendEvent.class, List.class, new Getter<List, GravePistonExtendEvent>() {
            @Override
            public List<Block> get(GravePistonExtendEvent e) {
                return e.getMovedBlocks().stream()
                        .filter(block -> block.getType() != Material.AIR)
                        .collect(Collectors.toList());
            }
        }, 0);
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<Location> location;
    private Literal<Block> pistonBlock;
    private Literal<BlockFace> direction;
    private Literal<List<Block>> movedBlocks;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        //pistonBlock = (Literal<Block>) args[0];
        //direction = (Literal<BlockFace>) args[0];
        //movedBlocks = (Literal<List<Block>>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GravePistonExtendEvent) {
            GravePistonExtendEvent event = (GravePistonExtendEvent) e;
            if (entity != null && !entity.check(event, new Checker<Entity>() {
                @Override
                public boolean check(Entity ent) {
                    return ent.equals(event.getEntity());
                }
            })) {
                return false;
            }
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
            if (pistonBlock != null && !pistonBlock.check(event, new Checker<Block>() {
                @Override
                public boolean check(Block pisbloc) {
                    return pisbloc.equals(event.getPistonBlock());
                }
            })) {
                return false;
            }
            if (direction != null && !direction.check(event, new Checker<BlockFace>() {
                @Override
                public boolean check(BlockFace dir) {
                    return dir.equals(event.getDirection());
                }
            })) {
                return false;
            }
            if (movedBlocks != null && !movedBlocks.check(event, new Checker<List<Block>>() {
                @Override
                public boolean check(List<Block> movbl) {
                    return movbl.equals(event.getMovedBlocks());
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
        return "Grave piston extend " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "") +
                (pistonBlock != null ? " with piston block " + pistonBlock.toString(e, debug) : "") +
                (direction != null ? " with direction " + direction.toString(e, debug) : "") +
                (movedBlocks != null ? " with moved blocks " + movedBlocks.toString(e, debug) : "");
    }
}