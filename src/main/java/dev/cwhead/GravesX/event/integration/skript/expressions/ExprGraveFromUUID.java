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

@Name("GravesX Grave From UUID")
@Description("Gets a grave from a UUID through the GravesX grave management API.")
@Examples("set {_grave} to grave from uuid {_uuid}")
public class ExprGraveFromUUID extends SimpleExpression<Grave> {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprGraveFromUUID.class, Grave.class)
                        .addPatterns("[the] grave from uuid %string%", "[the] grave with uuid %string%")
                        .build()
        );
    }

    private Expression<String> uuid;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.uuid = (Expression<String>) expressions[0];
        return true;
    }

    @Override
    protected @Nullable Grave[] get(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        String uuidValue = uuid.getSingle(event);
        if (api == null || uuidValue == null) {
            return null;
        }

        try {
            Grave grave = api.getGrave(UUID.fromString(uuidValue));
            return grave != null ? new Grave[]{grave} : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Grave> getReturnType() {
        return Grave.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "grave from uuid " + uuid;
    }
}
