package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.entity.EntityData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveExplodeEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Explode Event")
@Description("Triggered when a grave explodes. Provides access to the entity, grave, and location of the explosion.")
@Examples({
        "on grave explode:",
        "\tbroadcast \"%entity% caused grave %event-grave% to explode at location %event-location%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\"",
        "\tbroadcast \"Experience in grave: %event-grave's experience%\""
})
public class EvtGraveExplode extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Explode", EvtGraveExplode.class, GraveExplodeEvent.class, "[grave] explode[ing]");

        // Registering entity values
        EventValues.registerEventValue(GraveExplodeEvent.class, Entity.class, new Getter<Entity, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Entity get(GraveExplodeEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, String.class, new Getter<String, GraveExplodeEvent>() {
            @Override
            @Nullable
            public String get(GraveExplodeEvent e) {
                return e.getEntity() != null ? e.getEntity().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, String.class, new Getter<String, GraveExplodeEvent>() {
            @Override
            @Nullable
            public String get(GraveExplodeEvent e) {
                return e.getEntity() != null ? e.getEntity().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveExplodeEvent.class, Grave.class, new Getter<Grave, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Grave get(GraveExplodeEvent e) {
                return e.getGrave();
            }
        }, 0);

        // Registering location values
        EventValues.registerEventValue(GraveExplodeEvent.class, Location.class, new Getter<Location, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Location get(GraveExplodeEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, String.class, new Getter<String, GraveExplodeEvent>() {
            @Override
            @Nullable
            public String get(GraveExplodeEvent e) {
                return e.getLocation() != null ? e.getLocation().getWorld().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, Number.class, new Getter<Number, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Number get(GraveExplodeEvent e) {
                return e.getLocation() != null ? e.getLocation().getX() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, Number.class, new Getter<Number, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Number get(GraveExplodeEvent e) {
                return e.getLocation() != null ? e.getLocation().getY() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, Number.class, new Getter<Number, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Number get(GraveExplodeEvent e) {
                return e.getLocation() != null ? e.getLocation().getZ() : null;
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveExplodeEvent.class, String.class, new Getter<String, GraveExplodeEvent>() {
            @Override
            @Nullable
            public String get(GraveExplodeEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, String.class, new Getter<String, GraveExplodeEvent>() {
            @Override
            @Nullable
            public String get(GraveExplodeEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveExplodeEvent.class, Number.class, new Getter<Number, GraveExplodeEvent>() {
            @Override
            @Nullable
            public Number get(GraveExplodeEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);
    }

    private Literal<EntityData<?>> entities;
    private Literal<Location> location;
    private Literal<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        entities = (Literal<EntityData<?>>) args[0];
        location = (Literal<Location>) args[1];
        grave = (Literal<Grave>) args[2];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveExplodeEvent) {
            GraveExplodeEvent event = (GraveExplodeEvent) e;
            if (entities != null && !entities.check(event, new Checker<EntityData<?>>() {
                @Override
                public boolean check(EntityData<?> data) {
                    return data.isInstance(event.getEntity());
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
            if (grave != null && !grave.check(event, new Checker<Grave>() {
                @Override
                public boolean check(Grave g) {
                    return g.equals(event.getGrave());
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
        return "Grave explode event " + (entities != null ? entities.toString(e, debug) : "") +
                (location != null ? " at " + location.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}