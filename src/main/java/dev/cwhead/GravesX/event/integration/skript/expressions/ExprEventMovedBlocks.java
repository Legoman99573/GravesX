package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.event.GravePistonExtendEvent;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class ExprEventMovedBlocks extends SimpleExpression<List> {
    @Override
    protected @Nullable List[] get(Event e) {
        if (e instanceof GravePistonExtendEvent) {
            return new List[]{((GravePistonExtendEvent) e).getMovedBlocks().stream()
                    .filter(block -> block.getType() != Material.AIR)
                    .collect(Collectors.toList())};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends List> getReturnType() {
        // Return the raw type of the list
        return List.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event moved blocks";
    }
}