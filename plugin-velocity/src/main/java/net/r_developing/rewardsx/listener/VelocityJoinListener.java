package net.r_developing.rewardsx.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.r_developing.rewardsx.api.core.rewards.RewardQueueManager;

public class VelocityJoinListener {

    private final RewardQueueManager queueManager;

    public VelocityJoinListener(RewardQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        queueManager.onPlayerJoin(event.getPlayer().getUsername());
    }
}