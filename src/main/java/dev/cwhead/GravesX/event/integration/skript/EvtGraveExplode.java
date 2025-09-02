package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveExplodeEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Explode Event")
@Description("Triggered when a grave explodes. Provides access to the entity, grave, and location.")
@Examples({
        "on grave explode:",
        "\tbroadcast \"Entity %event-entity% caused grave %event-grave% to explode at location %event-location%\""
})
public class EvtGraveExplode extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Explode", EvtGraveExplode.class, GraveExplodeEvent.class, "[grave] explod(e|ing|ed)");

        EventValues.registerEventValue(GraveExplodeEvent.class, Entity.class, GraveExplodeEvent::getEntity, 0);

        EventValues.registerEventValue(GraveExplodeEvent.class, Grave.class, GraveExplodeEvent::getGrave, 0);

        EventValues.registerEventValue(GraveExplodeEvent.class, Location.class, GraveExplodeEvent::getLocation, 0);
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
        if (e instanceof GraveExplodeEvent) {
            GraveExplodeEvent event = (GraveExplodeEvent) e;
            if (entity != null) {
               entity.check(event, new Predicate<Entity>() {
                   @Override
                   public boolean test(Entity ent) {
                       return ent.equals(event.getEntity());
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
        return "Grave explode event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
