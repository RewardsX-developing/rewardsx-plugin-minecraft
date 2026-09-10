package net.r_developing.rewardsx.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.player.BungeePlayer;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;

public class Commands extends Command {
    private final CoreCommands coreCommands;
    private final PlatformLogger logger;

    public Commands(String name, CoreCommands coreCommands, PlatformLogger logger) {
        super(name, "rewardsx.use", "rx");
        this.coreCommands = coreCommands;
        this.logger = logger;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer proxiedPlayer) {
            RPlayer rPlayer = new BungeePlayer(proxiedPlayer);
            coreCommands.execute(rPlayer, args);
        } else {
            coreCommands.executeConsole(args);
        }
    }
}