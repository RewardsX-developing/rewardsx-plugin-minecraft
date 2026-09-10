package net.r_developing.rewardsx;

import net.r_developing.rewardsx.api.core.RewardsXCore;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.network.Translator;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.config.BukkitYamlConfig;
import net.r_developing.rewardsx.gui.RewardsGUI;
import net.r_developing.rewardsx.listener.SpigotProxyListener;
import net.r_developing.rewardsx.logger.BukkitLogger;
import net.r_developing.rewardsx.platform.*;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.io.File;

public class SpigotRewardsxCore extends RewardsXCore {
    private final Plugin plugin;

    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;
    private final RServer server;
    private final PlatformAdapter adapter;
    private final Api api;
    private PlatformGUI gui;
    private final SpigotProxyListener spigotProxyListener;
    private final AbstractProxyListener coreProxyListener;

    public SpigotRewardsxCore(Plugin plugin) {
        this.plugin = plugin;
        this.logger = new BukkitLogger(plugin, getMainConfigWrapper());
        this.scheduler = new BukkitSchedulerWrapper(plugin);
        this.server = new BukkitServer(plugin);
        this.adapter = new BukkitPlatformAdapter(plugin, logger);
        this.api = new Api();
        this.gui = adapter.getGUI();

        if (gui instanceof Listener) {
            Bukkit.getPluginManager().registerEvents((Listener) gui, plugin);
        }

        this.coreProxyListener = new AbstractProxyListener(getPlatformLogger(), getRServer(), null);

        this.spigotProxyListener = new SpigotProxyListener(plugin, coreProxyListener, (RewardsGUI) gui, adapter);

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "rewardsx:command", spigotProxyListener);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "rewardsx:command");
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
        return new BukkitYamlConfig(plugin, "main.yml");
    }

    @Override
    protected PlatformConfig getMessagesConfigWrapper() {
        return new BukkitYamlConfig(plugin, "messages.yml");
    }

    @Override
    protected Translator getTranslator() {
        return new Translator(api);
    }

    @Override
    protected ProxySender getProxySender() {
        return new ProxySender(adapter);
    }

    @Override
    protected PlatformCommandExecutor getCommandExecutor() {
        return new BukkitCommandExecutor(plugin);
    }

    @Override
    protected PlatformGUI createGui() {
        RewardsGUI createdGui = new RewardsGUI(plugin, fetcher, messager, buy);
        this.gui = createdGui;

        Bukkit.getPluginManager().registerEvents(createdGui, plugin);

        this.spigotProxyListener.setRewardsGUI(createdGui);
        return createdGui;
    }

    @Override
    protected AbstractProxyListener getCoreProxyListener() {
        if (this.buy != null) {
            this.coreProxyListener.setBuy(this.buy);
        }
        return coreProxyListener;
    }

    @Override
    protected RewardPollingTask getPollingTask(){
        return pollingTask;
    }
}