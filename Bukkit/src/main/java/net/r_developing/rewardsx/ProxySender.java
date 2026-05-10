package net.r_developing.rewardsx;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ProxySender {
    private final Plugin plugin;

    public ProxySender(Plugin plugin) {
        this.plugin = plugin;
    }

    public void sendCommand(Player player, String command, String content) {
        try(ByteArrayOutputStream bout = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bout)) {

            out.writeUTF("playerUUID:" + player.getUniqueId());
            out.writeUTF("command:" + command);
            out.writeUTF("content:" + content);

            player.sendPluginMessage(plugin, "rewardsx:command", bout.toByteArray());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}

