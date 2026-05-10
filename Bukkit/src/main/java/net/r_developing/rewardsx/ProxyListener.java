package net.r_developing.rewardsx;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.UUID;

public class ProxyListener implements PluginMessageListener {
    private final Plugin plugin;
    private final RewardsGUI rewardsGUI;
    private final Buy buy;

    public ProxyListener(Plugin plugin, RewardsGUI rewardsGUI, Buy buy) {
        this.plugin = plugin;
        this.rewardsGUI = rewardsGUI;
        this.buy = buy;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if(!channel.equals("rewardsx:command")) return;

        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            UUID playerUUID = null;
            int port = 0;
            String command = null;
            String content = null;

            while(in.available() > 0) {
                String msg = in.readUTF();
                if(msg.startsWith("playerUUID:")) {
                    playerUUID = UUID.fromString(msg.substring("playerUUID:".length()).trim());
                }
                if(msg.startsWith("port:")) {
                    port = Integer.parseInt(msg.substring("port:".length()).trim());
                }
                if(msg.startsWith("command:")) {
                    command = msg.substring("command:".length()).trim().toUpperCase();
                }
                if(msg.startsWith("content:")) {
                    content = msg.substring("content:".length()).trim();
                }
            }

            if(playerUUID == null) {
                plugin.getLogger().warning("Received message without playerUUID");
                return;
            }

            Player targetPlayer = Bukkit.getPlayer(playerUUID);
            if(targetPlayer == null || !targetPlayer.isOnline()) {
                plugin.getLogger().warning("Target player not online: " + playerUUID);
                return;
            }

            if(port != 0 && port == detectServerPort()) {
                if(command != null && content != null) {
                    switch(command) {
                        case "OPENGUI":
                            rewardsGUI.open(targetPlayer);
                            break;
                        case "RUN":
                            buy.executeCommand(targetPlayer, content);
                            break;
                        default:
                            plugin.getLogger().warning("Not valid command in ProxyListener");
                            break;
                    }
                }
            } else if(port == 999999) buy.executeCommand(targetPlayer, content);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int detectServerPort() {
        return Bukkit.getServer().getPort();
    }
}
