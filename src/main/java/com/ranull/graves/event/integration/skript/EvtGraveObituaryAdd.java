package com.ranull.graves.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.event.GraveObituaryAddEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Obituary Add Event")
@Description("Triggered when an obituary is to be added to a grave. Provides access to the entity, grave, and location.")
@Examples({
        "on grave obituary add:",
        "\tbroadcast \"Obituary added to %event-player%'s grave %event-grave% at location %event-location%\"",
})
public class EvtGraveObituaryAdd extends SkriptEvent {
    static {
        Skript.registerEvent("Grave Obituary Add", EvtGraveObituaryAdd.class, GraveObituaryAddEvent.class, "[grave] obituar(y|ies) ad(d|ded)");

        EventValues.registerEventValue(GraveObituaryAddEvent.class, Entity.class, GraveObituaryAddEvent::getEntity, 0);

        EventValues.registerEventValue(GraveObituaryAddEvent.class, Grave.class, GraveObituaryAddEvent::getGrave, 0);

        EventValues.registerEventValue(GraveObituaryAddEvent.class, Location.class, GraveObituaryAddEvent::getLocation, 0);
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveObituaryAddEvent) {
            GraveObituaryAddEvent event = (GraveObituaryAddEvent) e;
            if (entity != null) {
                entity.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity ent) {
                        return ent.equals(event.getEntity());
                    }
                });
            }
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
        return "Grave obituary add event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
