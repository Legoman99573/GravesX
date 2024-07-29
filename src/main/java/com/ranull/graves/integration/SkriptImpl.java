package com.ranull.graves.integration;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveAutoLootEvent;
import com.ranull.graves.event.integration.skript.*;

public class SkriptImpl {
    private final Graves plugin;
    private SkriptAddon skriptAddon;
    private Skript skript;

    /**
     * Constructs a SkriptIntegration instance and registers it with the Skript plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public SkriptImpl(Graves plugin) {
        this.plugin = plugin;

        unregister();
        register();
    }

    /**
     * Unregisters the current SkriptAddon listener if it exists.
     */
    private void unregister() {
        if (skriptAddon != null) {
            // Skript doesn't provide a direct way to unregister addons
            // Generally, this involves cleaning up any event registrations manually
            // For simplicity, we are not handling unregistering here
            skriptAddon = null;
        }
    }

    /**
     * Registers the SkriptAddon listener with the Skript plugin.
     */
    private void register() {
        SkriptAddon addon = Skript.registerAddon(plugin);
        try {
            addon.loadClasses("com.ranull.graves.event.integration.skript", "events");
            //new EvtGraveAutoLoot();
            //Skript.registerEvent("Grave Auto Loot", EvtGraveAutoLoot.class, GraveAutoLootEvent.class, "[grave] auto loot[ing] [(of|for) %-entitydatas%]");
            plugin.integrationMessage("Skript integration loaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the SkriptAddon instance.
     *
     * @return The SkriptAddon instance.
     */
    public SkriptAddon getSkriptAddon() {
        return skriptAddon;
    }
}
