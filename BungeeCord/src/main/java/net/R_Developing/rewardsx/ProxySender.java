package net.R_Developing.rewardsx;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ProxySender {

    public void sendCommand(ProxiedPlayer player, String server, String command, String content) {
        try(ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout)) {

            ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(server);
            if(serverInfo != null) {
                SocketAddress socketAddress = serverInfo.getSocketAddress();
                if(socketAddress instanceof InetSocketAddress) {
                    int port = server.equalsIgnoreCase("all") ? 999999 : ((InetSocketAddress) socketAddress).getPort();

                    out.writeUTF("playerUUID:" + player.getUniqueId().toString());
                    out.writeUTF("port:" + port);
                    out.writeUTF("command:" + command);
                    out.writeUTF("content:" + content);

                    player.getServer().getInfo().sendData("rewardsx:command", bout.toByteArray(), false);
                }
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}