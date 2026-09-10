package net.r_developing.rewardsx.platform;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.player.RPlayerOffline;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.player.SpigotPlayer;
import net.r_developing.rewardsx.player.SpigotPlayerOffline;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitServer implements RServer {

    private final Plugin plugin;

    public BukkitServer(Plugin plugin){
        this.plugin = plugin;
    }

    @Override
    public RPlayer getPlayerExact(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return (player != null) ? new SpigotPlayer(player) : null;
    }

    @Override
    public RPlayer getPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return (player != null) ? new SpigotPlayer(player) : null;
    }

    @Override
    public RPlayer getPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return (player != null) ? new SpigotPlayer(player) : null;
    }

    @Override
    public RPlayerOffline getOfflinePlayer(String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        return (player != null && player.hasPlayedBefore()) ? new SpigotPlayerOffline(player) : null;
    }

    @Override
    public RPlayerOffline getOfflinePlayer(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return (player != null && player.hasPlayedBefore()) ? new SpigotPlayerOffline(player) : null;
    }

    @Override
    public void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugins()[0], task);
        }
    }

    @Override
    public void dispatchConsoleCommand(String cmd) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    @Override
    public void registerEvents(Object... listeners) {
        for (Object listener : listeners) {
            Bukkit.getPluginManager().registerEvents((org.bukkit.event.Listener) listener, Bukkit.getPluginManager().getPlugins()[0]);
        }
    }
}