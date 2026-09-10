package net.r_developing.rewardsx;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;

@Getter
public final class Plugin extends net.md_5.bungee.api.plugin.Plugin {

    private BungeeRewardsxCore core;

    @Override
    public void onEnable() {
        core = new BungeeRewardsxCore(this);
        core.onEnable();
    }

    @Override
    public void onDisable() {
        if (core != null) core.onDisable();
    }

    public RewardPollingTask getPollingTask(){
        return core.getPollingTask();
    }
}
