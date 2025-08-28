package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.event.GraveLootedEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Looted Event")
@Description("Triggered when an inventory associated with a grave is completely looted. Provides access to the grave and inventory view.")
@Examples({
        "on grave looted:",
        "\tbroadcast \"%event-player% completely looted grave %event-grave% at block %event-block% and inventory %event-inventory-view%\""
})
public class EvtGraveLooted extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Looted", EvtGraveLooted.class, GraveLootedEvent.class, "[grave] loo(t|ting|ted)");

        EventValues.registerEventValue(GraveLootedEvent.class, Player.class, GraveLootedEvent::getPlayer, 0);

        EventValues.registerEventValue(GraveLootedEvent.class, Grave.class, GraveLootedEvent::getGrave, 0);

        EventValues.registerEventValue(GraveLootedEvent.class, InventoryView.class, GraveLootedEvent::getInventoryView, 0);
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<InventoryView> inventoryView;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //grave = (Literal<Grave>) args[0];
        //inventoryView = (Literal<InventoryView>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveLootedEvent) {
            GraveLootedEvent event = (GraveLootedEvent) e;
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
            if (inventoryView != null) {
                inventoryView.check(event, new Predicate<InventoryView>() {
                    @Override
                    public boolean test(InventoryView view) {
                        return view.equals(event.getInventoryView());
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave looted event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (inventoryView != null ? " with inventory view " + inventoryView.toString(e, debug) : "");
    }
}