package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.ranull.graves.event.GravePistonExtendEvent;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEventDirection extends SimpleExpression<BlockFace> {
    @Override
    protected @Nullable BlockFace[] get(Event e) {
        if (e instanceof GravePistonExtendEvent) {
            return new BlockFace[]{((GravePistonExtendEvent) e).getDirection()};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends BlockFace> getReturnType() {
        return BlockFace.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event blockface";
    }
}