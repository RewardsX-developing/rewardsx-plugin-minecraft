package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;

import java.nio.file.Path;

/**
 * Abstraction layer for platform-specific implementations.
 *
 * <p>Defines the contract that Bukkit, BungeeCord, and Velocity implementations
 * must fulfill. The core RewardsX logic is platform-independent and uses this
 * interface to interact with server APIs without hard dependencies on any of them.
 *
 * <p>Each platform (Spigot, BungeeCord, Velocity) provides a concrete implementation
 * that wraps the platform's native APIs - inventory opening, task scheduling, logging,
 * command registration, etc. The core code calls methods on PlatformAdapter and lets
 * the implementation decide how to fulfill them.
 */
public interface PlatformAdapter {
    /**
     * Cancels all currently scheduled tasks.
     *
     * <p>Used on plugin disable to clean up. Implementation calls the platform's
     * task scheduler (Bukkit.getScheduler().cancelTasks(), etc.).
     */
    void cancelAllTasks();

    /**
     * Schedules a task to run after a delay.
     *
     * <p>Called for things like checking updates or retrying failed operations.
     * The delay is given in server ticks (20 ticks = 1 second).
     *
     * @param task       the runnable to execute
     * @param delayTicks the delay in server ticks before execution
     */
    void runTaskLater(Runnable task, long delayTicks);

    /**
     * Returns the plugin instance.
     *
     * <p>Used to access plugin metadata and configuration folders.
     * On Spigot, this is a JavaPlugin; on proxies, it's a custom wrapper.
     *
     * @return the platform-specific plugin instance
     */
    PlatformPlugin getInstance();

    /**
     * Returns the GUI implementation for this platform.
     *
     * <p>On Spigot, opens a native inventory. On Velocity, opens a web iframe.
     * The core code calls gui.open() without caring which one.
     *
     * @return the platform's PlatformGUI implementation
     */
    PlatformGUI getGUI();

    /**
     * Returns the logger for this platform.
     *
     * <p>Provides platform-independent logging - abstracts away differences
     * between Bukkit's Logger, BungeeCord's Logger, Velocity's Logger, etc.
     *
     * @return the platform's PlatformLogger implementation
     */
    PlatformLogger getLogger();

    /**
     * Injects core dependencies into the platform.
     *
     * <p>Called during plugin initialization to hand off references to the core
     * logic objects (GUI, commands, proxy listener). The platform stores these
     * and uses them to handle events and user interactions.
     *
     * @param gui             the rewards GUI
     * @param coreCommands    the /rewardsx command handler
     * @param proxyListener   the proxy message listener (maybe null on non-proxies)
     */
    void setupDependencies(PlatformGUI gui, CoreCommands coreCommands, AbstractProxyListener proxyListener);

    /**
     * Registers command executors and event listeners.
     *
     * <p>Called after setupDependencies(). The platform registers the core commands
     * with the server and hooks the core logic into its event system (e.g. player login,
     * inventory click, chat, etc.).
     */
    void registerCommandsAndEvents();

    /**
     * Enum for platform type detection.
     *
     * <p>Used to determine which platform is running and adapt behavior accordingly.
     */
    enum Type { SPIGOT, BUNGEECORD, VELOCITY }

    /**
     * Returns the type of platform this adapter represents.
     *
     * @return one of SPIGOT, BUNGEECORD, or VELOCITY
     */
    PlatformAdapter.Type type();

    // ===== Logging methods (platform-independent) =====
    // These provide consistent logging across all platforms without exposing
    // SLF4J or java.util.logging directly.

    /**
     * Logs an info message.
     *
     * @param msg the message
     */
    void info(String msg);

    /**
     * Logs an error message.
     *
     * @param msg the message
     */
    void error(String msg);

    /**
     * Logs a debug message (only shown if debug mode is enabled).
     *
     * @param msg the message
     */
    void debug(String msg);

    /**
     * Logs a warning message.
     *
     * @param msg the message
     */
    void warning(String msg);

    /**
     * Logs a severe message (highest priority).
     *
     * @param msg the message
     */
    void severe(String msg);

    /**
     * Returns the platform name (e.g. "Spigot", "BungeeCord", "Velocity").
     *
     * @return the platform name
     */
    String getName();

    /**
     * Returns the version of the RewardsX plugin.
     *
     * @return version string (e.g. "1.0.0")
     */
    String getPluginVersion();

    /**
     * Checks whether database functionality is enabled.
     *
     * <p>Used to determine if persistent storage (player data, transactions) is
     * available. If false, the plugin runs in limited mode with in-memory storage only.
     *
     * @return true if database is configured and available
     */
    boolean isDbEnabled();

    /**
     * Returns the data folder path for this plugin.
     *
     * <p>Used to store config files, databases, and other plugin data.
     * On Spigot, this is typically plugins/RewardsX/.
     * On proxies, it's the proxy's plugin data directory.
     *
     * @return the path to the data folder
     */
    Path getDataPath();

    /**
     * Returns the raw plugin instance for use in third-party libraries.
     *
     * <p>Some libraries (like SQLite drivers) need the actual plugin object
     * to function. This returns the platform-specific JavaPlugin/Plugin instance
     * so libraries can use it for logger access, resource loading, etc.
     *
     * @return the platform's plugin instance object (JavaPlugin, PluginContainer, etc.)
     */
    Object pluginInstance();
}