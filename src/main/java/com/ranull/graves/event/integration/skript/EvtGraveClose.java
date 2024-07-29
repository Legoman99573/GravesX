package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.HumanEntity;
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
@Description("Triggered when an inventory associated with a grave is closed. Provides access to the grave, player, and inventory view.")
@Examples({
        "on grave close:",
        "\tbroadcast \"%player% closed the inventory of grave %event-grave%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\""
})
public class EvtGraveClose extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Close", EvtGraveClose.class, GraveCloseEvent.class, "[grave] close[ing]");

        // Registering player values
        EventValues.registerEventValue(GraveCloseEvent.class, HumanEntity.class, new Getter<HumanEntity, GraveCloseEvent>() {
            @Override
            @Nullable
            public HumanEntity get(GraveCloseEvent e) {
                return e.getPlayer();
            }
        }, 0);
        EventValues.registerEventValue(GraveCloseEvent.class, String.class, new Getter<String, GraveCloseEvent>() {
            @Override
            @Nullable
            public String get(GraveCloseEvent e) {
                return e.getPlayer() != null ? e.getPlayer().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCloseEvent.class, String.class, new Getter<String, GraveCloseEvent>() {
            @Override
            @Nullable
            public String get(GraveCloseEvent e) {
                return e.getPlayer() != null ? e.getPlayer().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveCloseEvent.class, Grave.class, new Getter<Grave, GraveCloseEvent>() {
            @Override
            @Nullable
            public Grave get(GraveCloseEvent e) {
                return e.getGrave();
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveCloseEvent.class, String.class, new Getter<String, GraveCloseEvent>() {
            @Override
            @Nullable
            public String get(GraveCloseEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCloseEvent.class, String.class, new Getter<String, GraveCloseEvent>() {
            @Override
            @Nullable
            public String get(GraveCloseEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCloseEvent.class, Number.class, new Getter<Number, GraveCloseEvent>() {
            @Override
            @Nullable
            public Number get(GraveCloseEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);

        // Registering inventory view value
        EventValues.registerEventValue(GraveCloseEvent.class, InventoryView.class, new Getter<InventoryView, GraveCloseEvent>() {
            @Override
            @Nullable
            public InventoryView get(GraveCloseEvent e) {
                return e.getView();
            }
        }, 0);
    }

    private Literal<Grave> grave;
    private Literal<HumanEntity> player;
    private Literal<InventoryView> inventoryView;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, @NotNull ParseResult parseResult) {
        grave = (Literal<Grave>) args[0];
        player = (Literal<HumanEntity>) args[1];
        inventoryView = (Literal<InventoryView>) args[2];
        return true;
    }

    @Override
    public boolean check(@NotNull Event e) {
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
            if (player != null && !player.check(event, new Checker<HumanEntity>() {
                @Override
                public boolean check(HumanEntity p) {
                    return p.equals(event.getPlayer());
                }
            })) {
                return false;
            }
            if (inventoryView != null && !inventoryView.check(event, new Checker<InventoryView>() {
                @Override
                public boolean check(InventoryView view) {
                    return view.equals(event.getView());
                }
            })) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public @NotNull String toString(@Nullable Event e, boolean debug) {
        return "Grave close event " + (grave != null ? grave.toString(e, debug) : "") +
                (player != null ? " by player " + player.toString(e, debug) : "") +
                (inventoryView != null ? " with inventory view " + inventoryView.toString(e, debug) : "");
    }
}