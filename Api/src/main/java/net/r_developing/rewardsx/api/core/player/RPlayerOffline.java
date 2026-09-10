package net.r_developing.rewardsx.api.core.player;

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
public interface RPlayerOffline extends RPlayer {

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

    String getName();

    boolean isBanned();

    boolean hasPlayedBefore();

    long getFirstPlayed();

    long getLastPlayed();

    /* ------------------------------------------------------------------------
     * Basic message channel
     * --------------------------------------------------------------------- */


}
