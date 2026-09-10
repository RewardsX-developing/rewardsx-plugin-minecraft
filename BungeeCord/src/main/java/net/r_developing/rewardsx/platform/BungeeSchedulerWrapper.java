package net.r_developing.rewardsx.platform;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;

import java.util.concurrent.TimeUnit;

public class BungeeSchedulerWrapper implements PlatformScheduler {
    private final Plugin plugin;

    public BungeeSchedulerWrapper(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsyncRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        long delayMillis = delayTicks * 50L;
        long periodMillis = periodTicks * 50L;
        ProxyServer.getInstance().getScheduler().schedule(
                plugin,
                task,
                delayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void runAsync(Runnable task) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        // Bungee non ha il concetto di main thread del mondo come Bukkit,
        // ma puoi comunque pianificare l’esecuzione immediata
        ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        long delayMillis = delayTicks * 50L;
        ProxyServer.getInstance().getScheduler().schedule(
                plugin,
                task,
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void cancelAll() {
        ProxyServer.getInstance().getScheduler().cancel(plugin);
    }
}