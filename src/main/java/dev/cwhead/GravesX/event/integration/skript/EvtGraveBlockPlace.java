package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveBlockPlaceEvent;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

@Name("Grave Block Place Event")
@Description("Triggered when a block is placed for a grave. Provides access to the grave, block type, and location.")
@Examples({
        "on grave block place:",
        "\tbroadcast \"Block type %event-block-type% was placed for grave %event-grave% at location %event-location% by entity %event-entity%\""
})
public class EvtGraveBlockPlace extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveBlockPlace.class, "Grave Block Place")
                        .addEvent(GraveBlockPlaceEvent.class)
                        .addPatterns("[grave] bloc(k|ks) plac(e|ing|ed)")
                        .addDescription("Triggered when a block is placed for a grave. Provides access to the grave, block type, and location.")
                        .addExamples(
                                "on grave block place:",
                                "\tbroadcast \"Block type %event-block-type% was placed for grave %event-grave% at location %event-location% by entity %event-entity%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveBlockPlaceEvent.class, Grave.class)
                .getter(GraveBlockPlaceEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveBlockPlaceEvent.class, Location.class)
                .getter(GraveBlockPlaceEvent::getLocation)
                .patterns("location")
                .build());

        registry.register(EventValue.builder(GraveBlockPlaceEvent.class, BlockData.BlockType.class)
                .getter(GraveBlockPlaceEvent::getBlockType)
                .patterns("block[-]type")
                .build());
    }

    private Literal<Grave> grave;
    private Literal<Location> location;
    private Literal<BlockData.BlockType> blockType;

    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GraveBlockPlaceEvent event)) {
            return false;
        }

        if (grave != null && !grave.check(event, graveValue -> graveValue.equals(event.getGrave()))) {
            return false;
        }

        if (location != null && !location.check(event, locationValue -> locationValue.equals(event.getLocation()))) {
            return false;
        }

        if (blockType != null && !blockType.check(event, blockTypeValue -> blockTypeValue.equals(event.getBlockType()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave block place event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "") +
                (blockType != null ? " with block type " + blockType.toString(e, debug) : "");
    }
}
