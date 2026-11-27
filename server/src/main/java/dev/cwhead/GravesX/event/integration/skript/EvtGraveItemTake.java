package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveItemTakeEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.addon.SkriptAddon;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Item Take Event")
@Description("Triggered when an item is clicked or moved in a grave.")
@Examples({
        "on grave item take:",
        "\tbroadcast \"Entity %event-entity% took %event-itemstack% using action %event-inventoryaction% from %event-grave% at location %event-location%\""
})
public class EvtGraveItemTake extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveItemTake.class, "Grave Item Take")
                        .addEvent(GraveItemTakeEvent.class)
                        .addPatterns("grav(e|es) Ite(m|ms) Tak(e|ing)")
                        .addDescription("Triggered when an item is clicked or moved in a grave.")
                        .addExamples(
                                "on grave item take:",
                                "\tbroadcast \"Entity %event-entity% took %event-itemstack% using action %event-inventoryaction% from %event-grave% at location %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveItemTakeEvent.class, Entity.class)
                .getter(GraveItemTakeEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GraveItemTakeEvent.class, Grave.class)
                .getter(GraveItemTakeEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveItemTakeEvent.class, ItemStack.class)
                .getter(GraveItemTakeEvent::getItem)
                .patterns("item", "itemstack")
                .build());

        registry.register(EventValue.builder(GraveItemTakeEvent.class, InventoryAction.class)
                .getter(GraveItemTakeEvent::getAction)
                .patterns("inventory[-]action", "action")
                .build());
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<ItemStack> itemStack;
    private Literal<InventoryAction> inventoryAction;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //itemStack = (Literal<itemStack> args[0];
        //inventoryAction = (Literal<inventoryAction> args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveItemTakeEvent) {
            GraveItemTakeEvent event = (GraveItemTakeEvent) e;
            if (entity != null) {
                entity.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity ent) {
                        return ent.equals(event.getEntity());
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
            if (itemStack != null) {
                itemStack.check(event, new Predicate<ItemStack>() {
                    @Override
                    public boolean test(ItemStack is) {
                        return is.equals(event.getItem());
                    }
                });
            }
            if (inventoryAction != null) {
                inventoryAction.check(event, new Predicate<InventoryAction>() {
                    @Override
                    public boolean test(InventoryAction ia) {
                        return ia.equals(event.getAction());
                    }
                });
            }

            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave item take event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (itemStack != null ? " with item stack " + itemStack.toString(e, debug) : "") +
                (inventoryAction != null ? " with inventory action " + inventoryAction.toString(e, debug) : "");
    }
}
