package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveAbandonedEvent;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

@Name("Grave Abandoned Event")
@Description("Triggered when a grave is abandoned. Provides access to the grave and location.")
@Examples({
        "on grave abandoned:",
        "\tbroadcast \"Grave %event-grave% is now abandoned at location %event-location%\""
})
public class EvtGraveAbandoned extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveAbandoned.class, "Grave Abandoned")
                        .addEvent(GraveAbandonedEvent.class)
                        .addPatterns("grav(e|es) aband(on|oned|ed|oning)")
                        .addDescription("Triggered when a grave is abandoned. Provides access to the grave and location.")
                        .addExamples(
                                "on grave abandoned:",
                                "\tbroadcast \"Grave %event-grave% is now abandoned at location %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveAbandonedEvent.class, Grave.class)
                .getter(GraveAbandonedEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveAbandonedEvent.class, Location.class)
                .getter(GraveAbandonedEvent::getLocation)
                .patterns("location")
                .build());
    }

    private Literal<Grave> grave;
    private Literal<Location> location;

    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GraveAbandonedEvent event)) {
            return false;
        }

        if (grave != null && !grave.check(event, g -> g.equals(event.getGrave()))) {
            return false;
        }

        if (location != null && !location.check(event, l -> l.equals(event.getLocation()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave abandoned event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}