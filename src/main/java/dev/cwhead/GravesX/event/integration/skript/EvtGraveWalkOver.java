package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.event.GraveWalkOverEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

@Name("Grave Walk Over Event")
@Description("Triggered when an entity walks over a grave. Provides access to the entity, grave, and location.")
@Examples({
        "on grave walk over:",
        "\tbroadcast \"Entity %event-entity% walked over and looted grave %event-grave% at location %event-location%\"",
})
public class EvtGraveWalkOver extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Walk Over", EvtGraveWalkOver.class, GraveWalkOverEvent.class, "[grave] wal(k|ked|king) over");

        EventValues.registerEventValue(GraveWalkOverEvent.class, Entity.class, GraveWalkOverEvent::getEntity, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, String.class, GraveWalkOverEvent::getEntityName, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, UUID.class, GraveWalkOverEvent::getEntityUniqueId, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, Entity.class, GraveWalkOverEvent::getEntity, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, Location.class, GraveWalkOverEvent::getLocation, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, UUID.class, GraveWalkOverEvent::getGraveOwnerUniqueId, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, String.class, GraveWalkOverEvent::getGraveOwnerDisplayName, 0);

        EventValues.registerEventValue(GraveWalkOverEvent.class, Number.class, GraveWalkOverEvent::getGraveExperience, 0);
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveWalkOverEvent) {
            GraveWalkOverEvent event = (GraveWalkOverEvent) e;
            if (entity != null) {
                entity.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity e) {
                        return e.equals(event.getEntity());
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
        return "Grave walk over event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}