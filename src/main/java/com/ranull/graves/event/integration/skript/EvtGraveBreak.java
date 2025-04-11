package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import com.ranull.graves.event.GraveEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveBreakEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;

import java.util.function.Predicate;

@Name("Grave Break Event")
@Description("Triggered when a grave block is broken. Provides access to the grave, player, block, and block type.")
@Examples({
        "on grave break:",
        "\tbroadcast \"%event-player% broke grave %event-grave% at block %event-block% with experience %event-blockexp%\"",
})
public class EvtGraveBreak extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Break", EvtGraveBreak.class, GraveBreakEvent.class, "[grave] br(eak|eaking|oken)");

        EventValues.registerEventValue(GraveBreakEvent.class, Player.class, GraveBreakEvent::getPlayer, 0);

        EventValues.registerEventValue(GraveBreakEvent.class, Grave.class, GraveBreakEvent::getGrave, 0);

        EventValues.registerEventValue(GraveBreakEvent.class, Block.class, GraveBreakEvent::getBlock, 0);

        EventValues.registerEventValue(GraveBreakEvent.class, Integer.class, GraveBreakEvent::getBlockExp, 0);
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<Block> block;
    private Literal<Integer> blockExp;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Player>) args[0];
        //grave = (Literal<Grave>) args[0];
        //block = (Literal<Block>) args[0];
        //blockExp = (Literal<Integer>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveBreakEvent) {
            GraveBreakEvent event = (GraveBreakEvent) e;

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
            if (block != null) {
                block.check(event, new Predicate<Block>() {
                    @Override
                    public boolean test(Block b) {
                        return b.equals(event.getBlock());
                    }
                });
            }
            if (blockExp != null) {
                blockExp.check(event, new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer be) {
                        return be.equals(event.getBlockExp());
                    }
                });
            }
            return true;
        }
        return false;
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