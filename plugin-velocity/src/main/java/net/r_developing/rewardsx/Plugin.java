package net.r_developing.rewardsx;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import org.slf4j.Logger;

import java.nio.file.Path;

@com.velocitypowered.api.plugin.Plugin(
        id = "rewardsx",
        name = "RewardsX",
        version = "2026.08.30",
        description = "RewardsX Minecraft plugin for Velocity",
        authors = {"R_Developing"}
)
public final class Plugin {

    @Getter
    private final ProxyServer server;
    @Getter
    private final PluginContainer pluginContainer;
    @Getter
    private final Logger logger;
    @Getter
    private final Path dataDirectory;

    private VelocityRewardsxCore core;

    @Inject
    public Plugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginContainer pluginContainer) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.core = new VelocityRewardsxCore(this, server, logger, dataDirectory);
        this.core.onEnable();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.core != null) {
            this.core.onDisable();
        }
    }

    public RewardPollingTask getPollingTask(){
        return core.getPollingTask();
    }

}