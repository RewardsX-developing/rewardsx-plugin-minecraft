package net.r_developing.rewardsx.api.core.network;

import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.rewards.RewardFetcher;

import java.util.Set;

/**
 * Dedicated task for polling pending reward grants from the backend.
 *
 * <p>Extracted from Fetcher to separate concerns - this class handles only the
 * reward polling loop, while Fetcher handles rewards list fetching and update checks.
 * Runs an async repeating task at a configurable interval (default 60 seconds).
 *
 * <p>Each poll iteration fetches pending grants from the backend and passes them
 * to Buy.confirm() for execution. Uses the inFlight set to prevent re-processing
 * of grants that are still being executed.
 *
 * <p>Can be stopped and started independently via the running flag.
 */
public class RewardPollingTask {

    /** Scheduler for async tasks. */
    private final PlatformScheduler scheduler;
    /** Fetcher for pending rewards from the backend. */
    private final RewardFetcher rewardFetcher;
    /** Config file access (for platform ID and secret key). */
    private final Config config;
    /** Handler for reward grant execution. */
    private final Buy buy;
    /** Set of grants currently in flight (format: "userId:transactionId"). */
    private final Set<String> inFlight;

    /**
     * Flag to enable/disable polling.
     *
     * <p>When false, the polling loop exits immediately without doing work.
     * Volatile so the polling thread sees updates from other threads immediately.
     */
    private volatile boolean running = false;

    /**
     * Constructor injection. No logic here, just wiring up dependencies.
     *
     * @param scheduler     async task scheduler
     * @param rewardFetcher backend reward fetcher
     * @param config        config file access
     * @param buy           reward grant handler
     * @param inFlight      set tracking grants currently being processed
     */
    public RewardPollingTask(PlatformScheduler scheduler, RewardFetcher rewardFetcher, Config config, Buy buy, Set<String> inFlight) {
        this.scheduler = scheduler;
        this.rewardFetcher = rewardFetcher;
        this.config = config;
        this.buy = buy;
        this.inFlight = inFlight;
    }

    /**
     * Starts the polling task.
     *
     * <p>Begins the async repeating task that fetches and processes pending rewards.
     * If already running, does nothing (idempotent).
     *
     * <p>Poll interval is read from config (fetch_interval key, default 60 seconds).
     * Initial delay is 20 ticks (1 second) before the first poll.
     *
     * <p>Each poll iteration:
     *   1. Checks the running flag - exits if false
     *   2. Reads the platform secret key from config
     *   3. Fetches pending grants from the backend
     *   4. Passes each grant to Buy.confirm() for execution
     *   5. Removes the grant from inFlight when execution completes
     */
    public void start() {
        // Only start once - ignore subsequent calls.
        if (running) return;
        running = true;

        // Read the poll interval from config (in seconds).
        int intervalSeconds = config.getMainConfig().getInt("fetch_interval", 60);
        long intervalTicks = intervalSeconds * 20L; // Convert to server ticks (20 ticks/sec).

        // Schedule the polling loop to run async.
        scheduler.runAsyncRepeatingTask(() -> {
            // Check the running flag - exit if polling has been stopped.
            if (!running) return;

            // Read credentials from config.
            String secret = config.getMainConfig().getString("platform_key");
            String platformId = config.getMainConfig().getString("platform_id");

            // Skip this poll if the secret key is missing or empty (server not linked).
            if (secret == null || secret.isBlank()) {
                return;
            }

            // Fetch all pending rewards from the backend and process each one.
            rewardFetcher.fetchPendingRewards(platformId, secret, inFlight, (userId, transactionId, username, commands) -> {
                // Protect against the case where Buy is null (defensive programming).
                if (buy != null) {
                    // Execute the grant and remove from inFlight when done.
                    buy.confirm(userId, transactionId, username, commands, () -> inFlight.remove(userId + ":" + transactionId));
                }
            });

        }, 20L, intervalTicks); // Initial delay: 20 ticks (1 sec), repeat every intervalTicks.
    }
}