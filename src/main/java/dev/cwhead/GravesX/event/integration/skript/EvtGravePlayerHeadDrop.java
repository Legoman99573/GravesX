package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.event.GravePlayerHeadDropEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Player Head Drop Event")
@Description("Triggered when a player head is dropped at a grave site. Provides access to the entity, grave, and location.")
@Examples({
        "on grave player head drop:",
        "\tbroadcast \"Dropped Player Head for %event-player%'s grave %event-grave% at location %event-location%\"",
})
public class EvtGravePlayerHeadDrop extends SkriptEvent {
    static {
        Skript.registerEvent("Grave Player Head Drop", EvtGravePlayerHeadDrop.class, GravePlayerHeadDropEvent.class, "[grave] playe(r|rs) hea(d|ds) dro(p|ped|pping)");

        EventValues.registerEventValue(GravePlayerHeadDropEvent.class, Entity.class, GravePlayerHeadDropEvent::getEntity, 0);

        EventValues.registerEventValue(GravePlayerHeadDropEvent.class, Grave.class, GravePlayerHeadDropEvent::getGrave, 0);

        EventValues.registerEventValue(GravePlayerHeadDropEvent.class, Location.class, GravePlayerHeadDropEvent::getLocation, 0);
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
        if (e instanceof GravePlayerHeadDropEvent) {
            GravePlayerHeadDropEvent event = (GravePlayerHeadDropEvent) e;
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
        return "Grave player head drop " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}