package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveLootedEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.addon.SkriptAddon;
import dev.cwhead.GravesX.integration.SkriptImpl;
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
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveLooted.class, "Grave Looted")
                        .addEvent(GraveLootedEvent.class)
                        .addPatterns("grav(e|es) loo(t|ting|ted)")
                        .addDescription("Triggered when an inventory associated with a grave is completely looted. Provides access to the grave and inventory view.")
                        .addExamples(
                                "on grave looted:",
                                "\tbroadcast \"%event-player% completely looted grave %event-grave% at block %event-block% and inventory %event-inventory-view%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveLootedEvent.class, Player.class)
                .getter(GraveLootedEvent::getPlayer)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveLootedEvent.class, Grave.class)
                .getter(GraveLootedEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveLootedEvent.class, InventoryView.class)
                .getter(GraveLootedEvent::getInventoryView)
                .patterns("inventory[-]view")
                .build());
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