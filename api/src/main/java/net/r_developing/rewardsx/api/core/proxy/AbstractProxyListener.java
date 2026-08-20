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
 * Gestisce i messaggi in ingresso inviati tramite i Plugin Channels.
 * È totalmente indipendente dalla piattaforma.
 */
public class AbstractProxyListener {

    private final PlatformLogger logger;
    private final RServer server;
    @Setter
    @Getter
    private Buy buy;

    public AbstractProxyListener(PlatformLogger logger, RServer server, Buy buy) {
        this.logger = logger;
        this.server = server;
        this.buy = buy;
    }

    /**
     * Da invocare dal listener nativo della piattaforma quando arriva un messaggio su "rewardsx:command".
     *
     * @param message L'array di byte ricevuto
     * @param serverPort La porta del server corrente su cui sta girando il plugin
     * @param guiOpener Un'azione callback per aprire la GUI (poiché le GUI sono specifiche per piattaforma)
     */
    public void handlePluginMessage(byte[] message, int serverPort, GuiOpener guiOpener) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            UUID playerUUID = null;
            int port = 0;
            String command = null;
            String content = ""; // Inizializzato a stringa vuota per evitare null check failure

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

            logger.debug("[AbstractProxyListener] Decoded packet -> playerUUID: " + playerUUID
                    + " | portInPacket: " + port
                    + " | currentServerPort: " + serverPort
                    + " | command: " + command
                    + " | content: " + content);

            if (playerUUID == null) {
                logger.warning("Received message without playerUUID");
                return;
            }

            RPlayer targetPlayer = server.getPlayer(playerUUID);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                logger.warning("Target player not online: " + playerUUID);
                return;
            }

            if (command == null) {
                logger.warning("Received plugin message without command");
                return;
            }

            // Se la porta è specificata (diversa da 0 e non broadcast 999999), controlla la corrispondenza
            if (port != 0 && port != 999999 && port != serverPort) {
                logger.debug("[AbstractProxyListener] Ignored message: port mismatch (" + port + " != " + serverPort + ")");
                return;
            }

            switch (command) {
                case "OPENGUI":
                    logger.debug("[AbstractProxyListener] Opening GUI for target: " + targetPlayer.getPlayerName());
                    guiOpener.openGui(targetPlayer);
                    break;
                case "RUN":
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
     * Callback per demandare l'apertura della GUI alla piattaforma.
     */
    public interface GuiOpener {
        void openGui(RPlayer player);
    }
}