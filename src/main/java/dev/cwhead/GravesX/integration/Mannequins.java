package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
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

    // Spigot mannequin API
    private final Method setDescriptionString;
    private final Method setHideDescription;
    private final Method setImmovable;
    private final Method mannequinSetPose;

    // Paper / generic entity pose API
    private final Method entitySetPose;
    private final Method entitySetPoseFixed; // (Pose, boolean)

    // Profile setters (Spigot vs Paper)
    private final Method setPlayerProfile;   // Spigot: setPlayerProfile(PlayerProfile)
    private final Method setProfile;         // Paper: setProfile(ResolvableProfile)

    // Optional: Paper description (Component)
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

            // Tag so we can find it later
            String idNoDashes = uuid.toString().replace("-", "");
            living.addScoreboardTag(TAG_PREFIX + idNoDashes);

            // Corpse-like settings
            living.setAI(false);
            living.setSilent(true);
            living.setCanPickupItems(false);
            living.setRemoveWhenFarAway(false);
            living.setCollidable(false);
            living.setInvulnerable(true);
            living.setPersistent(true);

            // Description + immovable (best-effort, Spigot/Paper differ)
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

            // ✅ Pose: SWIMMING
            applySwimmingPose(living);

            // Skin/profile (Spigot vs Paper)
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
            // Spigot: Mannequin#setPose(Pose) :contentReference[oaicite:5]{index=5}
            if (mannequinSetPose != null) {
                mannequinSetPose.invoke(mannequin, Pose.SWIMMING);
                return;
            }

            // Paper: Entity#setPose(Pose, boolean fixed) exists :contentReference[oaicite:6]{index=6}
            if (entitySetPoseFixed != null) {
                entitySetPoseFixed.invoke(mannequin, Pose.SWIMMING, true);
                return;
            }

            // Paper: Entity#setPose(Pose)
            if (entitySetPose != null) {
                entitySetPose.invoke(mannequin, Pose.SWIMMING);
                return;
            }

            // Last resort (may not force the pose visually)
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
            // Spigot path: setPlayerProfile(PlayerProfile) requires textures present :contentReference[oaicite:7]{index=7}
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

            // Paper path: mannequin uses setProfile(ResolvableProfile) :contentReference[oaicite:8]{index=8}
            if (setProfile != null) {
                Object resolvable = buildPaperResolvableProfile(grave.getOwnerUUID());
                if (resolvable != null) {
                    setProfile.invoke(mannequin, resolvable);
                }
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

            Object profile = Bukkit.getServer().createPlayerProfile(owner, name);

            URL skinUrl = extractSkinUrl(grave.getOwnerTexture());
            if (skinUrl == null) return profile;

            Method getTextures = profile.getClass().getMethod("getTextures");
            Object textures = getTextures.invoke(profile);
            if (textures == null) return profile;

            Method setSkin = textures.getClass().getMethod("setSkin", URL.class);
            setSkin.invoke(textures, skinUrl);

            return profile;
        } catch (Throwable t) {
            return null;
        }
    }

    private Object buildPaperResolvableProfile(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        try {
            Class<?> rpClass = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
            Method builderFactory = rpClass.getMethod("resolvableProfile"); // static -> Builder
            Object builder = builderFactory.invoke(null);

            Method uuid = builder.getClass().getMethod("uuid", UUID.class);
            uuid.invoke(builder, ownerUuid);

            // build() comes from DataComponentBuilder
            Method build = builder.getClass().getMethod("build");
            return build.invoke(builder);
        } catch (Throwable ignored) {
            return null;
        }
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
        if (textureValue == null || textureValue.isEmpty()) return null;

        try {
            if (textureValue.startsWith("http://") || textureValue.startsWith("https://")) {
                return new URL(textureValue);
            }

            if (textureValue.startsWith("eyJ")) {
                String json = new String(Base64.getDecoder().decode(textureValue), StandardCharsets.UTF_8);
                Matcher m = BASE64_JSON_URL.matcher(json);
                if (m.find()) {
                    String url = m.group(1);
                    if (url.startsWith("http://") || url.startsWith("https://")) return new URL(url);
                }
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

                // Paper has Entity#setPose(Pose) and setPose(Pose, boolean)
                Method entitySetPose = null;
                Method entitySetPoseFixed = null;
                try {
                    Class<?> entity = Class.forName("org.bukkit.entity.Entity");
                    entitySetPose = tryMethod(entity, "setPose", Pose.class);
                    entitySetPoseFixed = tryMethod(entity, "setPose", Pose.class, boolean.class);
                } catch (Throwable ignored) {
                }

                // Profile setters: Spigot setPlayerProfile(PlayerProfile), Paper setProfile(ResolvableProfile)
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

                // Paper description: setDescription(Component)
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