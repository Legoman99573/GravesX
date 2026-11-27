package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.UUID;

@Name("GravesX Grave Viewer UUID")
@Description("Gets the UUID of the player currently viewing a grave through the GravesX grave management API.")
@Examples("broadcast \"Viewer: %grave viewer uuid of event-grave%\"")
public class ExprGraveViewerUUID extends SimpleExpression<String> {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprGraveViewerUUID.class, String.class)
                        .addPatterns("[the] grave viewer uuid of %grave%", "[the] viewer uuid of %grave%")
                        .build()
        );
    }

    private Expression<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        if (api == null || graveValue == null) {
            return null;
        }

        UUID viewer = api.getGraveViewerUUID(graveValue);
        return viewer != null ? new String[]{viewer.toString()} : null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "grave viewer uuid of " + grave;
    }
}
