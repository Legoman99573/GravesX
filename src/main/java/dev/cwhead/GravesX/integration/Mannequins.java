package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.listener.integration.mannequins.MannequinInteractListener;
import dev.cwhead.GravesX.manager.ChunkManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Mannequins extends EntityDataManager {

    private static final String ENTITY_TYPE_NAME = "MANNEQUIN";
    private static final String MANNEQUIN_CLASS_NAME = "org.bukkit.entity.Mannequin";

    private static final String TAG_PREFIX = "gravesx_corpse:";
    private static final String HARDCODED_DESCRIPTION = "<empty>";

    private static final long PROFILE_TIMEOUT_SECONDS = 5L;

    private final Graves plugin;

    private final boolean supported;
    private final Class<?> mannequinClass;

    private final Method setDescriptionString;
    private final Method setDescriptionComponent;
    private final Method setHideDescription;
    private final Method setImmovable;

    private final Method mannequinSetPose;

    private final Method entitySetPose;
    private final Method entitySetPoseFixed;

    private final Method setProfile;
    private final Method setPlayerProfile;

    public Mannequins(Graves plugin) {
        super(plugin);
        this.plugin = plugin;

        Reflection r = Reflection.resolve(plugin);

        this.supported = r.supported;
        this.mannequinClass = r.mannequinClass;

        this.setDescriptionString = r.setDescriptionString;
        this.setDescriptionComponent = r.setDescriptionComponent;
        this.setHideDescription = r.setHideDescription;
        this.setImmovable = r.setImmovable;

        this.mannequinSetPose = r.mannequinSetPose;

        this.entitySetPose = r.entitySetPose;
        this.entitySetPoseFixed = r.entitySetPoseFixed;

        this.setProfile = r.setProfile;
        this.setPlayerProfile = r.setPlayerProfile;

        registerListeners();
    }

    public void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(new MannequinInteractListener(plugin), plugin);
    }

    public boolean isSupported() {
        return supported;
    }

    public void createCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getSchedulerManager().runTask(() -> {
            if (!supported) return;

            if (!plugin.getConfigManager().getConfigSection("mannequins.corpse.enabled", grave)
                    .getBoolean("mannequins.corpse.enabled")) return;

            if (grave.getOwnerType() != EntityType.PLAYER) return;
            if (location == null || location.getWorld() == null) return;

            Location mannequinLocation = location.clone();
            try {
                double x = plugin.getConfigManager().getConfigSection("mannequins.corpse.offset.x", grave).getDouble("mannequins.corpse.offset.x");
                double y = plugin.getConfigManager().getConfigSection("mannequins.corpse.offset.y", grave).getDouble("mannequins.corpse.offset.y");
                double z = plugin.getConfigManager().getConfigSection("mannequins.corpse.offset.z", grave).getDouble("mannequins.corpse.offset.z");
                mannequinLocation.add(x, y, z);
            } catch (IllegalArgumentException ignored) {
                mannequinLocation.add(-0.5, 0, -0.5);
            }

            EntityType mannequinType;
            try {
                mannequinType = EntityType.valueOf(ENTITY_TYPE_NAME);
            } catch (Throwable ignored) {
                return;
            }

            Entity spawned;
            try {
                spawned = Objects.requireNonNull(mannequinLocation.getWorld()).spawnEntity(mannequinLocation, mannequinType);
            } catch (Throwable t) {
                return;
            }

            if (!(spawned instanceof LivingEntity living) || !mannequinClass.isInstance(spawned)) {
                spawned.remove();
                return;
            }

            String idNoDashes = uuid.toString().replace("-", "");
            living.addScoreboardTag(TAG_PREFIX + idNoDashes);

            living.setAI(false);
            living.setSilent(true);
            living.setCanPickupItems(false);
            living.setRemoveWhenFarAway(false);
            living.setCollidable(false);
            living.setInvulnerable(true);
            living.setPersistent(true);

            try {
                if (setDescriptionString != null) setDescriptionString.invoke(living, HARDCODED_DESCRIPTION);
                if (setDescriptionComponent != null) {
                    Object comp = buildAdventureText(HARDCODED_DESCRIPTION);
                    if (comp != null) setDescriptionComponent.invoke(living, comp);
                }
                if (setHideDescription != null) setHideDescription.invoke(living, true);
                if (setImmovable != null) setImmovable.invoke(living, true);
            } catch (Throwable ignored) {}

            applySwimmingPose(living);

            try {
                createEntityData(living.getLocation(), living.getUniqueId(), grave.getUUID(), EntityData.Type.MANNEQUIN);
                plugin.debugMessage("[Mannequins] spawned=" + spawned.getClass().getName()
                        + " setProfile=" + (setProfile != null)
                        + " setPlayerProfile=" + (setPlayerProfile != null), 2);

                applySkin(living, grave);

                if (plugin.getConfigManager().getConfigSection("mannequins.corpse.armor", grave)
                        .getBoolean("mannequins.corpse.armor")) {
                    equipArmor(living, grave);
                }
                if (plugin.getConfigManager().getConfigSection("mannequins.corpse.hand", grave)
                        .getBoolean("mannequins.corpse.hand")) {
                    equipHands(living, grave);
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("[Mannequins] createEntityData failed: " + t.getCause());
                plugin.logStackTrace(t);
            }
        });
    }

    public void removeCorpse(Grave grave) {
        if (!supported || grave == null) return;

        Location base = safeGraveLocation(grave);
        String idNoDashes = grave.getUUID().toString().replace("-", "");
        String tag = TAG_PREFIX + idNoDashes;

        if (base != null && base.getWorld() != null) {
            Location anchor = base.clone();

            plugin.getChunkManager().ensureLoadedAndExecute(
                    anchor,
                    base,
                    false,
                    false,
                    () -> {
                        try {
                            Collection<Entity> nearby = base.getWorld().getNearbyEntities(base, 16, 16, 16);
                            for (Entity e : nearby) {
                                if (!isTaggedCorpse(e, tag)) continue;

                                try {
                                    EntityData data = new EntityData(
                                            e.getLocation(),
                                            e.getUniqueId(),
                                            grave.getUUID(),
                                            EntityData.Type.MANNEQUIN
                                    );

                                    plugin.getDataManager().removeEntityData(data);
                                } catch (Throwable t) {
                                    plugin.getLogger().severe(t.getMessage());
                                    plugin.logStackTrace(t);
                                }

                                e.remove();
                            }
                        } catch (Throwable t) {
                            plugin.getLogger().severe(t.getMessage());
                            plugin.logStackTrace(t);
                        }
                    }
            );

            return;
        }

        if (plugin.getChunkManager().getChunkType().effective() == ChunkManager.ChunkType.FOLIA) {
            return;
        }

        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (!isTaggedCorpse(e, tag)) continue;

                try {
                    EntityData data = new EntityData(e.getLocation(), e.getUniqueId(), grave.getUUID(), EntityData.Type.MANNEQUIN);

                    plugin.getDataManager().removeEntityData(data);
                } catch (Throwable t) {
                    plugin.getLogger().severe(t.getMessage());
                    plugin.logStackTrace(t);
                }

                e.remove();
            }
        }
    }

    public boolean hasCorpse(Grave grave) {
        if (!supported || grave == null) return false;

        Location base = safeGraveLocation(grave);
        String idNoDashes = grave.getUUID().toString().replace("-", "");
        String tag = TAG_PREFIX + idNoDashes;

        if (base != null && base.getWorld() != null) {
            for (Entity e : base.getWorld().getNearbyEntities(base, 16, 16, 16)) {
                if (isTaggedCorpse(e, tag)) return true;
            }
            return false;
        }

        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (isTaggedCorpse(e, tag)) return true;
            }
        }
        return false;
    }

    private boolean isTaggedCorpse(Entity e, String tag) {
        if (e == null) return false;
        if (!mannequinClass.isInstance(e)) return false;
        return e.getScoreboardTags().contains(tag);
    }

    private void applySwimmingPose(LivingEntity mannequin) {
        try {
            if (mannequinSetPose != null) {
                mannequinSetPose.invoke(mannequin, Pose.SWIMMING);
                return;
            }
            if (entitySetPoseFixed != null) {
                entitySetPoseFixed.invoke(mannequin, Pose.SWIMMING, true);
                return;
            }
            if (entitySetPose != null) {
                entitySetPose.invoke(mannequin, Pose.SWIMMING);
                return;
            }
            mannequin.setSwimming(true);
        } catch (Throwable ignored) {}
    }

    private void equipArmor(LivingEntity mannequin, Grave grave) {
        EntityEquipment eq = mannequin.getEquipment();
        if (eq == null) return;

        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HEAD))
            eq.setHelmet(grave.getEquipmentMap().get(EquipmentSlot.HEAD));
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.CHEST))
            eq.setChestplate(grave.getEquipmentMap().get(EquipmentSlot.CHEST));
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.LEGS))
            eq.setLeggings(grave.getEquipmentMap().get(EquipmentSlot.LEGS));
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.FEET))
            eq.setBoots(grave.getEquipmentMap().get(EquipmentSlot.FEET));
    }

    private void equipHands(LivingEntity mannequin, Grave grave) {
        EntityEquipment eq = mannequin.getEquipment();
        if (eq == null) return;

        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND))
            eq.setItemInMainHand(grave.getEquipmentMap().get(EquipmentSlot.HAND));

        if (plugin.getVersionManager().hasSecondHand() && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND))
            eq.setItemInOffHand(grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
    }

    private void applySkin(LivingEntity mannequin, Grave grave) {
        if (grave == null) return;

        UUID owner = grave.getOwnerUUID();
        if (owner == null) return;

        String username = null;
        try {
            OfflinePlayer off = Bukkit.getOfflinePlayer(owner);
            username = (off != null) ? off.getName() : null;
        } catch (Throwable ignored) {}
        if (username == null || username.isEmpty()) username = "GravesX";

        final String texturesBase64 = toValidBase64Texture(grave.getOwnerTexture());
        final String sig = safeString(grave::getOwnerTextureSignature);

        // Paper mannequin: setProfile(ResolvableProfile)
        if (setProfile != null) {
            String ptName = setProfile.getParameterTypes()[0].getName();
            if ("io.papermc.paper.datacomponent.item.ResolvableProfile".equals(ptName)) {
                plugin.debugMessage("[Mannequins] applySkin: Paper ResolvableProfile path; name=" + username, 1);
                applySkinPaperResolvable(mannequin, username, texturesBase64, sig);
                return;
            }
        }

        // Spigot-ish mannequin: setPlayerProfile(PlayerProfile)
        if (setPlayerProfile != null) {
            String ptName = setPlayerProfile.getParameterTypes()[0].getName();
            if ("org.bukkit.profile.PlayerProfile".equals(ptName)) {
                plugin.debugMessage("[Mannequins] applySkin: Bukkit PlayerProfile path; name=" + username, 1);
                applySkinBukkitProfile(mannequin, owner, username, texturesBase64, sig);
                return;
            }
        }

        plugin.debugMessage("[Mannequins] applySkin: no compatible skin setter found", 1);
    }

    /**
     * Paper mannequin path without reflecting ResolvableProfile or PlayerProfile:
     * - build com.destroystokyo.paper.profile.PlayerProfile (Bukkit.createProfile)
     * - inject textures (or update() fallback)
     * - wrap via ResolvableProfile.resolvableProfile(profile)
     * - invoke mannequin setProfile via reflected method we already discovered
     */
    private void applySkinPaperResolvable(LivingEntity mannequin, String username, String texturesBase64, String sig) {
        // Do the heavy work async (especially if update() happens)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Object resolved;

                // If we have textures, avoid network
                if (texturesBase64 != null && !texturesBase64.isEmpty()) {
                    resolved = PaperSkin.buildResolvedFromTextures(username, texturesBase64, sig);
                } else {
                    // Otherwise, do update() with timeout like the example plugin
                    CompletableFuture<Object> fut = CompletableFuture.supplyAsync(() -> PaperSkin.buildResolvedFromLookup(username));
                    resolved = fut.get(PROFILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }

                if (resolved == null) {
                    plugin.debugMessage("[Mannequins] applySkinPaperResolvable: resolved profile is null", 1);
                    return;
                }

                Object finalResolved = resolved;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!mannequin.isValid()) return;
                    try {
                        setProfile.invoke(mannequin, finalResolved);
                        plugin.debugMessage("[Mannequins] applySkinPaperResolvable: setProfile success", 1);
                    } catch (Throwable t) {
                        plugin.debugMessage("[Mannequins] applySkinPaperResolvable: setProfile failed: " + t, 1);
                    }
                });

            } catch (Throwable t) {
                plugin.debugMessage("[Mannequins] applySkinPaperResolvable failed: " + t, 1);

                // Default skin fallback (Steve-ish) like the other plugin
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!mannequin.isValid()) return;
                    try {
                        Object resolved = PaperSkin.buildResolvedDefault(username);
                        if (resolved != null) {
                            setProfile.invoke(mannequin, resolved);
                            plugin.debugMessage("[Mannequins] applySkinPaperResolvable: default skin applied", 2);
                        }
                    } catch (Throwable ignored) {}
                });
            }
        });
    }

    private void applySkinBukkitProfile(LivingEntity mannequin, UUID uuid, String username, String texturesBase64, String sig) {
        try {
            Object profile = BukkitSkin.buildProfile(uuid, username, texturesBase64, sig);
            if (profile == null) {
                plugin.debugMessage("[Mannequins] applySkinBukkitProfile: profile null", 1);
                return;
            }
            setPlayerProfile.invoke(mannequin, profile);
            plugin.debugMessage("[Mannequins] applySkinBukkitProfile: setPlayerProfile success", 1);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.debugMessage("[Mannequins] applySkinBukkitProfile: Bukkit profile API not present: " + e, 1);
        } catch (Throwable t) {
            plugin.debugMessage("[Mannequins] applySkinBukkitProfile failed: " + t, 1);
        }
    }

    private static String toValidBase64Texture(String graveTexture) {
        if (graveTexture == null) return "";

        String s = graveTexture.trim();
        if (s.isEmpty()) return "";

        if (s.startsWith("eyJ")) return s; // already base64 json

        String url = s;

        if (s.matches("^[0-9a-fA-F]{32,}$")) {
            url = "https://textures.minecraft.net/texture/" + s;
        } else if (s.startsWith("textures.minecraft.net/texture/")) {
            url = "https://" + s;
        }

        if (!(url.startsWith("http://") || url.startsWith("https://"))) return "";

        String json = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private Object buildAdventureText(String text) {
        try {
            Class<?> component = Class.forName("net.kyori.adventure.text.Component");
            Method m = component.getMethod("text", String.class);
            return m.invoke(null, text);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Location safeGraveLocation(Grave grave) {
        try {
            Method m = grave.getClass().getMethod("getLocation");
            Object loc = m.invoke(grave);
            return (loc instanceof Location l) ? l : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safeString(SupplierThrows<String> sup) {
        try {
            String s = sup.get();
            return (s == null) ? null : s.trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SupplierThrows<T> {
        T get() throws Throwable;
    }

    /**
     * Paper-only skin logic (NO reflection of ResolvableProfile / PlayerProfile).
     * If Paper classes aren't present, class loading will throw NoClassDefFoundError,
     * which we catch where this is called.
     */
    private static final class PaperSkin {
        private PaperSkin() {}

        static Object buildResolvedFromTextures(String username, String texturesBase64, String sig) {
            try {
                // Paper legacy profile
                com.destroystokyo.paper.profile.PlayerProfile profile = (com.destroystokyo.paper.profile.PlayerProfile) Bukkit.createPlayerProfile(username);

                // inject textures property
                if (sig != null && !sig.isEmpty()) {
                    profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", texturesBase64, sig));
                } else {
                    profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", texturesBase64));
                }

                // Wrap to ResolvableProfile
                return io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(profile);

            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                // Paper not present or old Paper
                return null;
            }
        }

        static Object buildResolvedFromLookup(String username) {
            try {
                com.destroystokyo.paper.profile.PlayerProfile profile = (com.destroystokyo.paper.profile.PlayerProfile) Bukkit.createPlayerProfile(username);
                profile.update(); // Mojang lookup
                return io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(profile);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                return null;
            }
        }

        static Object buildResolvedDefault(String username) {
            try {
                // Default profile (no update)
                com.destroystokyo.paper.profile.PlayerProfile profile = (com.destroystokyo.paper.profile.PlayerProfile) Bukkit.createPlayerProfile(username);
                return io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(profile);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                return null;
            }
        }
    }

    /**
     * Bukkit-only skin logic (NO reflection of Bukkit PlayerProfile).
     * If Bukkit profile API isn't present, class loading will throw NoClassDefFoundError,
     * which we catch where this is called.
     */
    private static final class BukkitSkin {
        private static final Pattern BASE64_JSON_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

        private BukkitSkin() {}

        static Object buildProfile(UUID uuid, String name, String textureString, String sigIgnored) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(uuid, name);

                URL skinUrl = extractSkinUrl(textureString);
                if (skinUrl != null) {
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(skinUrl);
                    profile.setTextures(textures);
                }

                return profile;
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                return null;
            } catch (Throwable t) {
                return null;
            }
        }

        private static URL extractSkinUrl(String textureValue) {
            if (textureValue == null) return null;

            String s = textureValue.trim();
            if (s.isEmpty()) return null;

            try {
                // Direct URL
                if (s.startsWith("http://") || s.startsWith("https://")) {
                    return new URL(s);
                }

                // textures.minecraft.net/texture/<hash>
                if (s.startsWith("textures.minecraft.net/texture/")) {
                    return new URL("https://" + s);
                }

                // Raw hash
                if (s.matches("^[0-9a-fA-F]{32,}$")) {
                    return new URL("https://textures.minecraft.net/texture/" + s);
                }

                // Base64 JSON
                // (your stored value might be the base64-encoded textures payload)
                String json = new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
                Matcher m = BASE64_JSON_URL.matcher(json);
                if (m.find()) {
                    String url = m.group(1);
                    if (url.startsWith("http://") || url.startsWith("https://")) return new URL(url);
                    if (url.startsWith("textures.minecraft.net/texture/")) return new URL("https://" + url);
                }
            } catch (Throwable ignored) {
            }

            return null;
        }
    }

    private static final class Reflection {
        final boolean supported;
        final Class<?> mannequinClass;

        final Method setDescriptionString;
        final Method setDescriptionComponent;
        final Method setHideDescription;
        final Method setImmovable;

        final Method mannequinSetPose;

        final Method entitySetPose;
        final Method entitySetPoseFixed;

        final Method setProfile;
        final Method setPlayerProfile;

        private Reflection(boolean supported,
                           Class<?> mannequinClass,
                           Method setDescriptionString,
                           Method setDescriptionComponent,
                           Method setHideDescription,
                           Method setImmovable,
                           Method mannequinSetPose,
                           Method entitySetPose,
                           Method entitySetPoseFixed,
                           Method setProfile,
                           Method setPlayerProfile) {
            this.supported = supported;
            this.mannequinClass = mannequinClass;

            this.setDescriptionString = setDescriptionString;
            this.setDescriptionComponent = setDescriptionComponent;
            this.setHideDescription = setHideDescription;
            this.setImmovable = setImmovable;

            this.mannequinSetPose = mannequinSetPose;

            this.entitySetPose = entitySetPose;
            this.entitySetPoseFixed = entitySetPoseFixed;

            this.setProfile = setProfile;
            this.setPlayerProfile = setPlayerProfile;
        }

        static Reflection resolve(Graves plugin) {
            try {
                EntityType.valueOf(ENTITY_TYPE_NAME);
                Class<?> mannequin = Class.forName(MANNEQUIN_CLASS_NAME);

                Method setImmovable = tryMethod(mannequin, "setImmovable", boolean.class);
                Method setHideDescription = tryMethod(mannequin, "setHideDescription", boolean.class);
                Method setDescriptionString = tryMethod(mannequin, "setDescription", String.class);

                Method mannequinSetPose = tryMethod(mannequin, "setPose", Pose.class);

                Method entitySetPose = null;
                Method entitySetPoseFixed = null;
                try {
                    Class<?> entity = Class.forName("org.bukkit.entity.Entity");
                    entitySetPose = tryMethod(entity, "setPose", Pose.class);
                    entitySetPoseFixed = tryMethod(entity, "setPose", Pose.class, boolean.class);
                } catch (Throwable ignored) {}

                Method setProfile = null;
                Method setPlayerProfile = null;
                for (Method m : mannequin.getMethods()) {
                    if (m.getName().equals("setProfile") && m.getParameterCount() == 1) setProfile = m;
                    if (m.getName().equals("setPlayerProfile") && m.getParameterCount() == 1) setPlayerProfile = m;
                }

                Method setDescriptionComponent = null;
                try {
                    Class<?> component = Class.forName("net.kyori.adventure.text.Component");
                    setDescriptionComponent = tryMethod(mannequin, "setDescription", component);
                } catch (Throwable ignored) {}

                boolean ok = (setImmovable != null)
                        && (mannequinSetPose != null || entitySetPose != null || entitySetPoseFixed != null)
                        && (setProfile != null || setPlayerProfile != null);

                plugin.debugMessage("[Mannequins] Reflection: ok=" + ok
                                + " setProfile=" + (setProfile != null ? setProfile.getParameterTypes()[0].getName() : "null")
                                + " setPlayerProfile=" + (setPlayerProfile != null ? setPlayerProfile.getParameterTypes()[0].getName() : "null"),
                        2);

                return new Reflection(ok, mannequin,
                        setDescriptionString, setDescriptionComponent, setHideDescription, setImmovable,
                        mannequinSetPose,
                        entitySetPose, entitySetPoseFixed,
                        setProfile, setPlayerProfile);

            } catch (Throwable t) {
                plugin.debugMessage("[Mannequins] Reflection failed: " + t, 1);
                return new Reflection(false, Object.class,
                        null, null, null, null,
                        null,
                        null, null,
                        null, null);
            }
        }

        private static Method tryMethod(Class<?> clazz, String name, Class<?>... params) {
            try {
                return clazz.getMethod(name, params);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
