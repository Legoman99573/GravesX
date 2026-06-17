package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveBreakEvent;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

@Name("Grave Break Event")
@Description("Triggered when a grave block is broken. Provides access to the grave, player, block, and block type.")
@Examples({
        "on grave break:",
        "\tbroadcast \"%event-player% broke grave %event-grave% at block %event-block% with experience %event-blockexp%\"",
})
public class EvtGraveBreak extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveBreak.class, "Grave Break")
                        .addEvent(GraveBreakEvent.class)
                        .addPatterns("[grave] br(eak|eaking|oken)")
                        .addDescription("Triggered when a grave block is broken. Provides access to the grave, player, block, and block type.")
                        .addExamples(
                                "on grave break:",
                                "\tbroadcast \"%event-player% broke grave %event-grave% at block %event-block% with experience %event-blockexp%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveBreakEvent.class, Player.class)
                .getter(GraveBreakEvent::getPlayer)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveBreakEvent.class, Grave.class)
                .getter(GraveBreakEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveBreakEvent.class, Block.class)
                .getter(GraveBreakEvent::getBlock)
                .patterns("block")
                .build());

        registry.register(EventValue.builder(GraveBreakEvent.class, Integer.class)
                .getter(GraveBreakEvent::getBlockExp)
                .patterns("block[-]exp", "block[-]experience")
                .build());
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<Block> block;
    private Literal<Integer> blockExp;

    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GraveBreakEvent event)) {
            return false;
        }

        if (player != null && !player.check(event, playerValue -> playerValue.equals(event.getPlayer()))) {
            return false;
        }

        if (grave != null && !grave.check(event, graveValue -> graveValue.equals(event.getGrave()))) {
            return false;
        }

        if (block != null && !block.check(event, blockValue -> blockValue.equals(event.getBlock()))) {
            return false;
        }

        if (blockExp != null && !blockExp.check(event, blockExpValue -> blockExpValue.equals(event.getBlockExp()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave break event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (block != null ? " with block " + block.toString(e, debug) : "") +
                (blockExp != null ? " with block experience " + blockExp.toString(e, debug) : "");
    }
}
