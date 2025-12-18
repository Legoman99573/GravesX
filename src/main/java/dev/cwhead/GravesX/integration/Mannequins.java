package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.util.SkinTextureUtil_post_1_21_9;
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

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Mannequins extends EntityDataManager {

    private static final String ENTITY_TYPE_NAME = "MANNEQUIN";
    private static final String MANNEQUIN_CLASS_NAME = "org.bukkit.entity.Mannequin";

    private static final String HARDCODED_DESCRIPTION = "<empty>";

    private static final String TAG_PREFIX = "gravesx_corpse:";
    private static final Pattern BASE64_JSON_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final Graves plugin;

    private final boolean supported;
    private final Class<?> mannequinClass;

    private final Method setDescriptionString;
    private final Method setHideDescription;
    private final Method setImmovable;
    private final Method mannequinSetPose;

    private final Method entitySetPose;
    private final Method entitySetPoseFixed;

    private final Method setPlayerProfile;
    private final Method setProfile;

    private final Method setDescriptionComponent;

    public Mannequins(Graves plugin) {
        super(plugin);
        this.plugin = plugin;

        Reflection r = Reflection.resolve();
        this.supported = r.supported;
        this.mannequinClass = r.mannequinClass;

        this.setDescriptionString = r.setDescriptionString;
        this.setDescriptionComponent = r.setDescriptionComponent;
        this.setHideDescription = r.setHideDescription;
        this.setImmovable = r.setImmovable;
        this.mannequinSetPose = r.mannequinSetPose;

        this.entitySetPose = r.entitySetPose;
        this.entitySetPoseFixed = r.entitySetPoseFixed;

        this.setPlayerProfile = r.setPlayerProfile;
        this.setProfile = r.setProfile;
    }

    public boolean isSupported() {
        return supported;
    }

    public void createCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getGravesXScheduler().runTask(() -> {
            if (!supported) return;

            if (!plugin.getConfig("mannequins.corpse.enabled", grave).getBoolean("mannequins.corpse.enabled")
                    || grave.getOwnerType() != EntityType.PLAYER) return;

            if (location == null || location.getWorld() == null) return;

            try {
                location.getBlock().setType(Material.AIR);
            } catch (Throwable ignored) {
            }

            Location mannequinLocation = location.clone();
            try {
                double x = plugin.getConfig("mannequins.corpse.offset.x", grave).getDouble("mannequins.corpse.offset.x");
                double y = plugin.getConfig("mannequins.corpse.offset.y", grave).getDouble("mannequins.corpse.offset.y");
                double z = plugin.getConfig("mannequins.corpse.offset.z", grave).getDouble("mannequins.corpse.offset.z");
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
                    Object comp = buildAdventureText();
                    if (comp != null) setDescriptionComponent.invoke(living, comp);
                }
                if (setHideDescription != null) setHideDescription.invoke(living, true);
                if (setImmovable != null) setImmovable.invoke(living, true);
            } catch (Throwable ignored) {
            }

            applySwimmingPose(living);

            applySkinAsync(living, grave);

            // Equipment
            if (plugin.getConfig("mannequins.corpse.armor", grave).getBoolean("mannequins.corpse.armor")) {
                equipArmor(living, grave);
            }
            if (plugin.getConfig("mannequins.corpse.hand", grave).getBoolean("mannequins.corpse.hand")) {
                equipHands(living, grave);
            }
        });
    }

    public void removeCorpse(Grave grave) {
        if (!supported || grave == null) return;

        Location base = safeGraveLocation(grave);
        String idNoDashes = grave.getUUID().toString().replace("-", "");
        String tag = TAG_PREFIX + idNoDashes;

        if (base != null && base.getWorld() != null) {
            Collection<Entity> nearby = base.getWorld().getNearbyEntities(base, 16, 16, 16);
            for (Entity e : nearby) {
                if (isTaggedCorpse(e, tag)) e.remove();
            }
            return;
        }

        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (isTaggedCorpse(e, tag)) e.remove();
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
        } catch (Throwable ignored) {
        }
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

    private void applySkinAsync(LivingEntity mannequin, Grave grave) {
        try {
            if (setPlayerProfile != null) {
                Object profile = buildBukkitProfileWithTextures(grave);
                if (profile == null) return;

                if (profileHasSkin(profile)) {
                    trySetProfileSpigot(mannequin, profile);
                    return;
                }

                Method update = profile.getClass().getMethod("update");
                Object futObj = update.invoke(profile);
                if (futObj instanceof CompletableFuture<?> fut) {
                    fut.thenAccept(updatedProfile -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!mannequin.isValid() || updatedProfile == null) return;
                        if (!profileHasSkin(updatedProfile)) return;
                        trySetProfileSpigot(mannequin, updatedProfile);
                    }));
                }
                return;
            }

            if (setProfile != null) {
                Object resolvable = buildPaperResolvableProfile(grave);
                if (resolvable != null) setProfile.invoke(mannequin, resolvable);
            }
        } catch (Throwable ignored) {
        }
    }


    private void trySetProfileSpigot(LivingEntity mannequin, Object profile) {
        try {
            setPlayerProfile.invoke(mannequin, profile);
        } catch (Throwable ignored) {
        }
    }

    private Object buildBukkitProfileWithTextures(Grave grave) {
        try {
            UUID owner = grave.getOwnerUUID();
            if (owner == null) return null;

            OfflinePlayer off = Bukkit.getOfflinePlayer(owner);
            String name = off.getName();
            if (name == null || name.isEmpty()) name = "GravesX";

            String tex = grave.getOwnerTexture();

            return dev.cwhead.GravesX.util.SkinTextureUtil_post_1_21_9
                    .createProfileFromTextureString(owner, name, tex);
        } catch (Throwable t) {
            return null;
        }
    }

    private Object buildPaperResolvableProfile(Grave grave) {
        UUID ownerUuid = grave.getOwnerUUID();
        if (ownerUuid == null) return null;

        try {
            Class<?> rpClass = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
            Object builder = rpClass.getMethod("resolvableProfile").invoke(null);

            builder.getClass().getMethod("uuid", UUID.class).invoke(builder, ownerUuid);

            try {
                OfflinePlayer off = Bukkit.getOfflinePlayer(ownerUuid);
                String name = (off != null) ? off.getName() : null;
                if (name != null && !name.isEmpty()) {
                    builder.getClass().getMethod("name", String.class).invoke(builder, name);
                }
            } catch (Throwable ignored) {}

            String texturesBase64 = toValidBase64Texture(grave.getOwnerTexture());
            String sig = null;
            try { sig = grave.getOwnerTextureSignature(); } catch (Throwable ignored) {}

            if (texturesBase64 != null && !texturesBase64.isEmpty()) {
                Class<?> propClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                Object prop;
                try {
                    prop = propClass.getConstructor(String.class, String.class, String.class)
                            .newInstance("textures", texturesBase64, (sig == null || sig.isEmpty()) ? null : sig);
                } catch (NoSuchMethodException ignored) {
                    prop = propClass.getConstructor(String.class, String.class)
                            .newInstance("textures", texturesBase64);
                }

                builder.getClass().getMethod("addProperty", propClass).invoke(builder, prop);
            }

            return builder.getClass().getMethod("build").invoke(builder);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String toValidBase64Texture(String graveTexture) {
        if (graveTexture == null) return "";

        String s = graveTexture.trim();
        if (s.isEmpty()) return "";

        if (s.startsWith("eyJ")) return s;

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


    private boolean profileHasSkin(Object profile) {
        try {
            Method getTextures = profile.getClass().getMethod("getTextures");
            Object textures = getTextures.invoke(profile);
            if (textures == null) return false;

            Method getSkin = textures.getClass().getMethod("getSkin");
            Object url = getSkin.invoke(textures);
            return url instanceof URL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private URL extractSkinUrl(String textureValue) {
        if (textureValue == null) return null;

        String s = textureValue.trim();
        if (s.isEmpty()) return null;

        try {
            if (s.startsWith("http://") || s.startsWith("https://")) {
                return new URL(s);
            }

            if (s.startsWith("textures.minecraft.net/texture/")) {
                return new URL("https://" + s);
            }

            if (s.matches("^[0-9a-fA-F]{32,}$")) {
                return new URL("https://textures.minecraft.net/texture/" + s);
            }

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

    private Object buildAdventureText() {
        try {
            Class<?> component = Class.forName("net.kyori.adventure.text.Component");
            Method m = component.getMethod("text", String.class);
            return m.invoke(null, Mannequins.HARDCODED_DESCRIPTION);
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

        final Method setPlayerProfile;
        final Method setProfile;

        private Reflection(boolean supported,
                           Class<?> mannequinClass,
                           Method setDescriptionString,
                           Method setDescriptionComponent,
                           Method setHideDescription,
                           Method setImmovable,
                           Method mannequinSetPose,
                           Method entitySetPose,
                           Method entitySetPoseFixed,
                           Method setPlayerProfile,
                           Method setProfile) {
            this.supported = supported;
            this.mannequinClass = mannequinClass;
            this.setDescriptionString = setDescriptionString;
            this.setDescriptionComponent = setDescriptionComponent;
            this.setHideDescription = setHideDescription;
            this.setImmovable = setImmovable;
            this.mannequinSetPose = mannequinSetPose;
            this.entitySetPose = entitySetPose;
            this.entitySetPoseFixed = entitySetPoseFixed;
            this.setPlayerProfile = setPlayerProfile;
            this.setProfile = setProfile;
        }

        static Reflection resolve() {
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
                } catch (Throwable ignored) {
                }

                Method setPlayerProfile = null;
                for (Method m : mannequin.getMethods()) {
                    if (m.getName().equals("setPlayerProfile") && m.getParameterCount() == 1) {
                        setPlayerProfile = m;
                        break;
                    }
                }

                Method setProfile = null;
                for (Method m : mannequin.getMethods()) {
                    if (m.getName().equals("setProfile") && m.getParameterCount() == 1) {
                        setProfile = m;
                        break;
                    }
                }

                Method setDescriptionComponent = null;
                try {
                    Class<?> component = Class.forName("net.kyori.adventure.text.Component");
                    setDescriptionComponent = tryMethod(mannequin, "setDescription", component);
                } catch (Throwable ignored) {
                }

                boolean ok = (setImmovable != null) && (mannequinSetPose != null || entitySetPose != null || entitySetPoseFixed != null)
                        && (setPlayerProfile != null || setProfile != null);

                return new Reflection(ok, mannequin,
                        setDescriptionString, setDescriptionComponent, setHideDescription, setImmovable,
                        mannequinSetPose, entitySetPose, entitySetPoseFixed,
                        setPlayerProfile, setProfile);

            } catch (Throwable t) {
                return new Reflection(false, Object.class, null, null, null, null, null, null, null, null, null);
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