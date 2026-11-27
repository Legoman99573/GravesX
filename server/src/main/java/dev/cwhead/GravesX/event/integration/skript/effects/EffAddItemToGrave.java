package dev.cwhead.GravesX.event.integration.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Add Item To Grave")
@Description("Adds or sets an item in a grave inventory slot through the GravesX grave management API.")
@Examples({
        "add player's tool to grave {_grave} at slot 0",
        "add player's tool to slot 5 of grave {_grave}",
        "add player's tool to next available slot of grave {_grave}",
        "set slot 0 of grave {_grave} to player's tool"
})
public class EffAddItemToGrave extends Effect {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffAddItemToGrave.class)
                        .addPatterns(
                                "add %itemstack% to [the] grave %grave% at slot %number%",
                                "add %itemstack% to slot %number% of [the] grave %grave%",
                                "add %itemstack% to [the] next available slot of [the] grave %grave%",
                                "add %itemstack% to [the] grave %grave% in [the] next available slot",
                                "set slot %number% of [the] grave %grave% to %itemstack%",
                                "set [the] grave %grave% slot %number% to %itemstack%"
                        )
                        .build()
        );
    }

    private Expression<ItemStack> itemStack;
    private Expression<Grave> grave;
    private Expression<Number> slot;
    private boolean nextAvailableSlot;
    private boolean replaceExisting;
    private int matchedPattern;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.matchedPattern = matchedPattern;
        this.nextAvailableSlot = matchedPattern == 2 || matchedPattern == 3;
        this.replaceExisting = matchedPattern == 4 || matchedPattern == 5;

        switch (matchedPattern) {
            case 0 -> {
                this.itemStack = (Expression<ItemStack>) expressions[0];
                this.grave = (Expression<Grave>) expressions[1];
                this.slot = (Expression<Number>) expressions[2];
            }
            case 1 -> {
                this.itemStack = (Expression<ItemStack>) expressions[0];
                this.slot = (Expression<Number>) expressions[1];
                this.grave = (Expression<Grave>) expressions[2];
            }
            case 2, 3 -> {
                this.itemStack = (Expression<ItemStack>) expressions[0];
                this.grave = (Expression<Grave>) expressions[1];
            }
            case 4 -> {
                this.slot = (Expression<Number>) expressions[0];
                this.grave = (Expression<Grave>) expressions[1];
                this.itemStack = (Expression<ItemStack>) expressions[2];
            }
            case 5 -> {
                this.grave = (Expression<Grave>) expressions[0];
                this.slot = (Expression<Number>) expressions[1];
                this.itemStack = (Expression<ItemStack>) expressions[2];
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void execute(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        if (api == null) {
            return;
        }

        Grave graveValue = grave.getSingle(event);
        ItemStack itemValue = itemStack.getSingle(event);
        if (graveValue == null || itemValue == null) {
            return;
        }

        if (nextAvailableSlot) {
            api.addItemToNextAvailableGraveSlot(graveValue, itemValue);
            return;
        }

        Number slotValue = slot != null ? slot.getSingle(event) : null;
        if (slotValue == null) {
            return;
        }

        int slotIndex = slotValue.intValue();
        if (replaceExisting) {
            api.setItemInGraveSlot(graveValue, itemValue, slotIndex);
        } else {
            api.addItemToGraveSlot(graveValue, itemValue, slotIndex);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (matchedPattern) {
            case 0 -> "add " + itemStack + " to grave " + grave + " at slot " + slot;
            case 1 -> "add " + itemStack + " to slot " + slot + " of grave " + grave;
            case 2 -> "add " + itemStack + " to next available slot of grave " + grave;
            case 3 -> "add " + itemStack + " to grave " + grave + " in next available slot";
            case 4 -> "set slot " + slot + " of grave " + grave + " to " + itemStack;
            case 5 -> "set grave " + grave + " slot " + slot + " to " + itemStack;
            default -> "add item to grave";
        };
    }
}
