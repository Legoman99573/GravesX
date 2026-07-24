package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveCloseEvent;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

@Name("Grave Close Event")
@Description("Triggered when an inventory associated with a grave is closed. Provides access to the grave and inventory view.")
@Examples({
        "on grave close:",
        "\tbroadcast \"%event-player% closed grave %event-grave% at block %event-block% and inventory %event-inventory-view%\""
})
public class EvtGraveClose extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveClose.class, "Grave Close")
                        .addEvent(GraveCloseEvent.class)
                        .addPatterns("grav(e|es) clos(e|ing|ed)")
                        .addDescription("Triggered when an inventory associated with a grave is closed. Provides access to the grave and inventory view.")
                        .addExamples(
                                "on grave close:",
                                "\tbroadcast \"%event-player% closed grave %event-grave% at block %event-block% and inventory %event-inventory-view%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveCloseEvent.class, Player.class)
                .getter(GraveCloseEvent::getPlayer)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveCloseEvent.class, Grave.class)
                .getter(GraveCloseEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveCloseEvent.class, InventoryView.class)
                .getter(GraveCloseEvent::getInventoryView)
                .patterns("inventory[-]view")
                .build());
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<InventoryView> inventoryView;

    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GraveCloseEvent)) {
            return false;
        }

        GraveCloseEvent event = (GraveCloseEvent) e;

        if (player != null && !player.check(event, playerValue -> playerValue.equals(event.getPlayer()))) {
            return false;
        }

        if (grave != null && !grave.check(event, graveValue -> graveValue.equals(event.getGrave()))) {
            return false;
        }

        if (inventoryView != null && !inventoryView.check(event, inventoryViewValue -> inventoryViewValue.equals(event.getInventoryView()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave close event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (inventoryView != null ? " with inventory view " + inventoryView.toString(e, debug) : "");
    }
}
