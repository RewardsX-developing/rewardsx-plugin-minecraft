package net.r_developing.rewardsx.api.core.proxy;

import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ProxySender {
    private final PlatformAdapter plugin;

    public ProxySender(PlatformAdapter plugin) {
        this.plugin = plugin;
    }

    public void sendCommand(RPlayer player, String command, String content) {
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

