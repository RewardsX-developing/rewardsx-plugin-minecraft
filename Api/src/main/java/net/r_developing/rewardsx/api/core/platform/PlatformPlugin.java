package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.network.RewardPollingTask;

/**
 * Platform-independent plugin metadata accessor.
 *
 * <p>Provides basic information about the RewardsX plugin instance that varies
 * by platform. Implementations wrap platform-specific plugin objects (JavaPlugin
 * for Bukkit, PluginContainer for Velocity, PluginDescription for BungeeCord).
 *
 * <p>This interface is used internally for logging, version checking, and accessing
 * the polling task. Keeps the core code from importing platform-specific classes.
 */
public interface PlatformPlugin {
    /**
     * Returns the plugin name.
     *
     * <p>Typically "RewardsX" (as defined in plugin.yml or plugin.conf).
     *
     * @return the plugin name
     */
    String getName();

    /**
     * Returns the plugin version.
     *
     * <p>Read from the plugin metadata (plugin.yml on Spigot, velocity-plugin.json
     * on Velocity, etc.). Used for version checking and update notifications.
     *
     * @return the version string (e.g. "1.0.0", "2.1.3-SNAPSHOT")
     */
    String getVersion();

    /**
     * Returns the raw platform-specific plugin instance.
     *
     * <p>On Spigot, this is a JavaPlugin instance.
     * On Velocity, this is a PluginContainer instance.
     * On BungeeCord, this is a Plugin instance.
     *
     * <p>Used when third-party libraries or platform APIs need the actual
     * plugin object (e.g. for resource loading, data folder access, or
     * registering with the platform).
     *
     * @return the platform-specific plugin instance
     */
    Object getPlugin();

    /**
     * Returns the reward polling task.
     *
     * <p>Provides access to the task that periodically fetches pending reward
     * grants from the backend. The core code uses this to start/stop polling
     * and track in-flight grants.
     *
     * @return the RewardPollingTask instance
     */
    RewardPollingTask getPollingTask();
}