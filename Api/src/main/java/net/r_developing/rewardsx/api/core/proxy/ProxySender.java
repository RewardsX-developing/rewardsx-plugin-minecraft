package net.r_developing.rewardsx.api.core.proxy;

import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Sends plugin channel messages from a backend server to the proxy.
 *
 * <p>Used on backend servers (Spigot, etc.) to communicate with the proxy
 * (BungeeCord, Velocity). Sends serialized messages on the "rewardsx:command"
 * channel for actions that need to be handled by the proxy or another backend.
 *
 * <p>Complements AbstractProxyListener, which handles incoming messages from the proxy.
 * Together they enable bidirectional communication across the proxy network.
 *
 * <p>Common use cases:
 *   - Open GUI on proxy when backend server has no GUI implementation
 *   - Forward commands to another backend server
 *   - Request player data from proxy
 *
 * <p>Message format (UTF strings):
 *   playerUUID:{uuid}
 *   command:{OPENGUI|RUN|...}
 *   content:{optional-payload}
 */
public class ProxySender {
    /** Platform adapter - used to access the plugin instance for sendPluginMessage(). */
    private final PlatformAdapter plugin;

    /**
     * Constructor injection.
     *
     * @param plugin the platform adapter (wraps the native plugin instance)
     */
    public ProxySender(PlatformAdapter plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a plugin message to the proxy on the "rewardsx:command" channel.
     *
     * <p>Serializes the player UUID, command, and content into a byte packet,
     * then sends it to the player's connected proxy server. The player is used
     * as the routing target - the proxy knows which server they're on and
     * delivers the message accordingly.
     *
     * <p>This is a one-way send - the caller does not wait for a response.
     * If a response is needed, the proxy must send a message back on the same
     * channel (handled by AbstractProxyListener).
     *
     * <p>Common usage:
     *   - sendCommand(player, "OPENGUI", "") - open the GUI on proxy
     *   - sendCommand(player, "RUN", "give [player] diamond") - execute command
     *
     * <p>Packet format:
     *   playerUUID:{uuid}
     *   command:{command}
     *   content:{content}
     *
     * @param player  the player to use as the routing target
     *                (the proxy knows which server they're on)
     * @param command the action to perform (e.g. "OPENGUI", "RUN")
     * @param content optional payload for the command
     *                (e.g. command string for "RUN", empty for "OPENGUI")
     */
    public void sendCommand(RPlayer player, String command, String content) {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bout)) {

            // Write the player UUID so the proxy knows who this is for.
            out.writeUTF("playerUUID:" + player.getUniqueId());
            // Write the command (what action the proxy should take).
            out.writeUTF("command:" + command);
            // Write the content (optional payload - command string, GUI params, etc.).
            out.writeUTF("content:" + content);

            // Send the serialized packet to the player via plugin messaging.
            // The player's connection to the proxy is used for routing.
            player.sendPluginMessage(plugin, "rewardsx:command", bout.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}