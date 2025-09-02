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
import dev.cwhead.GravesX.event.GraveTeleportEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Teleport Event")
@Description("Triggered when an entity teleports to a grave. Provides access to the grave, entity and location.")
@Examples({
        "on grave teleport:",
        "\tbroadcast \"%event-entity% teleported to grave %event-grave% at location %event-location%\""
})
public class EvtGraveTeleport extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Teleport", EvtGraveTeleport.class, GraveTeleportEvent.class, "[grave] telepor(t|ting|ted)");

        EventValues.registerEventValue(GraveTeleportEvent.class, Entity.class, GraveTeleportEvent::getEntity, 0);

        EventValues.registerEventValue(GraveTeleportEvent.class, Grave.class, GraveTeleportEvent::getGrave, 0);

        EventValues.registerEventValue(GraveTeleportEvent.class, Location.class, GraveTeleportEvent::getLocation, 0);
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
        if (e instanceof GraveTeleportEvent) {
            GraveTeleportEvent event = (GraveTeleportEvent) e;
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
        return "Grave teleport event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
