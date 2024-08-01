package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveCloseEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Close Event")
@Description("Triggered when an inventory associated with a grave is closed. Provides access to the grave and inventory view.")
@Examples({
        "on grave close:",
        "\tbroadcast \"%event-player% closed grave %event-grave% at block %event-block% and inventory %event-inventory-view%\""
})
public class EvtGraveClose extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Close", EvtGraveClose.class, GraveCloseEvent.class, "[grave] clos(e|ing|ed)");

        // Registering event values
        EventValues.registerEventValue(GraveCloseEvent.class, Player.class, new Getter<Player, GraveCloseEvent>() {
            @Override
            public Player get(GraveCloseEvent e) {
                return e.getPlayer();
            }
        }, 0);
        EventValues.registerEventValue(GraveCloseEvent.class, Grave.class, new Getter<Grave, GraveCloseEvent>() {
            @Override
            public Grave get(GraveCloseEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveCloseEvent.class, InventoryView.class, new Getter<InventoryView, GraveCloseEvent>() {
            @Override
            public InventoryView get(GraveCloseEvent e) {
                return e.getInventoryView();
            }
        }, 0);
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
        if (e instanceof GraveCloseEvent) {
            GraveCloseEvent event = (GraveCloseEvent) e;
            if (player != null && !player.check(event, new Checker<Player>() {
                @Override
                public boolean check(Player p) {
                    return p.equals(event.getPlayer());
                }
            })) {
                return false;
            }
            if (grave != null && !grave.check(event, new Checker<Grave>() {
                @Override
                public boolean check(Grave g) {
                    return g.equals(event.getGrave());
                }
            })) {
                return false;
            }
            if (inventoryView != null && !inventoryView.check(event, new Checker<InventoryView>() {
                @Override
                public boolean check(InventoryView view) {
                    return view.equals(event.getInventoryView());
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
        return "Grave close event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (inventoryView != null ? " with inventory view " + inventoryView.toString(e, debug) : "");
    }
}