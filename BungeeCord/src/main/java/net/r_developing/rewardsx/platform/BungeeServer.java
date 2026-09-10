package net.r_developing.rewardsx.platform;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.player.RPlayerOffline;
import net.r_developing.rewardsx.player.BungeePlayer;

import java.util.UUID;

public class BungeeServer implements RServer {
    private final ProxyServer proxy;

    public BungeeServer(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public RPlayer getPlayerExact(String name) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        return player != null ? new BungeePlayer(player) : null;
    }

    @Override
    public RPlayer getPlayer(UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player != null ? new BungeePlayer(player) : null;
    }

    @Override
    public RPlayer getPlayer(String name) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        return player != null ? new BungeePlayer(player) : null;
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
        proxy.getScheduler().runAsync(proxy.getPluginManager().getPlugin("RewardsX"), task);
        // se usi un altro nome di plugin, cambialo di conseguenza
    }

    @Override
    public void dispatchConsoleCommand(String cmd) {
        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), cmd);
    }

    @Override
    public void registerEvents(Object... listeners) {
        for (Object obj : listeners) {
            if (obj instanceof Listener) {
                Listener l = (Listener) obj;
                proxy.getPluginManager().registerListener(
                        proxy.getPluginManager().getPlugin("RewardsX_Bungee"),
                        l
                );
            } else {
                proxy.getLogger().warning("[RewardsX] Tried to register non-listener object: " + obj);
            }
        }
    }

    // Su Bungee non ha senso Offline; puoi tornare null o implementare un wrapper dummy
}