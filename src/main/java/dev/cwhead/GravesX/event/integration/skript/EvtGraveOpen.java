package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveOpenEvent;
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

@Name("Grave Open Event")
@Description("Triggered when an inventory associated with a grave is opened. Provides access to the player, grave, and inventory view.")
@Examples({
        "on grave open:",
        "\tbroadcast \"%event-player% opened grave %event-grave% at location %event-location%\"",
})
public class EvtGraveOpen extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveOpen.class, "Grave Open")
                        .addEvent(GraveOpenEvent.class)
                        .addPatterns("[grave] ope(n|ning|ned)")
                        .addDescription("Triggered when an inventory associated with a grave is opened. Provides access to the player, grave, and inventory view.")
                        .addExamples(
                                "on grave open:",
                                "\tbroadcast \"%event-player% opened grave %event-grave% at location %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveOpenEvent.class, Player.class)
                .getter(GraveOpenEvent::getPlayer)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveOpenEvent.class, Grave.class)
                .getter(GraveOpenEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveOpenEvent.class, InventoryView.class)
                .getter(GraveOpenEvent::getView)
                .patterns("inventory[-]view")
                .build());
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<InventoryView> inventoryView;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //inventoryView = (Literal<InventoryView>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveOpenEvent) {
            GraveOpenEvent event = (GraveOpenEvent) e;
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
                        return view.equals(event.getView());
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave open event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (inventoryView != null ? " with inventory view " + inventoryView.toString(e, debug) : "");
    }
}