package dev.cwhead.GravesX;

import com.ranull.graves.Graves;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

public class GravesXAPI {
    private static GravesXAPI instance;

    private Graves plugin;

    public GravesXAPI(Graves plugin) {
        this.plugin = plugin;
        instance = this;
    }

    //TODO Implement API methods

    public static GravesXAPI getInstance() {
        return instance;
    }

    public void register() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents((Listener) this, plugin);
    }
}
