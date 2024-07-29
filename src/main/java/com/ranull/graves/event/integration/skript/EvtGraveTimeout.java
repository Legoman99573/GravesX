package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.event.Event;
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
        "\tbroadcast \"Grave %event-grave% at location %event-location% has timed out\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\""
})
public class EvtGraveTimeout extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Timeout", EvtGraveTimeout.class, GraveTimeoutEvent.class, "[grave] timeout[ing]");

        // Registering grave values
        EventValues.registerEventValue(GraveTimeoutEvent.class, Grave.class, new Getter<Grave, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public Grave get(GraveTimeoutEvent e) {
                return e.getGrave();
            }
        }, 0);

        // Registering location values
        EventValues.registerEventValue(GraveTimeoutEvent.class, Location.class, new Getter<Location, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public Location get(GraveTimeoutEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, String.class, new Getter<String, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public String get(GraveTimeoutEvent e) {
                return e.getLocation() != null ? e.getLocation().getWorld().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, Number.class, new Getter<Number, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public Number get(GraveTimeoutEvent e) {
                return e.getLocation() != null ? e.getLocation().getX() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, Number.class, new Getter<Number, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public Number get(GraveTimeoutEvent e) {
                return e.getLocation() != null ? e.getLocation().getY() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, Number.class, new Getter<Number, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public Number get(GraveTimeoutEvent e) {
                return e.getLocation() != null ? e.getLocation().getZ() : null;
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveTimeoutEvent.class, String.class, new Getter<String, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public String get(GraveTimeoutEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, String.class, new Getter<String, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public String get(GraveTimeoutEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveTimeoutEvent.class, Number.class, new Getter<Number, GraveTimeoutEvent>() {
            @Override
            @Nullable
            public Number get(GraveTimeoutEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);
    }

    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        grave = (Literal<Grave>) args[0];
        location = (Literal<Location>) args[1];
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
        return "Grave timeout event " + (grave != null ? grave.toString(e, debug) : "") +
                (location != null ? " at " + location.toString(e, debug) : "");
    }
}