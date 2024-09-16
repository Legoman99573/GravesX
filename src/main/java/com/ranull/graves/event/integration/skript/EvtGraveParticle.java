package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import com.ranull.graves.event.GraveParticleEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Particle Event")
@Description("Triggered when a particle is targeted to a grave location.")
@Examples({
        "on grave particle:",
        "\tbroadcast \"%event-player% fired particles to grave location %event-location%\"",
})
public class EvtGraveParticle extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Compass Use", EvtGraveParticle.class, GraveParticleEvent.class, "[grave] particl(e|es)");

        // Registering event values
        EventValues.registerEventValue(GraveParticleEvent.class, Player.class, new Getter<Player, GraveParticleEvent>() {
            @Override
            public Player get(GraveParticleEvent e) {
                return e.getPlayer();
            }
        }, 0);
        EventValues.registerEventValue(GraveParticleEvent.class, Grave.class, new Getter<Grave, GraveParticleEvent>() {
            @Override
            public Grave get(GraveParticleEvent e) {
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
        if (e instanceof GraveParticleEvent) {
            GraveParticleEvent event = (GraveParticleEvent) e;

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
        return "Grave particle event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}