package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.util.Checker;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveProtectionExpiredEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;

@Name("Grave Protection Expired Event")
@Description("Triggered when a grave's protection expires. Provides access to the grave and location.")
@Examples({
        "on grave protection expired:",
        "\tbroadcast \"Grave %event-grave% protection expired at location %event-location%\""
})
public class EvtGraveProtectionExpired extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Protection Expired", EvtGraveProtectionExpired.class, GraveProtectionExpiredEvent.class, "[grave] protect(ing|ed|ion) expir(ing|ed)");

        // Registering event values
        EventValues.registerEventValue(GraveProtectionExpiredEvent.class, Grave.class, new Getter<Grave, GraveProtectionExpiredEvent>() {
            @Override
            public Grave get(GraveProtectionExpiredEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveProtectionExpiredEvent.class, Location.class, new Getter<Location, GraveProtectionExpiredEvent>() {
            @Override
            public Location get(GraveProtectionExpiredEvent e) {
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
        if (e instanceof GraveProtectionExpiredEvent) {
            GraveProtectionExpiredEvent event = (GraveProtectionExpiredEvent) e;
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
        return "Grave protection expired event "  +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}