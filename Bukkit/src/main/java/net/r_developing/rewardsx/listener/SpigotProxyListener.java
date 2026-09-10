package net.r_developing.rewardsx.listener;

import lombok.Setter;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.gui.RewardsGUI;
import net.r_developing.rewardsx.player.SpigotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class SpigotProxyListener implements PluginMessageListener {
    private final Plugin plugin;
    private final PlatformAdapter adapter;

    @Setter
    private AbstractProxyListener coreListener;
    @Setter
    private RewardsGUI rewardsGUI;

    public SpigotProxyListener(Plugin plugin,
                               AbstractProxyListener coreListener,
                               RewardsGUI rewardsGUI,
                               PlatformAdapter adapter) {
        this.plugin = plugin;
        this.coreListener = coreListener;
        this.rewardsGUI = rewardsGUI;
        this.adapter = adapter;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("rewardsx:command")) {
            return;
        }

        int serverPort = plugin.getServer().getPort();

        adapter.getLogger().debug(
                "[RewardsX] onPluginMessageReceived -> channel=" + channel +
                        " | player=" + player.getName() +
                        " | serverPort=" + serverPort +
                        " | messageLength=" + (message != null ? message.length : 0)
        );

        if (message == null || message.length == 0) {
            adapter.getLogger().debug("[RewardsX] onPluginMessageReceived: message is null or empty, ignoring.");
            return;
        }

        if (coreListener == null) {
            adapter.getLogger().warning("[RewardsX] coreListener is null! Cannot handle plugin message.");
            return;
        }

        coreListener.handlePluginMessage(message, serverPort, rPlayer -> {
            adapter.getLogger().debug(
                    "[RewardsX] handlePluginMessage callback invoked for player=" + rPlayer.getPlayerName()
            );

            if (rewardsGUI == null) {
                adapter.getLogger().warning("[RewardsX] Cannot open GUI: rewardsGUI is null in SpigotProxyListener!");
                return;
            }

            Player bukkitPlayer = (Player) rPlayer.getPlayer();
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                adapter.getLogger().warning("[RewardsX] Cannot open GUI: Bukkit player is null or offline.");
                return;
            }

            // Esecuzione sul thread principale di Bukkit
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    rewardsGUI.open(new SpigotPlayer(bukkitPlayer));
                    adapter.getLogger().debug("[RewardsX] GUI opened successfully for " + bukkitPlayer.getName());
                } catch (Exception e) {
                    adapter.getLogger().warning("[RewardsX] Error opening GUI: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }
}