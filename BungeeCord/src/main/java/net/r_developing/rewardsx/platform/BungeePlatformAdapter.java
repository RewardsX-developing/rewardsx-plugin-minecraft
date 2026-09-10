package net.r_developing.rewardsx.platform;

import net.md_5.bungee.api.ProxyServer;
import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.PlatformPlugin;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.commands.Commands;
import net.r_developing.rewardsx.listener.BungeePluginMessageListener;

import java.nio.file.Path;

public class BungeePlatformAdapter implements PlatformAdapter {
    private final Plugin plugin;
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;

    private PlatformGUI gui;
    private CoreCommands coreCommands;
    private Buy buy;
    private AbstractProxyListener proxyListener;

    public BungeePlatformAdapter(Plugin plugin, PlatformLogger logger, Buy buy) {
        this.plugin = plugin;
        this.logger = logger;
        this.buy = buy;
        this.scheduler = new BungeeSchedulerWrapper(plugin);
    }

    @Override
    public void cancelAllTasks() {
        ProxyServer.getInstance().getScheduler().cancel(plugin);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        scheduler.runTaskLater(task, delayTicks);
    }

    @Override
    public PlatformPlugin getInstance() {
        return new BungeePlatformPlugin(plugin);
    }

    @Override
    public PlatformGUI getGUI() {
        return gui;
    }

    @Override
    public PlatformLogger getLogger() {
        return logger;
    }

    @Override
    public void setupDependencies(PlatformGUI gui, CoreCommands coreCommands, AbstractProxyListener proxyListener) {
        this.gui = gui;
        this.coreCommands = coreCommands;
        this.proxyListener = proxyListener;
    }

    @Override
    public void registerCommandsAndEvents() {
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin,
                new Commands("rewardsx", coreCommands, logger)
        );
        ProxyServer.getInstance().getPluginManager().registerListener(plugin,
                new BungeePluginMessageListener(logger, buy)
        );
    }

    @Override
    public Type type() {
        return Type.BUNGEECORD;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void error(String msg) {
        logger.warning("[ERROR] " + msg);
    }

    @Override
    public void debug(String msg) {
        logger.info("[DEBUG] " + msg);
    }

    @Override
    public void warning(String msg) {
        logger.warning(msg);
    }

    @Override
    public void severe(String msg) {
        logger.warning("[SEVERE] " + msg);
    }

    @Override
    public String getName() {
        return plugin.getDescription().getName();
    }

    @Override
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isDbEnabled() {
        return false;
    }

    @Override
    public Path getDataPath() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Object pluginInstance() {
        return plugin;
    }
}