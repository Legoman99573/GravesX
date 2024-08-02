package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveTimeoutEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Timeout Event")
@Description("Triggered when a grave times out. Provides access to the grave and location.")
@Examples({
        "on grave timeout:",
        "\tbroadcast \"Grave %event-grave% timed out at location %event-location%\""
})
public class EvtGraveTimeout extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Timeout", EvtGraveTimeout.class, GraveTimeoutEvent.class, "[grave] tim(e|ed)(| |-)out");

        // Registering event values
        EventValues.registerEventValue(GraveTimeoutEvent.class, Grave.class, new Getter<Grave, GraveTimeoutEvent>() {
            @Override
            public Grave get(GraveTimeoutEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, Location.class, new Getter<Location, GraveTimeoutEvent>() {
            @Override
            public Location get(GraveTimeoutEvent e) {
                return e.getLocation();
            }
        }, 0);
    }

    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveTimeoutEvent) {
            GraveTimeoutEvent event = (GraveTimeoutEvent) e;
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
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave timeout event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}