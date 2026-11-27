package dev.cwhead.GravesX.module;

/**
 * Defines lifecycle hooks for a GravesX module.
 */
public abstract class GravesXModule {

    /**
     * Called after construction when the module is loaded but not yet enabled.
     *
     * @param ctx Module context provided by the host.
     */
    public void onModuleLoad(ModuleContext ctx) {}

    /**
     * Called when the module should become active.
     * Register listeners/commands and start tasks here.
     *
     * @param ctx Module context provided by the host.
     */
    public void onModuleEnable(ModuleContext ctx) {}

    /**
     * Called before the module is unloaded.
     * Unregister and release resources here.
     *
     * @param ctx Module context provided by the host.
     */
    public void onModuleDisable(ModuleContext ctx) {}
}