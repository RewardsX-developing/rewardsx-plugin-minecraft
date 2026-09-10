package net.r_developing.rewardsx;

import com.velocitypowered.api.proxy.ProxyServer;
import net.r_developing.rewardsx.api.core.RewardsXCore;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.network.Translator;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.config.VelocityYamlConfig;
import net.r_developing.rewardsx.gui.VelocityDummyGUI;
import net.r_developing.rewardsx.logger.VelocityLogger;
import net.r_developing.rewardsx.platform.VelocityCommandExecutor;
import net.r_developing.rewardsx.platform.VelocityPlatformAdapter;
import net.r_developing.rewardsx.platform.VelocitySchedulerWrapper;
import net.r_developing.rewardsx.platform.VelocityServer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class VelocityRewardsxCore extends RewardsXCore {
    private final Plugin plugin;
    private final ProxyServer proxyServer;
    private final Path dataDirectory;

    // Istanze concrete
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;
    private final RServer server;
    private final PlatformAdapter adapter;
    private final Api api;
    private final AbstractProxyListener coreProxyListener;

    public VelocityRewardsxCore(Plugin plugin, ProxyServer proxyServer, Logger slf4jLogger, Path dataDirectory) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        this.dataDirectory = dataDirectory;

        this.logger = new VelocityLogger(plugin, slf4jLogger, getMainConfigWrapper());
        this.scheduler = new VelocitySchedulerWrapper(plugin, proxyServer);
        this.server = new VelocityServer(proxyServer);
        this.adapter = new VelocityPlatformAdapter(plugin, proxyServer, logger, dataDirectory, buy);
        this.api = new Api();

        this.coreProxyListener = new AbstractProxyListener(getPlatformLogger(), getRServer(), null);
    }

    @Override
    protected File getDataFolder() {
        return plugin.getDataDirectory().toFile();
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
        return new VelocityYamlConfig(dataDirectory, "main.yml");
    }

    @Override
    protected PlatformConfig getMessagesConfigWrapper() {
        return new VelocityYamlConfig(dataDirectory, "messages.yml");
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
        return new VelocityCommandExecutor(proxyServer, getPlatformLogger());
    }

    @Override
    protected PlatformGUI createGui() {
        // Su Velocity non esistono GUI/inventari fisici (dummy GUI)
        return new VelocityDummyGUI();
    }

    @Override
    protected AbstractProxyListener getCoreProxyListener() {
        if (this.buy != null && this.coreProxyListener.getBuy() == null) {
            this.coreProxyListener.setBuy(this.buy);
        }
        return coreProxyListener;
    }

    @Override
    protected RewardPollingTask getPollingTask() {
        return pollingTask;
    }
}