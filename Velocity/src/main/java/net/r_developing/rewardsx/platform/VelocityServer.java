package net.r_developing.rewardsx.platform;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.player.RPlayerOffline;
import net.r_developing.rewardsx.player.VelocityPlayer;

import java.util.Optional;
import java.util.UUID;

public class VelocityServer implements RServer {
    private final ProxyServer proxyServer;
    private final EventManager events;

    public VelocityServer(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        this.events = proxyServer.getEventManager();
    }

    @Override
    public RPlayer getPlayerExact(String name) {
        Optional<Player> player = proxyServer.getPlayer(name);
        return player.map(VelocityPlayer::new).orElse(null);
    }

    @Override
    public RPlayer getPlayer(UUID uuid) {
        Optional<Player> player = proxyServer.getPlayer(uuid);
        return player.map(VelocityPlayer::new).orElse(null);
    }

    @Override
    public RPlayer getPlayer(String name) {
        Optional<Player> player = proxyServer.getPlayer(name);
        return player.map(VelocityPlayer::new).orElse(null);
    }

    @Override
    public RPlayerOffline getOfflinePlayer(String name) {
        return null;
    }

    @Override
    public RPlayerOffline getOfflinePlayer(UUID uuid) {
        return null;
    }

    @Override
    public void runSync(Runnable task) {
        task.run();
    }

    @Override
    public void dispatchConsoleCommand(String cmd) {
        proxyServer.getCommandManager().executeAsync(
                proxyServer.getConsoleCommandSource(),
                cmd
        );
    }

    @Override
    public void registerEvents(Object... listeners) {
        for (Object listener : listeners) {
            events.register(proxyServer.getPluginManager().getPlugin("RewardsX"), listener);  // Velocity accepts raw Object
        }
    }
}