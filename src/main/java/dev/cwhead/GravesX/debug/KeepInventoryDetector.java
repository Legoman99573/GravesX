package dev.cwhead.GravesX.debug;

import com.ranull.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepInventoryDetector {
    private static Graves plugin;
    private static volatile boolean installed = false;

    private static final Set<String> HOOKED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private KeepInventoryDetector() {}

    public static synchronized void install(Graves pluginInstance) {
        if (installed) return;
        installed = true;
        plugin = pluginInstance;
        try {
            wrapAllExcept(plugin);
        } catch (Throwable t) {
            installed = false;
            plugin.getLogger().warning("[KeepInventoryDetector] Install failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            try { plugin.logStackTrace(t instanceof Exception ? (Exception) t : new Exception(t)); } catch (Throwable ignored) {}
        }
    }

    public static void wrapPlugin(Plugin target) {
        if (plugin == null || target == null || target == plugin) return;
        final String key = target.getName();
        if (!HOOKED.add(key)) return;

        try {
            HandlerList list = PlayerDeathEvent.getHandlerList();
            RegisteredListener[] regs = list.getRegisteredListeners().clone();
            Map<Listener, List<RegisteredListener>> byListener = new LinkedHashMap<>();

            Field execField = RegisteredListener.class.getDeclaredField("executor");
            execField.setAccessible(true);

            for (RegisteredListener r : regs) {
                if (r.getPlugin() != target) continue;
                EventExecutor ex = (EventExecutor) execField.get(r);
                if (ex instanceof Wrapper) continue;
                byListener.computeIfAbsent(r.getListener(), k -> new ArrayList<>()).add(r);
            }

            if (byListener.isEmpty()) return;

            PluginManager pm = Bukkit.getPluginManager();
            for (Map.Entry<Listener, List<RegisteredListener>> entry : byListener.entrySet()) {
                Listener listener = entry.getKey();
                List<RegisteredListener> rs = entry.getValue();

                list.unregister(listener);

                for (RegisteredListener r : rs) {
                    final Plugin owner = r.getPlugin();
                    final EventPriority priority = r.getPriority();
                    final boolean ignoreCancelled = r.isIgnoringCancelled();
                    final EventExecutor original = (EventExecutor) execField.get(r);

                    EventExecutor wrapper = new Wrapper(original, owner, priority);
                    pm.registerEvent(PlayerDeathEvent.class, listener, priority, wrapper, owner, ignoreCancelled);
                }
            }
        } catch (Throwable t) {
            HOOKED.remove(key);
            plugin.getLogger().warning("[KeepInventoryDetector] wrapPlugin failed for "
                    + key + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    public static void unhookPlugin(Plugin target) {
        if (target == null) return;
        HOOKED.remove(target.getName());
    }

    private static void wrapAllExcept(Plugin self) throws Exception {
        HandlerList list = PlayerDeathEvent.getHandlerList();
        RegisteredListener[] snapshot = list.getRegisteredListeners().clone();

        Field execField = RegisteredListener.class.getDeclaredField("executor");
        execField.setAccessible(true);

        LinkedHashSet<Plugin> owners = new LinkedHashSet<>();
        for (RegisteredListener r : snapshot) {
            Plugin owner = r.getPlugin();
            if (owner == self) continue;
            EventExecutor ex = (EventExecutor) execField.get(r);
            if (ex instanceof Wrapper) continue;
            owners.add(owner);
        }

        for (Plugin owner : owners) {
            wrapPlugin(owner);
        }
    }

    public static void logWorldsWithGameruleKeepInventoryTrue(Graves plugin) {
        for (World w : Bukkit.getWorlds()) {
            if (isKeepInventoryGameruleTrue(w)) {
                plugin.getLogger().warning("[KeepInventoryDetector] World '" + w.getName()
                        + "' has Keep Inventory Gamerule enabled. Graves will not spawn in this world without using a bypass permission.");
            }
        }
    }

    private static boolean isKeepInventoryGameruleTrue(World w) {
        try {
            Boolean v = w.getGameRuleValue(GameRule.KEEP_INVENTORY);
            if (v != null) return v;
        } catch (Throwable ignored) {}
        try {
            String s = w.getGameRuleValue(GameRule.KEEP_INVENTORY.getName());
            return "true".equalsIgnoreCase(s);
        } catch (Throwable ignored) {}
        return false;
    }

    private static final class Wrapper implements EventExecutor {
        private final EventExecutor delegate;
        private final Plugin owner;
        private final EventPriority priority;

        private Wrapper(EventExecutor delegate, Plugin owner, EventPriority priority) {
            this.delegate = delegate;
            this.owner = owner;
            this.priority = priority;
        }

        @Override
        public void execute(@NotNull Listener l, @NotNull Event e) throws EventException {
            if (!(e instanceof PlayerDeathEvent)) {
                delegate.execute(l, e);
                return;
            }
            PlayerDeathEvent pd = (PlayerDeathEvent) e;

            boolean before = safeGetKeep(pd, false);
            delegate.execute(l, e);
            boolean after = safeGetKeep(pd, before);

            if (!before && after) {
                plugin.getLogger().warning("[KeepInventoryDetector] Keep Inventory was set to true by plugin "
                        + owner.getName() + " v." + owner.getDescription().getVersion() + " (priority " + priority + "). Graves will not spawn in this world without using a bypass permission or editing this plugins configuration. If there is no way to disable keepInventory through configuration or permission, contact the plugin author.");
            }
        }
    }

    private static boolean safeGetKeep(PlayerDeathEvent e, boolean fallback) {
        try { return e.getKeepInventory(); } catch (Throwable ignored) { return fallback; }
    }
}
