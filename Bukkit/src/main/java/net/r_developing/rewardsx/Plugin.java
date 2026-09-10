package net.r_developing.rewardsx;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.RewardsXCore;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Plugin extends JavaPlugin {

    private SpigotRewardsxCore core;

    @Override
    public void onEnable() {
        this.core = new SpigotRewardsxCore(this);
        this.core.onEnable();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.onDisable();
        }
    }


    public RewardPollingTask getPollingTask(){
        return core.getPollingTask();
    }

    public PlatformLogger getPlatformLogger(){
        return core.getPlatformLogger();
    }
}