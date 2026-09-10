package net.r_developing.rewardsx.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.player.VelocityPlayer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Optional;
import java.util.UUID;

public class VelocityPluginMessageListener {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("rewardsx:command");

    private final ProxyServer proxyServer;
    private final PlatformLogger logger;
    private final Buy buy;

    public VelocityPluginMessageListener(ProxyServer proxyServer, PlatformLogger logger, Buy buy) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.buy = buy;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Verifica il canale registrato
        if (!CHANNEL.equals(event.getIdentifier())) {
            return;
        }

        // Accetta messaggi solo se provengono da una connessione backend (Spigot -> Velocity)
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        try (ByteArrayInputStream bin = new ByteArrayInputStream(event.getData());
             DataInputStream in = new DataInputStream(bin)) {

            String playerUUIDData = in.readUTF();
            String commandData = in.readUTF();
            String contentData = in.readUTF();

            String playerUUID = playerUUIDData.substring("playerUUID:".length()).trim();
            String command = commandData.substring("command:".length()).trim();
            String content = contentData.substring("content:".length()).trim();

            Optional<Player> velocityPlayer = proxyServer.getPlayer(UUID.fromString(playerUUID));
            if (velocityPlayer.isEmpty()) {
                logger.warning("Player not found on proxy: " + playerUUID);
                return;
            }

            RPlayer rPlayer = new VelocityPlayer(velocityPlayer.get());
            handleVelocityCommand(command, content, rPlayer);

        } catch (Exception e) {
            logger.warning("Error processing plugin message from backend: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleVelocityCommand(String command, String content, RPlayer player) {
        switch (command.toUpperCase()) {
            case "BUY":
                if (buy != null) {
                    buy.send(player, content);
                } else {
                    logger.warning("Buy service is not initialized on Velocity proxy.");
                }
                break;
            default:
                logger.warning("Unknown command received from Spigot: " + command);
                break;
        }
    }
}