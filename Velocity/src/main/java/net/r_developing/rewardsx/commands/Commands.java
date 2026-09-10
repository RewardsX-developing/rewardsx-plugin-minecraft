package net.r_developing.rewardsx.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.player.VelocityPlayer;

import java.util.List;

public class Commands implements SimpleCommand {
    private final CoreCommands coreCommands;
    private final PlatformLogger logger;
    private final ProxyServer proxyServer;

    public Commands(CoreCommands coreCommands, PlatformLogger logger, ProxyServer proxyServer) {
        this.coreCommands = coreCommands;
        this.logger = logger;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source instanceof Player player) {
            RPlayer rPlayer = new VelocityPlayer(player);
            coreCommands.execute(rPlayer, args);
        } else {
            coreCommands.executeConsole(args);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (coreCommands != null) {
            return coreCommands.getTabCompletions(invocation.arguments());
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("rewardsx.use");
    }
}