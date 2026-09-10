package net.r_developing.rewardsx.player;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.platform.BungeePlatformAdapter;

import java.util.UUID;

public class BungeePlayer implements RPlayer {
    private final ProxiedPlayer player;

    public BungeePlayer(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public Object getPlayer() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getPlayerName() {
        return player.getName();
    }

    @Override
    public void sendMessage(String msg) {
        player.sendMessage(msg);
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // Bungee non ha API title standard; se vuoi, usa una libreria esterna o lascia vuoto
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        // idem
    }

    @Override
    public void performCommand(String cmd) {
        player.chat("/" + cmd);
    }

    @Override
    public void closeInventory() {
        // Non esistono inventory su Bungee
    }

    @Override
    public void openInventory(Object inv) {
        // Non supportato su Bungee
    }

    @Override
    public boolean hasPermission(String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public boolean isOnline() {
        return player.isConnected();
    }

    @Override
    public void sendPluginMessage(PlatformAdapter plugin, String channel, byte[] data) {
        // Invia il packet al server su cui si trova il player
        Server server = player.getServer();
        if (server == null) {
            plugin.getLogger().debug("[RewardsX] sendPluginMessage: player has no server (lobby?)");
            return;
        }

        plugin.getLogger().debug(
                "[RewardsX] sendPluginMessage -> channel=" + channel +
                        " | server=" + server.getInfo().getName() +
                        " | player=" + player.getName() +
                        " | dataLength=" + (data != null ? data.length : 0)
        );

        if (data == null || data.length == 0) {
            plugin.getLogger().debug("[RewardsX] sendPluginMessage: data is null or empty, skipping.");
            return;
        }

        // Usa sendData (Bungee 1.20+) o sendPluginMessage se usi versioni più vecchie
        server.sendData(channel, data);

        plugin.getLogger().debug(
                "[RewardsX] sendPluginMessage: packet sent to server=" + server.getInfo().getName()
        );
    }

    @Override
    public void sendMessageWithUrl(String message, String url) {
        player.sendMessage(message + " -> " + url);
    }
}