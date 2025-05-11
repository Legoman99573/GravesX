package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveEconomyEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;

import java.util.function.Predicate;

@Name("Grave Economy Event")
@Description("Triggered when an economy transaction is performed for a grave. Allows modification of the economy amount.")
@Examples({
        "on grave economy:",
        "\tbroadcast \"%event-player%'s balance was modified. New balance: %event-economy-amount%\""
})
public class EvtGraveEconomy extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Economy", EvtGraveEconomy.class, GraveEconomyEvent.class, "[grave] eco(nomy|n|system)");

        EventValues.registerEventValue(GraveEconomyEvent.class, Entity.class, GraveEconomyEvent::getPlayer, 0);

        EventValues.registerEventValue(GraveEconomyEvent.class, Grave.class, GraveEconomyEvent::getGrave, 0);

        EventValues.registerEventValue(GraveEconomyEvent.class, Double.class, GraveEconomyEvent::getEconomyAmount, 0);
    }

    private Literal<Entity> player;
    private Literal<Grave> grave;
    private Literal<Double> economyAmount;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[1];
        //economyAmount = (Literal<Double>) args[2];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveEconomyEvent) {
            GraveEconomyEvent event = (GraveEconomyEvent) e;
            if (player != null) {
                player.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity ent) {
                        return ent.equals(event.getPlayer());
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
            if (economyAmount != null) {
                economyAmount.check(event, new Predicate<Double>() {
                    @Override
                    public boolean test(Double amount) {
                        return amount.equals(event.getEconomyAmount());
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave economy event " +
                (player != null ? "with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (economyAmount != null ? " with economy amount " + economyAmount.toString(e, debug) : "");
    }
}
