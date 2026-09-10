package net.r_developing.rewardsx.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.player.BungeePlayer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class BungeePluginMessageListener implements Listener {
    private final PlatformLogger logger;
    private final Buy buy;

    public BungeePluginMessageListener(PlatformLogger logger, Buy buy) {
        this.logger = logger;
        this.buy = buy;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!"rewardsx:command".equals(event.getTag())) return;
        if (!(event.getSender() instanceof Server)) return;

        try (ByteArrayInputStream bin = new ByteArrayInputStream(event.getData());
             DataInputStream in = new DataInputStream(bin)) {

            String playerUUIDData = in.readUTF();
            String commandData = in.readUTF();
            String contentData = in.readUTF();

            String playerUUID = playerUUIDData.substring("playerUUID:".length());
            String command = commandData.substring("command:".length());
            String content = contentData.substring("content:".length());

            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(playerUUID));
            if (proxiedPlayer == null) {
                logger.warning("Player not found on proxy: " + playerUUID);
                return;
            }

            RPlayer rPlayer = new BungeePlayer(proxiedPlayer);
            handleBungeeCommand(command, content, rPlayer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleBungeeCommand(String command, String content, RPlayer player) {
        switch (command.toUpperCase()) {
            case "BUY":
                buy.send(player, content);
                break;
            default:
                logger.warning("Unknown command received from Spigot: " + command);
                break;
        }
    }
}