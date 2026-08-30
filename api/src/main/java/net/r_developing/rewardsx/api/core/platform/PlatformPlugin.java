package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.network.RewardPollingTask;

/**
 * Astrae le informazioni base del plugin che variano per piattaforma.
 */
public interface PlatformPlugin {
    String getName();

    String getVersion();

    Object getPlugin();

    RewardPollingTask getPollingTask();
}