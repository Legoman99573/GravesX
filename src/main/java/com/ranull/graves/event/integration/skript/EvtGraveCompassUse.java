package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveCompassUseEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Compass Use Event")
@Description("Triggered when a grave compass is fired.")
@Examples({
        "on grave compass use:",
        "\tbroadcast \"%event-player% used grave compass for %event-grave% for grave location %event-location%\"",
})
public class EvtGraveCompassUse extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Compass Use", EvtGraveCompassUse.class, GraveCompassUseEvent.class, "[grave] compas(s|ses) Us(e|ing|ed)");

        // Registering event values
        EventValues.registerEventValue(GraveCompassUseEvent.class, Player.class, new Getter<Player, GraveCompassUseEvent>() {
            @Override
            public Player get(GraveCompassUseEvent e) {
                return e.getPlayer();
            }
        }, 0);
        EventValues.registerEventValue(GraveCompassUseEvent.class, Grave.class, new Getter<Grave, GraveCompassUseEvent>() {
            @Override
            public Grave get(GraveCompassUseEvent e) {
                return e.getGrave();
            }
        }, 0);
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

            // Check for player
            if (player != null && !player.check(event, new Checker<Player>() {
                @Override
                public boolean check(Player p) {
                    return p.equals(event.getPlayer());
                }
            })) {
                return false;
            }

            // Check for grave
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
        return "Grave compass use event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}