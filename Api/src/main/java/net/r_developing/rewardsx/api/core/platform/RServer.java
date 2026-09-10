package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.player.RPlayerOffline;

import java.util.UUID;

/**
 * Platform-independent abstraction for server-level operations.
 *
 * <p>Provides access to players (online and offline), command dispatch, and event
 * registration without importing platform-specific APIs. Implementations wrap native
 * server objects:
 *   - SpigotServer wraps Bukkit.getServer()
 *   - VelocityServer wraps Velocity's ProxyServer
 *   - BungeeCordServer wraps ProxyServer
 *
 * <p>Used throughout the codebase to look up players by name or UUID, execute
 * console commands, and wire up event listeners.
 */
public interface RServer {
    /**
     * Finds an online player by exact name match.
     *
     * <p>Case-sensitive exact match. Returns null if the player is not online.
     * Preferred over getPlayer(String) when an exact match is required.
     *
     * @param name the player's exact name (e.g. "Steve", not "steve")
     * @return the online player, or null if not found
     */
    RPlayer getPlayerExact(String name);

    /**
     * Finds an online player by UUID.
     *
     * <p>The most reliable lookup method - UUIDs are unique and immutable.
     * Returns null if the player is not online.
     *
     * <p>Used by Config.getPlayerById() to find players when processing reward grants.
     *
     * @param uuid the player's Minecraft UUID
     * @return the online player, or null if not online or not found
     */
    RPlayer getPlayer(UUID uuid);

    /**
     * Finds an online player by name (case-insensitive prefix match).
     *
     * <p>Less strict than getPlayerExact() - matches the first player whose name
     * starts with the given string (case-insensitive). Useful for player commands
     * where the user might type partial names.
     *
     * @param name the player name or partial name
     * @return the online player, or null if no match found
     */
    RPlayer getPlayer(String name);

    /**
     * Gets offline player data by name.
     *
     * <p>Returns a player object even if the player has never logged in or is not
     * currently online. Useful for operations on past players or accessing their UUID.
     *
     * <p>On Spigot, this loads from server.dat or creates a new entry.
     * On proxies, this may be synthetic or cached depending on implementation.
     *
     * @param name the player name
     * @return an offline player object (may not have complete data)
     */
    RPlayerOffline getOfflinePlayer(String name);

    /**
     * Gets offline player data by UUID.
     *
     * <p>Returns a player object even if they're not online. UUID-based lookup
     * is more reliable than name-based since names can change.
     *
     * @param uuid the player's UUID
     * @return an offline player object
     */
    RPlayerOffline getOfflinePlayer(UUID uuid);

    /**
     * Executes a runnable on the main server thread.
     *
     * <p>Convenience method for synchronous execution without needing access to
     * the PlatformScheduler. Used when you're already in an async context and need
     * to do something on the main thread (e.g. in a callback).
     *
     * @param task the runnable to execute on the main thread
     */
    void runSync(Runnable task);

    /**
     * Executes a command as the console.
     *
     * <p>Runs the command with full server privileges, as if the console typed it.
     * Used to execute server commands for utility purposes (e.g. announcements,
     * teleportation, etc.).
     *
     * @param cmd the command (without the leading slash)
     *            e.g. "say Server restarting in 5 minutes"
     */
    void dispatchConsoleCommand(String cmd);

    /**
     * Registers event listeners with the server.
     *
     * <p>Wires up listener objects (classes implementing platform-specific event
     * interfaces) so they receive callbacks when server events occur.
     *
     * <p>Used during plugin initialization to hook the core logic into server
     * events (player login, inventory click, chat, etc.).
     *
     * @param listeners one or more listener objects to register
     */
    void registerEvents(Object... listeners);
}