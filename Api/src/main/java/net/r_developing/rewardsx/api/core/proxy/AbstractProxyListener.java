package net.r_developing.rewardsx.api.core.proxy;

import lombok.Getter;
import lombok.Setter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.buy.Buy;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles incoming plugin channel messages from a proxy network.
 *
 * <p>Completely platform-independent. The native platform listeners (BukkitProxyListener,
 * VelocityProxyListener, etc.) decode the platform-specific plugin message event, extract
 * the byte payload, and pass it here for processing.
 *
 * <p>Expects messages on the "rewardsx:command" channel with a fixed packet format:
 *   - playerUUID:{uuid}
 *   - port:{server-port}
 *   - command:{OPENGUI|RUN}
 *   - content:{command-content-for-RUN}
 *
 * <p>Used on proxy servers (BungeeCord, Velocity) to forward requests from one backend
 * server to another. For example, when a player on Server A wants to use the GUI but the
 * GUI handler is running on the proxy, the backend asks the proxy to open it.
 */
public class AbstractProxyListener {

    /** Console logger. */
    private final PlatformLogger logger;
    /** Access to online players - used to find the target player by UUID. */
    private final RServer server;
    /** Handler for reward grant execution (for RUN command). Injected after construction. */
    @Setter
    @Getter
    private Buy buy;

    /**
     * Constructor injection.
     *
     * @param logger the logger
     * @param server server access for player lookup
     * @param buy    the reward grant handler
     */
    public AbstractProxyListener(PlatformLogger logger, RServer server, Buy buy) {
        this.logger = logger;
        this.server = server;
        this.buy = buy;
    }

    /**
     * Processes an incoming plugin message on the "rewardsx:command" channel.
     *
     * <p>Called by the platform-specific proxy listener after receiving a plugin message event.
     * The listener extracts the byte payload and passes it to this method along with a callback
     * for opening the GUI (which must be done by the platform-specific listener, not here).
     *
     * <p>Packet format - UTF strings separated by newlines:
     *   playerUUID:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     *   port:25566
     *   command:OPENGUI
     *   content:
     *
     * <p>For RUN command:
     *   playerUUID:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     *   port:25566
     *   command:RUN
     *   content:give [player] diamond
     *
     * <p>Port matching:
     *   - port 0: broadcast to all backends (no filter)
     *   - port 999999: broadcast to all backends (no filter)
     *   - other: only process if matches currentServerPort (safety check)
     *
     * @param message      the raw byte payload from the plugin channel
     * @param serverPort   the port of the current backend server (used to filter broadcast messages)
     * @param guiOpener    callback to open the GUI (platform-specific implementation)
     */
    public void handlePluginMessage(byte[] message, int serverPort, GuiOpener guiOpener) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            UUID playerUUID = null;
            int port = 0;
            String command = null;
            String content = ""; // Initialize to empty string to avoid null check failures.

            // Parse the packet - read each UTF line and extract key-value pairs.
            while (in.available() > 0) {
                String msg = in.readUTF();
                if (msg.startsWith("playerUUID:")) {
                    playerUUID = UUID.fromString(msg.substring("playerUUID:".length()).trim());
                } else if (msg.startsWith("port:")) {
                    port = Integer.parseInt(msg.substring("port:".length()).trim());
                } else if (msg.startsWith("command:")) {
                    command = msg.substring("command:".length()).trim().toUpperCase();
                } else if (msg.startsWith("content:")) {
                    content = msg.substring("content:".length()).trim();
                }
            }

            // Validate that a player UUID was provided.
            if (playerUUID == null) {
                logger.warning("Received message without playerUUID");
                return;
            }

            // Find the player by UUID - they must be online on this server.
            RPlayer targetPlayer = server.getPlayer(playerUUID);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                logger.warning("Target player not online: " + playerUUID);
                return;
            }

            // Validate that a command was provided.
            if (command == null) {
                logger.warning("Received plugin message without command");
                return;
            }

            // Port matching - if the packet specifies a port (and it's not 0 or broadcast 999999),
            // only process if it matches this server's port. This prevents a message meant for
            // a different backend from being processed.
            if (port != 0 && port != 999999 && port != serverPort) {
                logger.debug("[AbstractProxyListener] Ignored message: port mismatch (" + port + " != " + serverPort + ")");
                return;
            }

            // Handle the command.
            switch (command) {
                case "OPENGUI":
                    // Open the rewards GUI for the target player.
                    // The actual GUI implementation is platform-specific, so it's provided
                    // as a callback (BukkitProxyListener.openGui, VelocityProxyListener.openGui, etc.).
                    guiOpener.openGui(targetPlayer);
                    break;

                case "RUN":
                    // Execute a command for the target player.
                    // Delegates to Buy.executeCommand() which schedules the command on the main thread
                    // and substitutes the player name into placeholders.
                    if (buy != null) {
                        buy.executeCommand(targetPlayer, content);
                    } else {
                        logger.warning("Buy service is null, cannot execute command RUN");
                    }
                    break;

                default:
                    logger.warning("Not valid command in ProxyListener: " + command);
                    break;
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to decode plugin message", e);
        }
    }

    /**
     * Functional interface for platform-specific GUI opening.
     *
     * <p>Since GUI rendering is platform-specific (Bukkit inventory vs Velocity web frame),
     * the actual implementation is provided by the platform's proxy listener as a callback.
     * This keeps AbstractProxyListener platform-independent.
     *
     * <p>Usage:
     *   guiOpener.openGui(player) - opens the GUI for the player
     */
    public interface GuiOpener {
        /**
         * Opens the rewards GUI for a player.
         *
         * @param player the player to show the GUI to
         */
        void openGui(RPlayer player);
    }
}