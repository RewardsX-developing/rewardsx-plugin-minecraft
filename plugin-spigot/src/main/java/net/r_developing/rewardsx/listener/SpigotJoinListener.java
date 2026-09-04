package net.r_developing.rewardsx.listener;

import net.r_developing.rewardsx.api.core.rewards.RewardQueueManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SpigotJoinListener implements Listener {

    private final RewardQueueManager queueManager;

    public SpigotJoinListener(RewardQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        queueManager.onPlayerJoin(event.getPlayer().getName());
    }
}