package net.r_developing.rewardsx.platform;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformCommandExecutor;
import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.util.List;

public class BungeeCommandExecutor implements PlatformCommandExecutor {
    private final Plugin plugin;

    public BungeeCommandExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void dispatchConsoleCommand(String command, RPlayer targetPlayer, String targetName) {
        String finalCmd = command;

        // 1. Rimpiazza %player% con il nome del giocatore (se disponibile)
        if (targetName != null) {
            finalCmd = finalCmd.replace("%player%", targetName);
        }

        // 2. Esegue il comando dalla console di BungeeCord
        try {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(),
                    finalCmd
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute proxy command '" + finalCmd + "': " + e.getMessage());
        }
    }

    @Override
    public boolean execute(RPlayer sender, String[] args) {
        return false;
    }

    @Override
    public boolean executeConsole(String senderName, String[] args) {
        return false;
    }

    @Override
    public List<String> getTabCompletions(String[] args) {
        return null;
    }
}