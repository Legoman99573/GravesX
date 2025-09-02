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
import dev.cwhead.GravesX.event.GraveAbandonedEvent;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Abandoned Event")
@Description("Triggered when a grave is abandoned. Provides access to the grave and location.")
@Examples({
        "on grave abandoned:",
        "\tbroadcast \"Grave %event-grave% is now abandoned at location %event-location%\""
})
public class EvtGraveAbandoned extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Abandoned", EvtGraveAbandoned.class, GraveAbandonedEvent.class, "[grave] aband(on|oned|ed|oning)");

        EventValues.registerEventValue(GraveAbandonedEvent.class, Grave.class, GraveAbandonedEvent::getGrave, 0);

        EventValues.registerEventValue(GraveAbandonedEvent.class, Location.class, GraveAbandonedEvent::getLocation, 0);
    }

    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveAbandonedEvent) {
            GraveAbandonedEvent event = (GraveAbandonedEvent) e;
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
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave abandoned event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}