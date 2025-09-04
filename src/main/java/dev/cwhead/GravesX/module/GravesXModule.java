package dev.cwhead.GravesX.module;

/**
 * Defines lifecycle hooks for a GravesX module.
 */
public interface GravesXModule {

    /**
     * Called after construction when the module is loaded but not yet enabled.
     *
     * @param ctx Module context provided by the host.
     * @throws Exception If initialization fails.
     */
    default void onModuleLoad(ModuleContext ctx) throws Exception {}

    /**
     * Called when the module should become active.
     * Register listeners/commands and start tasks here.
     *
     * @param ctx Module context provided by the host.
     * @throws Exception If enabling fails.
     */
    default void onModuleEnable(ModuleContext ctx) throws Exception {}

    /**
     * Called before the module is unloaded.
     * Unregister and release resources here.
     *
     * @param ctx Module context provided by the host.
     * @throws Exception If shutdown fails.
     */
    default void onModuleDisable(ModuleContext ctx) throws Exception {}
}
