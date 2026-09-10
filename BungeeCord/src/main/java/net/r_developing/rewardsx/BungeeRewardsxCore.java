package net.r_developing.rewardsx;

import net.md_5.bungee.api.ProxyServer;
import net.r_developing.rewardsx.api.core.RewardsXCore;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.network.Translator;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.config.BungeeYamlConfig;
import net.r_developing.rewardsx.logger.BungeeLogger;
import net.r_developing.rewardsx.platform.BungeeCommandExecutor;
import net.r_developing.rewardsx.platform.BungeePlatformAdapter;
import net.r_developing.rewardsx.platform.BungeeSchedulerWrapper;
import net.r_developing.rewardsx.platform.BungeeServer;
import net.r_developing.rewardsx.gui.BungeeDummyGUI;

import java.io.File;

public class BungeeRewardsxCore extends RewardsXCore {
    private final Plugin plugin;

    // Istanze concrete
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;
    private final RServer server;
    private final PlatformAdapter adapter;
    private final Api api;
    private final AbstractProxyListener coreProxyListener;

    public BungeeRewardsxCore(Plugin plugin) {
        this.plugin = plugin;
        this.logger = new BungeeLogger(plugin, getMainConfigWrapper());
        this.scheduler = new BungeeSchedulerWrapper(plugin);
        this.server = new BungeeServer(ProxyServer.getInstance());
        this.adapter = new BungeePlatformAdapter(plugin, logger, buy);
        this.api = new Api();

        this.coreProxyListener = new AbstractProxyListener(getPlatformLogger(), getRServer(), null);
    }

    @Override
    protected File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    protected PlatformLogger getPlatformLogger() {
        return logger;
    }

    @Override
    protected PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    protected RServer getRServer() {
        return server;
    }

    @Override
    protected PlatformAdapter getAdapter() {
        return adapter;
    }

    @Override
    protected Api getApi() {
        return api;
    }

    @Override
    protected PlatformConfig getMainConfigWrapper() {
        return new BungeeYamlConfig(plugin, "main.yml");
    }

    @Override
    protected PlatformConfig getMessagesConfigWrapper() {
        return new BungeeYamlConfig(plugin, "messages.yml");
    }

    @Override
    protected Translator getTranslator() {
        return new Translator(api);
    }

    @Override
    protected ProxySender getProxySender() {
        // Implementazione Bungee che manda plugin messages ai server Spigot
        return new ProxySender(adapter);
    }

    @Override
    protected PlatformCommandExecutor getCommandExecutor() {
        return new BungeeCommandExecutor(plugin);
    }

    @Override
    protected PlatformGUI createGui() {
        // Su Bungee non esistono inventory GUI: ritorna una GUI dummy
        return new BungeeDummyGUI();
    }

    @Override
    protected AbstractProxyListener getCoreProxyListener() {
        return coreProxyListener;
    }

    @Override
    protected RewardPollingTask getPollingTask() {
        return pollingTask;
    }
}