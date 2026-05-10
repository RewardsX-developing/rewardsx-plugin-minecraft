package net.R_Developing.rewardsx;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Optional;
import java.util.UUID;

public class ProxyListener {
    private final Main plugin;
    private final Buy buy;
    private final Logger logger;
    private final ChannelIdentifier channelIdentifier;

    public ProxyListener(Main plugin, Buy buy) {
        this.plugin = plugin;
        this.buy = buy;
        this.logger = plugin.getLogger();
        this.channelIdentifier = MinecraftChannelIdentifier.create("rewardsx", "command");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channelIdentifier)) return;
        if (!(event.getSource() instanceof ServerConnection)) return;

        try (ByteArrayInputStream bin = new ByteArrayInputStream(event.getData());
             DataInputStream in = new DataInputStream(bin)) {

            String playerUUIDData = in.readUTF();
            String commandData = in.readUTF();
            String contentData = in.readUTF();

            String playerUUID = playerUUIDData.substring("playerUUID:".length());
            String command = commandData.substring("command:".length());
            String content = contentData.substring("content:".length());

            Optional<Player> playerOpt = plugin.getProxy().getPlayer(UUID.fromString(playerUUID));
            if(playerOpt.isPresent()) {
                handleVelocityCommand(command, content, playerOpt.get());
            } else {
                logger.warn("Received plugin message for unknown player: {}", playerUUID);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleVelocityCommand(String command, String content, Player player) {
        switch(command.toUpperCase()) {
            case "BUY":
                buy.send(player, content);
                break;
            default:
                logger.warn("Unknown command received from backend server: {}", command);
                break;
        }
    }
}
