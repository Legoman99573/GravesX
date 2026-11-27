package dev.cwhead.GravesX.integration;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class SkriptTypes {

    private SkriptTypes() {
    }

    /**
     * Registers GravesX-specific Skript types so event values and expressions are first-class values.
     */
    public static void register() {
        registerGraveType();
        registerGraveBlockType();
    }

    private static void registerGraveType() {
        if (Classes.getExactClassInfo(Grave.class) != null) {
            return;
        }

        Classes.registerClass(new ClassInfo<>(Grave.class, "grave")
                .user("graves?")
                .name("Grave")
                .description("A GravesX grave object.")
                .usage("grave")
                .examples("broadcast \"%event-grave%\"")
                .parser(new Parser<>() {
                    @Override
                    public @Nullable Grave parse(String s, ParseContext context) {
                        return null;
                    }

                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }

                    @Override
                    public String toString(Grave grave, int flags) {
                        return String.valueOf(grave);
                    }

                    @Override
                    public String toVariableNameString(Grave grave) {
                        return String.valueOf(grave);
                    }
                }));
    }

    private static void registerGraveBlockType() {
        if (Classes.getExactClassInfo(BlockData.BlockType.class) != null) {
            return;
        }

        Classes.registerClass(new ClassInfo<>(BlockData.BlockType.class, "graveblocktype")
                .user("grave ?block ?types?")
                .name("Grave Block Type")
                .description("A GravesX grave block type.")
                .usage("grave block type")
                .examples("broadcast \"%event-block-type%\"")
                .parser(new Parser<>() {
                    @Override
                    public @Nullable BlockData.BlockType parse(String s, ParseContext context) {
                        String normalized = s.trim()
                                .replace('-', '_')
                                .replace(' ', '_')
                                .toUpperCase(Locale.ROOT);

                        try {
                            return BlockData.BlockType.valueOf(normalized);
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    }

                    @Override
                    public String toString(BlockData.BlockType blockType, int flags) {
                        return blockType.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                    }

                    @Override
                    public String toVariableNameString(BlockData.BlockType blockType) {
                        return blockType.name().toLowerCase(Locale.ROOT);
                    }
                }));
    }
}
