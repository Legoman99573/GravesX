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
import dev.cwhead.GravesX.event.GraveVirtualOpenEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Virtual Open Event")
@Description("Triggered when a virtual grave open is about to be processed. Provides access to the entity, player (if applicable), grave, location, and distance.")
@Examples({
        "on grave virtual open:",
        "\tbroadcast \"%event-player% virtually opened grave %event-grave% from %event-number% blocks away at %event-location%\"",
})
public class EvtGraveVirtualOpen extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Virtual Open", EvtGraveVirtualOpen.class, GraveVirtualOpenEvent.class,
                "[grave] virtua(l|ly) ope(n|ning|ned)");

        EventValues.registerEventValue(GraveVirtualOpenEvent.class, Player.class, event -> {
            boolean isEntity = event.hasPlayer();
            return isEntity ? event.getPlayer() : null;
        }, 0);

        EventValues.registerEventValue(GraveVirtualOpenEvent.class, Entity.class, GraveVirtualOpenEvent::getEntity, 0);

        EventValues.registerEventValue(GraveVirtualOpenEvent.class, Grave.class, GraveVirtualOpenEvent::getGrave, 0);

        EventValues.registerEventValue(GraveVirtualOpenEvent.class, Location.class, GraveVirtualOpenEvent::getLocation, 0);

        EventValues.registerEventValue(GraveVirtualOpenEvent.class, Number.class, GraveVirtualOpenEvent::getDistance, 0);
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<Entity> entity;
    private Literal<Location> location;
    private Literal<Number> distance;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Player>) args[0];
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        //distance = (Literal<Number>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveVirtualOpenEvent) {
            GraveVirtualOpenEvent event = (GraveVirtualOpenEvent) e;

            if (player != null) {
                player.check(event, new Predicate<Player>() {
                    @Override
                    public boolean test(Player p) {
                        Entity ent = event.getEntity();
                        return ent instanceof Player && ent.equals(p);
                    }
                });
            }

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

            if (distance != null) {
                distance.check(event, new Predicate<Number>() {
                    @Override
                    public boolean test(Number n) {
                        return n.doubleValue() == event.getDistance();
                    }
                });
            }

            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave virtual open event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " with location " + location.toString(e, debug) : "") +
                (distance != null ? " with distance " + distance.toString(e, debug) : "");
    }
}
