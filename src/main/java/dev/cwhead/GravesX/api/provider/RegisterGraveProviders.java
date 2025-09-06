package dev.cwhead.GravesX.api.provider;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RegisterGraveProviders {
    private RegisterGraveProviders() {}

    public static List<GraveProvider> getAll() {
        List<GraveProvider> out = new ArrayList<>();
        for (RegisteredServiceProvider<GraveProvider> rsp :
                Bukkit.getServicesManager().getRegistrations(GraveProvider.class)) {
            out.add(rsp.getProvider());
        }
        out.sort(Comparator.comparingInt(GraveProvider::order));
        return out;
    }
}
