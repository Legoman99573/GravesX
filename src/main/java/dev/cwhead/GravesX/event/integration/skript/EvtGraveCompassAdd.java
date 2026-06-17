package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveCompassAddEvent;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

@Name("Grave Compass Add Event")
@Description("Triggered when a grave compass is added to a users inventory.")
@Examples({
        "on grave compass add:",
        "\tbroadcast \"Grave compass for %event-grave% for grave location %event-location% added to %event-player%\"",
})
public class EvtGraveCompassAdd extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveCompassAdd.class, "Grave Compass Add")
                        .addEvent(GraveCompassAddEvent.class)
                        .addPatterns("[grave] compas(s|ses) ad(d|ding|ded)")
                        .addDescription("Triggered when a grave compass is added to a users inventory.")
                        .addExamples(
                                "on grave compass add:",
                                "\tbroadcast \"Grave compass for %event-grave% for grave location %event-location% added to %event-player%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveCompassAddEvent.class, Player.class)
                .getter(GraveCompassAddEvent::getPlayer)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveCompassAddEvent.class, Grave.class)
                .getter(GraveCompassAddEvent::getGrave)
                .patterns("grave")
                .build());
    }

    private Literal<Player> player;
    private Literal<Grave> grave;

    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        // player = (Literal<Player>) args[0];
        // grave = (Literal<Grave>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GraveCompassAddEvent event)) {
            return false;
        }

        if (player != null && !player.check(event, p -> p.equals(event.getPlayer()))) {
            return false;
        }

        if (grave != null && !grave.check(event, g -> g.equals(event.getGrave()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave compass add event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}