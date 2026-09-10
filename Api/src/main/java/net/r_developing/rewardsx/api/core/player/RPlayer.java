package net.r_developing.rewardsx.api.core.player;

import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;

import java.util.UUID;

/**
 * Cross-platform abstraction of a command sender that behaves “like a player”.
 * <p>
 * An implementation can wrap:
 * <ul>
 *   <li>Bukkit / Paper  – {@code org.bukkit.entity.Player} or {@code CommandSender}</li>
 *   <li>BungeeCord     – {@code net.md_5.bungee.api.connection.ProxiedPlayer} or {@code CommandSender}</li>
 *   <li>Velocity       – {@code com.velocitypowered.api.proxy.Player} or {@code CommandSource}</li>
 * </ul>
 * The goal is to shield business logic from the underlying server API.
 */
public interface RPlayer {

    /* ------------------------------------------------------------------------
     * Identity
     * --------------------------------------------------------------------- */

    /**
     * Check if RPlayer is a Player.
     * <p>
     * May return {@code false} when the sender is the console or another
     * non-player source.
     *
     * @return if is sender is Player
     */
    boolean isPlayer();

    /**
     * Get Player for different instance.
     * <p>
     *
     * @return Object Player Class
     */
    Object getPlayer();

    /**
     * Unique identifier of the player on Mojang's network.
     * <p>
     * May return {@code null} when the sender is the console or another
     * non-player source.
     *
     * @return the UUID of the player, or {@code null} if not applicable
     */
    UUID getUniqueId();

    /**
     * Display name shown to other users (e.g. “Notch” or “CONSOLE”).
     *
     * @return sender’s name, never {@code null}
     */
    String getPlayerName();

    /* ------------------------------------------------------------------------
     * Basic message channel
     * --------------------------------------------------------------------- */

    /**
     * Sends a plain chat message to the sender.
     *
     * @param msg legacy-colored string (e.g. “§aHello!”) or already serialized
     */
    void sendMessage(String msg);

    /* ------------------------------------------------------------------------
     * Titles and subtitles
     * --------------------------------------------------------------------- */

    /**
     * Shows a title with custom timings.
     *
     * @param title      main title text
     * @param subtitle   sub title text
     * @param fadeIn   fade-in time in ticks
     * @param stay     stay time in ticks
     * @param fadeOut  fade-out time in ticks
     */
    void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * Convenience overload: title with default timings (10 → 70 → 20 ticks).
     *
     * @param title main title text
     * @param subtitle sub title text
     */
    void sendTitle(String title, String subtitle);

    /* ------------------------------------------------------------------------
     * Command & permission helpers
     * --------------------------------------------------------------------- */

    /**
     * Executes a command as this sender.
     * <p>
     * The argument should NOT start with a slash; the implementation decides
     * whether to prepend it (Bukkit) or call the native execute method
     * (Velocity/Bungee).
     *
     * @param cmd command without leading “/”
     */
    void performCommand(String cmd);

    /**
     * Close player inventory.
     */
    void closeInventory();

    /**
     * Open player inventory.
     */
    void openInventory(Object inv);

    /**
     * Checks if the sender owns a specific permission node.
     *
     * @param perm permission string (e.g. “rewardsx.admin”)
     * @return {@code true} if the sender has the permission
     */
    boolean hasPermission(String perm);

    /**
     * Checks if the sender is online.
     *
     * @return {@code true} if the sender is online
     */
    boolean isOnline();

    void sendPluginMessage(PlatformAdapter plugin, String s, byte[] byteArray);

    void sendMessageWithUrl(String message, String url);
}
