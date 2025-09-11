package dev.cwhead.GravesX.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class PlayerHeadUtil {

    /** Marker separating your existing replace_data and our head snapshot payload. */
    public static final String MARKER = "||GXHEAD||";

    /** Append head snapshot JSON for the given skull block to the existing replace_data string. */
    public static String appendFromBlock(Block block, String existingReplaceData) {
        HeadPayload p = extract(block);
        if (p == null) return existingReplaceData; // not a head
        String json = toJson(p);
        if (json.isEmpty()) return existingReplaceData;

        if (existingReplaceData == null || existingReplaceData.isEmpty()) {
            return MARKER + json;
        }
        return existingReplaceData + MARKER + json;
    }

    /** Try to parse the last GXHEAD payload from replace_data (optional helper). */
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

    // =====================================================================================
    // Extraction (1.7 -> 1.21.x)
    // =====================================================================================

    /** Data we’ll serialize into the GXHEAD JSON payload. */
    public static final class HeadPayload {
        // schema
        int v = 1;
        // block identity
        String m;   // material name (PLAYER_HEAD / PLAYER_WALL_HEAD / SKULL / LEGACY_SKULL)
        String bd;  // blockdata as string (1.13+), e.g. "minecraft:player_head[rotation=0]"
        // placement
        String mount; // FLOOR | WALL | UNKNOWN
        String rf;    // rotation face for floor (BlockFace name), optional
        String wf;    // wall facing (NORTH/EAST/SOUTH/WEST), optional
        // owner/profile
        String ou;    // owner uuid string
        String on;    // owner name
        String tx;    // textures base64
        String sg;    // textures signature (optional)
        // custom name (JSON component; may be stringified JSON)
        String nm;
    }

    /** Extract head payload from a placed skull block. Returns null if block is not a head. */
    public static HeadPayload extract(Block block) {
        if (block == null || !isHead(block)) return null;

        HeadPayload p = new HeadPayload();
        p.m = safeMatName(block);
        p.bd = getBlockDataString(block); // null on 1.7-1.12

        // Mount + orientation
        MountFace mf = readMountAndFaces(block);
        p.mount = mf.mount;
        p.rf = mf.rotationFace; // optional
        p.wf = mf.wallFacing;   // optional

        // Owner / texture / name via Skull state
        try {
            Object state = block.getClass().getMethod("getState").invoke(block);
            if (state instanceof Skull) {
                Skull skull = (Skull) state;

                // Owner modern
                try {
                    Object owning = Skull.class.getMethod("getOwningPlayer").invoke(skull);
                    if (owning != null) {
                        try { p.ou = String.valueOf(owning.getClass().getMethod("getUniqueId").invoke(owning)); } catch (Throwable ignored) {}
                        try { p.on = String.valueOf(owning.getClass().getMethod("getName").invoke(owning)); }     catch (Throwable ignored) {}
                    }
                } catch (NoSuchMethodException nsme) {
                    // Legacy
                    try {
                        Object legacy = Skull.class.getMethod("getOwner").invoke(skull);
                        if (legacy != null) p.on = String.valueOf(legacy);
                    } catch (Throwable ignored) {}
                }

                // Texture via internal GameProfile "profile"
                try {
                    Field profileField = skull.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    Object gp = profileField.get(skull);
                    if (gp != null) {
                        if (p.ou == null) {
                            try { Object id = gp.getClass().getMethod("getId").invoke(gp);
                                if (id != null) p.ou = String.valueOf(id); } catch (Throwable ignored) {}
                        }
                        if (p.on == null) {
                            try { Object nm = gp.getClass().getMethod("getName").invoke(gp);
                                if (nm != null) p.on = String.valueOf(nm); } catch (Throwable ignored) {}
                        }
                        Collection<?> props = null;
                        try {
                            Object map = gp.getClass().getMethod("properties").invoke(gp);
                            props = getTextures(map);
                        } catch (NoSuchMethodException nsme) {
                            Object map = gp.getClass().getMethod("getProperties").invoke(gp);
                            props = getTextures(map);
                        }
                        if (props != null && !props.isEmpty()) {
                            Object prop = props.iterator().next();
                            p.tx = callString(prop, "value", "getValue");
                            p.sg = callString(prop, "signature", "getSignature");
                        }
                    }
                } catch (Throwable ignored) {}

                // Custom name (best effort)
                p.nm = tryReadCustomNameJson(skull);
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[GravesX] Skull read error: " + t.getMessage());
        }

        // Normalize empty strings to null to keep JSON small
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

    // =====================================================================================
    // Internals
    // =====================================================================================

    private static boolean isHead(Block b) {
        String n = safeMatName(b);
        return "PLAYER_HEAD".equals(n) || "PLAYER_WALL_HEAD".equals(n) || "SKULL".equals(n) || "LEGACY_SKULL".equals(n);
    }
    private static String safeMatName(Block b) { try { Material m = b.getType(); return m != null ? m.name() : null; } catch (Throwable t) { return null; } }
    private static boolean empty(String s) { return s == null || s.isEmpty(); }

    private static String getBlockDataString(Block block) {
        // 1.13+ only
        try {
            Object bd = block.getClass().getMethod("getBlockData").invoke(block);
            if (bd != null) {
                Method asString = bd.getClass().getMethod("getAsString", boolean.class);
                return String.valueOf(asString.invoke(bd, true));
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /** Mount + faces across eras. */
    private static final class MountFace {
        String mount;       // FLOOR | WALL | UNKNOWN
        String rotationFace;
        String wallFacing;
    }

    private static MountFace readMountAndFaces(Block block) {
        MountFace mf = null;

        // 1) 1.13+ Rotatable/Directional
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

        // 2) Legacy 1.7-1.12 org.bukkit.material.Skull
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

    private static String cardinal(String face) {
        if (face == null) return null;
        switch (face.toUpperCase(Locale.ROOT)) {
            case "NORTH": return "NORTH";
            case "SOUTH": return "SOUTH";
            case "EAST":  return "EAST";
            case "WEST":  return "WEST";
            default: return null;
        }
    }

    private static Class<?> tryLoad(String name) { try { return Class.forName(name); } catch (Throwable ignored) {} return null; }

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

    private static String callString(Object target, String modern, String legacy) {
        if (target == null) return null;
        try { return String.valueOf(target.getClass().getMethod(modern).invoke(target)); }
        catch (Throwable ignored) {}
        try { return String.valueOf(target.getClass().getMethod(legacy).invoke(target)); }
        catch (Throwable ignored) {}
        return null;
    }

    private static String tryReadCustomNameJson(Skull skull) {
        // Adventure Component (Paper): skull.customName()
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

        // Legacy: getCustomName()
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

    // =====================================================================================
    // JSON (tiny, dependency-free)
    // =====================================================================================

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
        // remove trailing comma if present
        if (sb.charAt(sb.length()-1) == ',') sb.setLength(sb.length()-1);
        sb.append('}');
        return sb.toString();
    }

    private static void writeInt(StringBuilder sb, String k, int v, boolean always) {
        if (!always) return;
        sb.append('"').append(k).append('"').append(':').append(v).append(',');
    }

    private static void writeStr(StringBuilder sb, String k, String v) {
        if (v == null) return;
        sb.append('"').append(k).append('"').append(':').append('"').append(escape(v)).append('"').append(',');
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Minimal parser back into HeadPayload (optional; only used by parseFromReplaceData)
    private static HeadPayload parseJson(String json) {
        HeadPayload p = new HeadPayload();
        // super light "parser": split top-level by ,"key":
        // (Safe here because values we write are plain strings/ints with no nested objects except nm which is a JSON string)
        Map<String,String> map = new HashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length()-1);

        // Split respecting quotes (simple state machine)
        List<String> pairs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i-1) != '\\')) inStr = !inStr;
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

    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length()-1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return s;
    }

}
