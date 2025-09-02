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
import dev.cwhead.GravesX.event.GraveCompassAddEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Compass Add Event")
@Description("Triggered when a grave compass is added to a users inventory.")
@Examples({
        "on grave compass add:",
        "\tbroadcast \"Grave compass for %event-grave% for grave location %event-location% added to %event-player%\"",
})
public class EvtGraveCompassAdd extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Compass Add", EvtGraveCompassAdd.class, GraveCompassAddEvent.class, "[grave] compas(s|ses) ad(d|ding|ded)");

        EventValues.registerEventValue(GraveCompassAddEvent.class, Player.class, GraveCompassAddEvent::getPlayer, 0);

        EventValues.registerEventValue(GraveCompassAddEvent.class, Grave.class, GraveCompassAddEvent::getGrave, 0);
    }

    private Literal<Player> player;
    private Literal<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Player>) args[0];
        //grave = (Literal<Grave>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveCompassAddEvent) {
            GraveCompassAddEvent event = (GraveCompassAddEvent) e;
            if (player != null) {
                player.check(event, new Predicate<Player>() {
                    @Override
                    public boolean test(Player p) {
                        return p.equals(event.getPlayer());
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
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave compass add event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}