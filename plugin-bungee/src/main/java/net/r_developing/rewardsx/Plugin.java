package net.r_developing.rewardsx;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.platform.PlatformCommandExecutor;
import net.r_developing.rewardsx.listener.BungeeJoinListener;

@Getter
public final class Plugin extends net.md_5.bungee.api.plugin.Plugin {

    private BungeeRewardsxCore core;

    @Override
    public void onEnable() {
        this.core = new BungeeRewardsxCore(this);
        this.core.onEnable();
        this.core.getRServer().registerEvents(new BungeeJoinListener(this.core.getQueueManager()));

    }

    @Override
    public void onDisable() {
        if (core != null) core.onDisable();
    }

    public RewardPollingTask getPollingTask(){
        return core.getPollingTask();
    }

    public PlatformCommandExecutor getPlatformCommandExecutor(){ return core.getCommandExecutor(); }
}
