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
import dev.cwhead.GravesX.event.GravePreTeleportEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Teleport Event")
@Description("Triggered when an entity teleports to a grave. Provides access to the grave, entity and location.")
@Examples({
        "on grave pre teleport:",
        "\tbroadcast \"%event-entity% is preparing to teleport to grave %event-grave% at location %event-location%\""
})
public class EvtGravePreTeleport extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Pre Teleport", EvtGraveTeleport.class, GravePreTeleportEvent.class, "[grave] pr(e|epare) telepor(t|ting|ted)");

        EventValues.registerEventValue(GravePreTeleportEvent.class, Entity.class, GravePreTeleportEvent::getEntity, 0);

        EventValues.registerEventValue(GravePreTeleportEvent.class, Grave.class, GravePreTeleportEvent::getGrave, 0);

        EventValues.registerEventValue(GravePreTeleportEvent.class, Location.class, GravePreTeleportEvent::getLocation, 0);
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
        if (e instanceof GravePreTeleportEvent) {
            GravePreTeleportEvent event = (GravePreTeleportEvent) e;
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
        return "Grave pre teleport event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}