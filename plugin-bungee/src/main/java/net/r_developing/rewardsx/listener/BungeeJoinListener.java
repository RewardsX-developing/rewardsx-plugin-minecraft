package net.r_developing.rewardsx.listener;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.r_developing.rewardsx.api.core.rewards.RewardQueueManager;

public class BungeeJoinListener implements Listener {

    private final RewardQueueManager queueManager;

    public BungeeJoinListener(RewardQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        queueManager.onPlayerJoin(event.getPlayer().getName());
    }
}