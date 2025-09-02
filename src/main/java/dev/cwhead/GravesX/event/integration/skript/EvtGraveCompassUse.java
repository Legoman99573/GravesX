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
import dev.cwhead.GravesX.event.GraveCompassUseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Compass Use Event")
@Description("Triggered when a grave compass is fired.")
@Examples({
        "on grave compass use:",
        "\tbroadcast \"%event-player% used grave compass for %event-grave% for grave location %event-location%\"",
})
public class EvtGraveCompassUse extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Compass Use", EvtGraveCompassUse.class, GraveCompassUseEvent.class, "[grave] compas(s|ses) Us(e|ing|ed)");

        EventValues.registerEventValue(GraveCompassUseEvent.class, Player.class, GraveCompassUseEvent::getPlayer, 0);

        EventValues.registerEventValue(GraveCompassUseEvent.class, Grave.class, GraveCompassUseEvent::getGrave, 0);
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
        if (e instanceof GraveCompassUseEvent) {
            GraveCompassUseEvent event = (GraveCompassUseEvent) e;
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
        return "Grave compass use event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}