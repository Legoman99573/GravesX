package dev.cwhead.GravesX.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Compact utilities for snapshotting player head (skull) blocks across versions.
 */
public final class PlayerHeadUtil {

    /** Delimiter used to append a GXHEAD JSON payload to replace_data. */
    public static final String MARKER = "||GXHEAD||";

    private PlayerHeadUtil() {}

    /**
     * Appends a head snapshot (if block is a head) to {@code existingReplaceData}.
     *
     * @param block the block to read
     * @param existingReplaceData prior replace_data (nullable)
     * @return replace_data with {@link #MARKER}+JSON appended when applicable
     */
    public static String appendFromBlock(Block block, String existingReplaceData) {
        HeadPayload p = extract(block);
        if (p == null) return existingReplaceData;

        String json = toJson(p);
        if (json.isEmpty()) return existingReplaceData;

        if (existingReplaceData == null || existingReplaceData.isEmpty()) {
            return MARKER + json;
        }
        return existingReplaceData + MARKER + json;
    }

    /**
     * Parses the last GXHEAD payload from {@code replaceData}.
     *
     * @param replaceData string containing zero or more payloads
     * @return last {@link HeadPayload}, if present
     */
    public static Optional<HeadPayload> parseFromReplaceData(String replaceData) {
        if (replaceData == null) return Optional.empty();
        int idx = replaceData.lastIndexOf(MARKER);
        if (idx < 0 || idx + MARKER.length() >= replaceData.length()) return Optional.empty();
        String json = replaceData.substring(idx + MARKER.length()).trim();
        try {
            return Optional.of(parseJson(json));
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[GravesX] Failed to parse GXHEAD payload: " + t.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Minimal serialized data for a skull block.
     */
    public static final class HeadPayload {
        int v = 1;
        String m;
        String bd;
        String mount, rf, wf;
        String ou, on;
        String tx, sg;
        String nm;
    }

    /**
     * Extracts snapshot data from a skull block.
     *
     * @param block block to inspect
     * @return payload or {@code null} if not a head
     */
    public static HeadPayload extract(Block block) {
        if (block == null || !isHead(block)) return null;

        HeadPayload p = new HeadPayload();
        p.m = safeMatName(block);
        p.bd = getBlockDataString(block);

        MountFace mf = readMountAndFaces(block);
        p.mount = mf.mount;
        p.rf = mf.rotationFace;
        p.wf = mf.wallFacing;

        try {
            Object state = block.getClass().getMethod("getState").invoke(block);
            if (state instanceof Skull skull) {

                try {
                    Object owning = Skull.class.getMethod("getOwningPlayer").invoke(skull);
                    if (owning != null) {
                        try { p.ou = String.valueOf(owning.getClass().getMethod("getUniqueId").invoke(owning)); } catch (Throwable ignored) {}
                        try { p.on = String.valueOf(owning.getClass().getMethod("getName").invoke(owning)); } catch (Throwable ignored) {}
                    }
                } catch (NoSuchMethodException nsme) {
                    try {
                        Object legacy = Skull.class.getMethod("getOwner").invoke(skull);
                        if (legacy != null) p.on = String.valueOf(legacy);
                    } catch (Throwable ignored) {}
                }

                try {
                    Field profileField = skull.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    Object gp = profileField.get(skull);
                    if (gp != null) {
                        if (p.ou == null) {
                            try {
                                Object id = gp.getClass().getMethod("getId").invoke(gp);
                                if (id != null) p.ou = String.valueOf(id);
                            } catch (Throwable ignored) {}
                        }
                        if (p.on == null) {
                            try {
                                Object nm = gp.getClass().getMethod("getName").invoke(gp);
                                if (nm != null) p.on = String.valueOf(nm);
                            } catch (Throwable ignored) {}
                        }
                        Collection<?> props;
                        try {
                            Object map = gp.getClass().getMethod("properties").invoke(gp);
                            props = getTextures(map);
                        } catch (NoSuchMethodException nsme) {
                            Object map = gp.getClass().getMethod("getProperties").invoke(gp);
                            props = getTextures(map);
                        }
                        if (!props.isEmpty()) {
                            Object prop = props.iterator().next();
                            p.tx = callString(prop, "value", "getValue");
                            p.sg = callString(prop, "signature", "getSignature");
                        }
                    }
                } catch (Throwable ignored) {}

                p.nm = tryReadCustomNameJson(skull);
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[GravesX] Skull read error: " + t.getMessage());
        }

        if (empty(p.bd)) p.bd = null;
        if (empty(p.mount)) p.mount = null;
        if (empty(p.rf)) p.rf = null;
        if (empty(p.wf)) p.wf = null;
        if (empty(p.ou)) p.ou = null;
        if (empty(p.on)) p.on = null;
        if (empty(p.tx)) p.tx = null;
        if (empty(p.sg)) p.sg = null;
        if (empty(p.nm)) p.nm = null;

        return p;
    }

    /**
     * @return true if block is any supported head material.
     */
    private static boolean isHead(Block b) {
        String n = safeMatName(b);
        return "PLAYER_HEAD".equals(n) || "PLAYER_WALL_HEAD".equals(n) || "SKULL".equals(n) || "LEGACY_SKULL".equals(n);
    }

    /**
     * Returns material name or {@code null}.
     */
    private static String safeMatName(Block b) {
        try {
            Material m = b.getType();
            return m.name();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * @return true if {@code s} is null/empty.
     */
    private static boolean empty(String s) { return s == null || s.isEmpty(); }

    /**
     * Gets {@code BlockData#getAsString(true)} on 1.13+, else {@code null}.
     */
    private static String getBlockDataString(Block block) {
        try {
            Object bd = block.getClass().getMethod("getBlockData").invoke(block);
            if (bd != null) {
                Method asString = bd.getClass().getMethod("getAsString", boolean.class);
                return String.valueOf(asString.invoke(bd, true));
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Holder for mount/orientation.
     */
    private static final class MountFace {
        String mount;        // FLOOR/WALL/UNKNOWN
        String rotationFace; // floor rotation
        String wallFacing;   // wall cardinal
    }

    /**
     * Reads mount and facing via 1.13+ APIs or legacy material data.
     */
    private static MountFace readMountAndFaces(Block block) {
        MountFace mf = null;

        try {
            Object bd = block.getClass().getMethod("getBlockData").invoke(block);
            if (bd != null) {
                Class<?> rotatable = tryLoad("org.bukkit.block.data.Rotatable");
                Class<?> directional = tryLoad("org.bukkit.block.data.Directional");
                if (rotatable != null && rotatable.isInstance(bd)) {
                    mf = new MountFace();
                    mf.mount = "FLOOR";
                    Method getRotation = rotatable.getMethod("getRotation");
                    Object face = getRotation.invoke(bd);
                    mf.rotationFace = (face != null ? face.toString() : null);
                }
                if (directional != null && directional.isInstance(bd)) {
                    if (mf == null) mf = new MountFace();
                    mf.mount = "WALL";
                    Method getFacing = directional.getMethod("getFacing");
                    Object face = getFacing.invoke(bd);
                    mf.wallFacing = (face != null ? cardinal(face.toString()) : null);
                }
            }
        } catch (Throwable ignored) {}

        if (mf == null) {
            try {
                Object state = block.getClass().getMethod("getState").invoke(block);
                Method getData = state.getClass().getMethod("getData");
                Object matData = getData.invoke(state);
                Class<?> legacySkull = tryLoad("org.bukkit.material.Skull");
                if (legacySkull != null && legacySkull.isInstance(matData)) {
                    mf = new MountFace();
                    Method getFacing = null, getRotation = null;
                    try { getFacing = legacySkull.getMethod("getFacing"); } catch (Throwable ignored) {}
                    try { getRotation = legacySkull.getMethod("getRotation"); } catch (Throwable ignored) {}
                    if (getFacing != null) {
                        Object bf = getFacing.invoke(matData);
                        if (bf != null) {
                            mf.mount = "WALL";
                            mf.wallFacing = cardinal(bf.toString());
                        }
                    }
                    if (getRotation != null) {
                        Object bf = getRotation.invoke(matData);
                        if (bf != null) {
                            mf.mount = "FLOOR";
                            mf.rotationFace = bf.toString();
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (mf == null) {
            mf = new MountFace();
            mf.mount = "UNKNOWN";
        }
        return mf;
    }

    /**
     * Keeps only cardinal directions.
     */
    private static String cardinal(String face) {
        if (face == null) return null;
        return switch (face.toUpperCase(Locale.ROOT)) {
            case "NORTH" -> "NORTH";
            case "SOUTH" -> "SOUTH";
            case "EAST"  -> "EAST";
            case "WEST"  -> "WEST";
            default -> null;
        };
    }

    /**
     * Loads a class or returns {@code null}.
     */
    private static Class<?> tryLoad(String name) {
        try { return Class.forName(name); } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Returns {@code PropertyMap["textures"]} as a collection when present.
     */
    private static Collection<?> getTextures(Object propertyMap) {
        if (propertyMap == null) return Collections.emptyList();
        try {
            boolean has = (boolean) propertyMap.getClass().getMethod("containsKey", Object.class).invoke(propertyMap, "textures");
            if (has) {
                Object c = propertyMap.getClass().getMethod("get", Object.class).invoke(propertyMap, "textures");
                if (c instanceof Collection) return (Collection<?>) c;
            }
        } catch (Throwable ignored) {}
        try {
            Object c = propertyMap.getClass().getMethod("get", Object.class).invoke(propertyMap, "textures");
            if (c instanceof Collection) return (Collection<?>) c;
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    /**
     * Invokes {@code modern} or fallback {@code legacy} string accessor.
     */
    private static String callString(Object target, String modern, String legacy) {
        if (target == null) return null;
        try { return String.valueOf(target.getClass().getMethod(modern).invoke(target)); }
        catch (Throwable ignored) {}
        try { return String.valueOf(target.getClass().getMethod(legacy).invoke(target)); }
        catch (Throwable ignored) {}
        return null;
    }

    /**
     * Reads custom name as JSON (Adventure if available; otherwise wraps legacy name).
     */
    private static String tryReadCustomNameJson(Skull skull) {
        try {
            Method m = skull.getClass().getMethod("customName");
            Object comp = m.invoke(skull);
            if (comp != null) {
                Class<?> serClazz = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
                Method gson = serClazz.getMethod("gson");
                Object serializer = gson.invoke(null);
                Method serialize = serializer.getClass().getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"));
                Object json = serialize.invoke(serializer, comp);
                return (json != null ? json.toString() : null);
            }
        } catch (Throwable ignored) {}

        try {
            Method m = skull.getClass().getMethod("getCustomName");
            Object s = m.invoke(skull);
            if (s != null) {
                String str = String.valueOf(s);
                if (!str.isEmpty()) return "{\"text\":\"" + str.replace("\"", "\\\"") + "\"}";
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Serializes a payload to compact JSON.
     */
    private static String toJson(HeadPayload p) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        writeInt(sb, "v", p.v, true);
        writeStr(sb, "m", p.m);
        writeStr(sb, "bd", p.bd);
        writeStr(sb, "mount", p.mount);
        writeStr(sb, "rf", p.rf);
        writeStr(sb, "wf", p.wf);
        writeStr(sb, "ou", p.ou);
        writeStr(sb, "on", p.on);
        writeStr(sb, "tx", p.tx);
        writeStr(sb, "sg", p.sg);
        writeStr(sb, "nm", p.nm);
        if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Writes an int field.
     */
    private static void writeInt(StringBuilder sb, String k, int v, boolean always) {
        if (!always) return;
        sb.append('"').append(k).append('"').append(':').append(v).append(',');
    }

    /**
     * Writes a string field if non-null.
     */
    private static void writeStr(StringBuilder sb, String k, String v) {
        if (v == null) return;
        sb.append('"').append(k).append('"').append(':').append('"').append(escape(v)).append('"').append(',');
    }

    /**
     * Escapes backslashes and quotes for JSON.
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Parses the compact JSON back into {@link HeadPayload}.
     */
    private static HeadPayload parseJson(String json) {
        HeadPayload p = new HeadPayload();
        Map<String, String> map = new HashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);

        List<String> pairs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) inStr = !inStr;
            if (!inStr && c == ',') {
                pairs.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) pairs.add(cur.toString());

        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon < 0) continue;
            String key = unquote(pair.substring(0, colon).trim());
            String val = pair.substring(colon + 1).trim();
            map.put(key, unquote(val));
        }

        if (map.containsKey("v")) try { p.v = Integer.parseInt(map.get("v")); } catch (Throwable ignored) {}
        p.m = map.get("m");
        p.bd = map.get("bd");
        p.mount = map.get("mount");
        p.rf = map.get("rf");
        p.wf = map.get("wf");
        p.ou = map.get("ou");
        p.on = map.get("on");
        p.tx = map.get("tx");
        p.sg = map.get("sg");
        p.nm = map.get("nm");
        return p;
    }

    /**
     * Removes surrounding quotes and unescapes JSON string.
     */
    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return s;
    }
}