package net.R_Developing.rewardsx;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class ProxyListener implements Listener {
    private final Plugin plugin;
    private final Buy buy;

    public ProxyListener(Plugin plugin, Buy buy) {
        this.plugin = plugin;
        this.buy = buy;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if(!event.getTag().equals("rewardsx:command")) return;
        if(!(event.getSender() instanceof Server)) return;

        try(ByteArrayInputStream bin = new ByteArrayInputStream(event.getData());
             DataInputStream in = new DataInputStream(bin)) {

            String playerUUIDData = in.readUTF();
            String commandData = in.readUTF();
            String contentData = in.readUTF();

            String playerUUID = playerUUIDData.substring("playerUUID:".length());
            String command = commandData.substring("command:".length());
            String content = contentData.substring("content:".length());
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(UUID.fromString(playerUUID));

            handleBungeeCommand(command, content, player);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleBungeeCommand(String command, String content, ProxiedPlayer player) {
        switch(command.toUpperCase()) {
            case "BUY":
                buy.send(player, content);
                break;
            default:
                plugin.getLogger().warning("Unknown command received from Spigot: " + command);
                break;
        }
    }
}
