package com.ranull.graves.event.integration.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.ranull.graves.event.GraveBreakEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEventBlockExp extends SimpleExpression<Integer> {

    // Getting the block experience
    @Override
    protected @Nullable Integer[] get(Event e) {
        if (e instanceof GraveBreakEvent) {
            return new Integer[]{((GraveBreakEvent) e).getBlockExp()};
        }
        return null;
    }

    // Setting the block experience
    public void set(Event e, @Nullable Integer exp) {
        if (e instanceof GraveBreakEvent && exp != null) {
            ((GraveBreakEvent) e).setBlockExp(exp);
        }
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event block experience";
    }
}