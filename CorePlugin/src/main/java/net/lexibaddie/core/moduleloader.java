package net.lexibaddie.core;

/**
 * Interface that all addons must implement.
 */
public interface moduleloader {
    /**
     * Called when the addon is loaded by the main plugin.
     * @param plugin The main plugin instance.
     */
    void onLoad(main plugin);

    default void onUnload(main plugin) {
        // Default implementation: do nothing.
    }

    default void onReload(main plugin) {
        // Default implementation: do nothing.
    }
}
