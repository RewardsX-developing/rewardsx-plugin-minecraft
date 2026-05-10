package net.R_Developing.rewardsx;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

public class ProxySender {
    private final ProxyServer proxy;
    private final ChannelIdentifier channelIdentifier;

    public ProxySender(Main plugin) {
        this.proxy = plugin.getProxy();
        this.channelIdentifier = MinecraftChannelIdentifier.create("rewardsx", "command");
    }

    public void sendCommand(Player player, String serverName, String command, String content) {
        try(ByteArrayOutputStream bout = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bout)) {

            Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
            if(serverOpt.isPresent()) {
                RegisteredServer registeredServer = serverOpt.get();
                InetSocketAddress socketAddress = registeredServer.getServerInfo().getAddress();

                if(socketAddress != null) {
                    int port = serverName.equalsIgnoreCase("all") ? 999999 : socketAddress.getPort();

                    out.writeUTF("playerUUID:" + player.getUniqueId().toString());
                    out.writeUTF("port:" + port);
                    out.writeUTF("command:" + command);
                    out.writeUTF("content:" + content);

                    player.getCurrentServer().ifPresent(serverConnection ->
                        serverConnection.sendPluginMessage(channelIdentifier, bout.toByteArray()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
