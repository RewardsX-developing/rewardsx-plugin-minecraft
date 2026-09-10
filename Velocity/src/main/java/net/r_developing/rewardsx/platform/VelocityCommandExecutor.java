package net.r_developing.rewardsx.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import net.r_developing.rewardsx.api.core.platform.PlatformCommandExecutor;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import org.slf4j.Logger;

import java.util.List;

public class VelocityCommandExecutor implements PlatformCommandExecutor {
    private final ProxyServer proxyServer;
    private final PlatformLogger logger;

    public VelocityCommandExecutor(ProxyServer proxyServer, PlatformLogger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Override
    public void dispatchConsoleCommand(String command, RPlayer targetPlayer, String targetName) {
        String finalCmd = command;

        // 1. Rimpiazza %player% con il nome del giocatore (se disponibile)
        if (targetName != null) {
            finalCmd = finalCmd.replace("%player%", targetName);
        }

        // 2. Esegue il comando dalla console di Velocity in modo asincrono
        try {
            proxyServer.getCommandManager().executeAsync(
                    proxyServer.getConsoleCommandSource(),
                    finalCmd
            );
        } catch (Exception e) {
            logger.warning("Failed to execute proxy command " + finalCmd + ". " + e.getMessage());
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