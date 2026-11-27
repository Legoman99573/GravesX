package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Grave Amount")
@Description("Gets the amount of graves through the GravesX grave management API.")
@Examples({
        "broadcast \"There are %amount of graves% loaded graves.\"",
        "broadcast \"You have %amount of graves of player% graves.\""
})
public class ExprGraveAmount extends SimpleExpression<Number> {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprGraveAmount.class, Number.class)
                        .addPatterns("[the] amount of graves", "[the] grave amount", "[the] amount of graves of %player%", "[the] grave amount of %player%")
                        .build()
        );
    }

    private Expression<Player> player;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        if (matchedPattern == 2 || matchedPattern == 3) {
            this.player = (Expression<Player>) expressions[0];
        }
        return true;
    }

    @Override
    protected @Nullable Number[] get(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        if (api == null) {
            return null;
        }

        Player playerValue = player != null ? player.getSingle(event) : null;
        return new Number[]{playerValue != null ? api.getGraveAmount(playerValue) : api.getGraveAmount()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return player != null ? "grave amount of " + player : "grave amount";
    }
}
