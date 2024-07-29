package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.entity.EntityData;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveOpenEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Open Event")
@Description("Triggered when an inventory associated with a grave is opened. Provides access to the grave, player, and inventory view.")
@Examples({
        "on grave open:",
        "\tbroadcast \"%player% opened the inventory of grave %event-grave%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\""
})
public class EvtGraveOpen extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Open", EvtGraveOpen.class, GraveOpenEvent.class, "[grave] open[ing]");

        // Registering entity values
        EventValues.registerEventValue(GraveOpenEvent.class, Entity.class, new Getter<Entity, GraveOpenEvent>() {
            @Override
            @Nullable
            public Entity get(GraveOpenEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveOpenEvent.class, String.class, new Getter<String, GraveOpenEvent>() {
            @Override
            @Nullable
            public String get(GraveOpenEvent e) {
                return e.getEntity() != null ? e.getEntity().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveOpenEvent.class, String.class, new Getter<String, GraveOpenEvent>() {
            @Override
            @Nullable
            public String get(GraveOpenEvent e) {
                return e.getEntity() != null ? e.getEntity().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveOpenEvent.class, Grave.class, new Getter<Grave, GraveOpenEvent>() {
            @Override
            @Nullable
            public Grave get(GraveOpenEvent e) {
                return e.getGrave();
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveOpenEvent.class, String.class, new Getter<String, GraveOpenEvent>() {
            @Override
            @Nullable
            public String get(GraveOpenEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveOpenEvent.class, String.class, new Getter<String, GraveOpenEvent>() {
            @Override
            @Nullable
            public String get(GraveOpenEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveOpenEvent.class, Number.class, new Getter<Number, GraveOpenEvent>() {
            @Override
            @Nullable
            public Number get(GraveOpenEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);

        // Registering inventory view value
        EventValues.registerEventValue(GraveOpenEvent.class, InventoryView.class, new Getter<InventoryView, GraveOpenEvent>() {
            @Override
            @Nullable
            public InventoryView get(GraveOpenEvent e) {
                return e.getView();
            }
        }, 0);
    }

    private Literal<EntityData<?>> entities;
    private Literal<Grave> grave;
    private Literal<InventoryView> inventoryView;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, @NotNull ParseResult parseResult) {
        entities = (Literal<EntityData<?>>) args[0];
        grave = (Literal<Grave>) args[1];
        inventoryView = (Literal<InventoryView>) args[2];
        return true;
    }

    @Override
    public boolean check(@NotNull Event e) {
        if (e instanceof GraveOpenEvent) {
            GraveOpenEvent event = (GraveOpenEvent) e;
            if (entities != null && !entities.check(event, new Checker<EntityData<?>>() {
                @Override
                public boolean check(EntityData<?> data) {
                    return data.isInstance(event.getEntity());
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
        return "Grave open event " + (entities != null ? entities.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (inventoryView != null ? " with inventory view " + inventoryView.toString(e, debug) : "");
    }
}