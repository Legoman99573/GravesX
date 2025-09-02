package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.event.graveevent.GravePlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEventPlayer extends SimpleExpression<Player> {

    @Override
    protected @Nullable Player[] get(Event e) {
        if (e instanceof GravePlayerEvent) {
            Player player = ((GravePlayerEvent) e).getPlayer();
            return new Player[]{player};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event player";
    }
}