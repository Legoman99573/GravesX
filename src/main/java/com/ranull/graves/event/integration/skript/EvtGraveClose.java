package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
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
        "\tbroadcast \"Entity %event-entity% closed grave %event-grave% at inventory %event-inventory-view%\""
})
public class EvtGraveClose extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Close", EvtGraveClose.class, GraveCloseEvent.class, "[grave] clos(e|ing)");

        // Registering event values
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