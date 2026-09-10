package net.r_developing.rewardsx.api.core.platform;

/**
 * Platform-independent abstraction for logging.
 *
 * <p>Provides three severity levels (debug, info, warning) without exposing
 * platform-specific logging frameworks. The core code calls these methods and
 * lets implementations handle the actual output.
 *
 * <p>Implementations wrap native loggers:
 *   - SpigotLogger wraps Bukkit's Logger or SLF4J
 *   - VelocityLogger wraps Velocity's SLF4J integration
 *   - BungeeCordLogger wraps BungeeCord's Logger
 *
 * <p>Each implementation decides where logs go (console, files, centralized logging)
 * and whether to include timestamps, colors, or other formatting.
 */
public interface PlatformLogger {
    /**
     * Logs a warning message.
     *
     * <p>Used for recoverable issues, deprecated code, or configuration problems
     * that don't stop the plugin but should be brought to the admin's attention.
     *
     * <p>Examples:
     *   - "Platform invalid. Limited mode enabled."
     *   - "Failed to check for updates: connection timeout"
     *   - "Action for reward X not found, please configure it on the web dashboard!"
     *
     * @param message the warning text
     */
    void warning(String message);

    /**
     * Logs an info message.
     *
     * <p>Used for normal operational events - things that are working as expected
     * but the admin might want to know about.
     *
     * <p>Examples:
     *   - "Successfully connect game server with name: MyServer"
     *   - "Config reloaded from console."
     *   - "Running version: 1.0.0"
     *   - "All translations loaded for it: 45 entries"
     *
     * @param message the informational text
     */
    void info(String message);

    /**
     * Logs a debug message.
     *
     * <p>Used for detailed diagnostic information. Only shown if debug mode is enabled
     * (checked via platform.isDebug()). Helps diagnose issues without cluttering normal logs.
     *
     * <p>Examples:
     *   - "Requesting API (POST) with credentials: id=server-123"
     *   - "Language already loaded: en"
     *   - "Requested message key: rewardReceived"
     *   - "Translated [it] 12/45 - reward_name"
     *
     * @param message the debug text
     */
    void debug(String message);

}