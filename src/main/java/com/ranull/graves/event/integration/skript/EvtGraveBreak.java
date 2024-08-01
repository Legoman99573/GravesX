package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveBreakEvent;
import com.ranull.graves.type.Grave;
import com.ranull.graves.data.BlockData;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Break Event")
@Description("Triggered when a grave block is broken. Provides access to the grave, player, block, and block type.")
@Examples({
        "on grave break:",
        "\tbroadcast \"%event-player% broke grave %event-grave% at block %event-block%\"",
})
public class EvtGraveBreak extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Break", EvtGraveBreak.class, GraveBreakEvent.class, "[grave] br(eak|eaking|oken)");

        // Registering event values
        EventValues.registerEventValue(GraveBreakEvent.class, Player.class, new Getter<Player, GraveBreakEvent>() {
            @Override
            public Player get(GraveBreakEvent e) {
                return e.getPlayer();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Grave.class, new Getter<Grave, GraveBreakEvent>() {
            @Override
            public Grave get(GraveBreakEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveBreakEvent.class, Block.class, new Getter<Block, GraveBreakEvent>() {
            @Override
            public Block get(GraveBreakEvent e) {
                return e.getBlock();
            }
        }, 0);
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<Block> block;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Player>) args[0];
        //grave = (Literal<Grave>) args[0];
        //block = (Literal<Block>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveBreakEvent) {
            GraveBreakEvent event = (GraveBreakEvent) e;
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
            if (block != null && !block.check(event, new Checker<Block>() {
                @Override
                public boolean check(Block b) {
                    return b.equals(event.getBlock());
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
        return "Grave break event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (block != null ? " with block " + block.toString(e, debug) : "");
    }
}