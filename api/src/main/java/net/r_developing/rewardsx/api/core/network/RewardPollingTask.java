package net.r_developing.rewardsx.api.core.network;

import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.rewards.RewardFetcher;

import java.util.Set;

public class RewardPollingTask {

    private final PlatformScheduler scheduler;
    private final RewardFetcher rewardFetcher;
    private final Config config;
    private final Buy buy;
    private final Set<String> inFlight;

    // Flag per bloccare l'esecuzione del codice nel task
    private volatile boolean running = false;

    public RewardPollingTask(PlatformScheduler scheduler, RewardFetcher rewardFetcher, Config config, Buy buy, Set<String> inFlight) {
        this.scheduler = scheduler;
        this.rewardFetcher = rewardFetcher;
        this.config = config;
        this.buy = buy;
        this.inFlight = inFlight;
    }

    public void start() {
        if (running) return;
        running = true;

        int intervalSeconds = config.getMainConfig().getInt("fetch_interval", 60);
        long intervalTicks = intervalSeconds * 20L;

        scheduler.runAsyncRepeatingTask(() -> {
            if (!running) return;

            String secret = config.getMainConfig().getString("platform_key");
            String platformId = config.getMainConfig().getString("platform_id");

            if (secret == null || secret.isBlank()) {
                return;
            }

            rewardFetcher.fetchPendingRewards(platformId, secret, inFlight, (userId, transactionId, username, commands) -> {
                if (buy != null) {
                    buy.confirm(userId, transactionId, username, commands, () -> {
                        inFlight.remove(userId + ":" + transactionId);
                    });
                }
            });

        }, 20L, intervalTicks);
    }

    public void stop() {
        running = false;
    }
}