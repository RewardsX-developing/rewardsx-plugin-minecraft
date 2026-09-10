package net.r_developing.rewardsx.platform;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.platform.PlatformPlugin;

public class VelocityPlatformPlugin implements PlatformPlugin {
    private final Plugin plugin;

    public VelocityPlatformPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return plugin.getPluginContainer()
                .getDescription()
                .getName()
                .orElse("RewardsX");
    }

    @Override
    public String getVersion() {
        return plugin.getPluginContainer()
                .getDescription()
                .getVersion()
                .orElse("1.0.0");
    }

    @Override
    public Object getPlugin() {
        return plugin;
    }

    @Override
    public RewardPollingTask getPollingTask() {
        return plugin.getPollingTask();
    }
}