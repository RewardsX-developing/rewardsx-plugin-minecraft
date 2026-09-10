package net.r_developing.rewardsx.api.core.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.Platform;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.rewards.RewardFetcher;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Polls the RewardsX backend for updates and manages in-flight reward grants.
 *
 * <p>Runs an async repeating task at a fixed interval (default once per minute) that:
 *   - Fetches the rewards list from the backend
 *   - Fetches pending reward grants and passes them to Buy.confirm()
 *   - Checks for plugin updates from SpigotMC
 *
 * <p>Also provides one-shot API methods for account linking and server authentication.
 *
 * <p>The inFlight set tracks which reward grants are currently being processed,
 * to avoid duplicate processing if a grant takes longer than the poll interval.
 */
public class Fetcher {
    /** Console logger. */
    private final PlatformLogger logger;
    /** Scheduler for async tasks. */
    private final PlatformScheduler scheduler;
    /** HTTP client for backend API calls. */
    private final Api api;
    /** Polling interval in server ticks (20 ticks = 1 second). */
    private long intervalTicks;
    /** Config file access (for the platform secret key). */
    private final Config config;
    /** Platform detection and ID access. */
    private final Platform platform;

    /** Handler for reward grant execution. Injected after construction. */
    @Setter
    private Buy buy;

    /**
     * The rewards list fetched from the backend.
     * Volatile so reads from the main thread see the latest write from the polling thread.
     */
    private volatile List<Map<String, String>> rewardsList = Collections.emptyList();

    /**
     * Player bits balances: userId -> bit count.
     * Updated by RewardFetcher during the polling interval.
     */
    public final Map<String, Integer> bitsList = new HashMap<>();

    /**
     * Player purchase counts: userId -> number of purchases.
     * Updated during polling; used by getBuys() to show player stats.
     */
    public final Map<String, Integer> buysList = new HashMap<>();

    /** Latest plugin version fetched from SpigotMC. */
    public String latestVersion = "";

    /** Fetcher for pending rewards from the backend. Injected after construction. */
    @Setter
    @Getter
    public RewardFetcher rewardFetcher;

    /**
     * Tracks reward grants currently being processed.
     * Format: "userId:rewardId"
     *
     * <p>Used to prevent duplicate processing if a grant stalls. When the grant
     * completes, the key is removed from inFlight. RewardFetcher checks this set
     * before returning pending grants, skipping any still in flight.
     */
    @Getter
    public final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    /**
     * Constructor injection. Calculates the polling interval from seconds to ticks.
     *
     * @param adapter         platform-specific utilities
     * @param scheduler       async task scheduler
     * @param api             HTTP client for backend
     * @param config          config file access
     * @param platform        platform detection
     * @param buy             reward grant handler
     * @param intervalSeconds polling interval in seconds (e.g. 60 for once per minute)
     */
    public Fetcher(PlatformAdapter adapter, PlatformScheduler scheduler, Api api, Config config, Platform platform, Buy buy, int intervalSeconds) {
        this.logger = adapter.getLogger();
        this.scheduler = scheduler;
        this.api = api;
        this.intervalTicks = intervalSeconds * 20L; // Convert seconds to server ticks (20 ticks/sec).
        this.config = config;
        this.platform = platform;
        this.buy = buy;
    }

    /**
     * Starts the polling task.
     *
     * <p>Schedules an async repeating task that runs every intervalTicks ticks.
     * Each poll iteration:
     *   1. Fetches the rewards list from the backend
     *   2. Fetches pending reward grants and processes them
     *   3. Checks for plugin updates from SpigotMC
     *
     * <p>Errors in polling are logged but do not stop the poll loop.
     */
    public void start() {
        scheduler.runAsyncRepeatingTask(() -> {

            // --- Fetch the rewards list ---
            Map<String, Object> rewardsPayload = new HashMap<>();
            rewardsPayload.put("platform", platform.getId());

            api.send("GET", "rewards", rewardsPayload, result -> {
                System.out.println(result);
                if (result != null) {
                    // The backend returns a "rewards" field containing the list.
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> list = (List<Map<String, String>>) result.get("rewards");
                    System.out.println(list);
                    rewardsList = list != null ? list : Collections.emptyList();
                    System.out.println(rewardsList);
                }
            });

            // --- Fetch and process pending reward grants ---
            String secret = config.getMainConfig().getString("platform_key");

            rewardFetcher.fetchPendingRewards(platform.getId(), secret, inFlight, (userId, rewardId, username, commands) -> {
                // A grant is pending - pass it to Buy to execute and remove from inFlight when done.
                buy.confirm(userId, rewardId, username, commands, () -> inFlight.remove(userId + ":" + rewardId));
            });

            // --- Check for plugin updates ---
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spiget.org/v2/resources/121867/versions/latest"))
                    .GET()
                    .build();

            try (InputStream in = client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
                Scanner scanner = new Scanner(in);

                String json = scanner.useDelimiter("\\A").next();
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String version = obj.get("name").getAsString();
                latestVersion = version.split(" ")[0]; // Extract just the version number.

            } catch (Exception e) {
                if (platform.isDebug()) {
                    logger.warning("Failed to check for updates: " + e.getMessage());
                }
            }
        }, 0L, intervalTicks); // Start immediately (0L delay), repeat every intervalTicks.
    }

    /**
     * Authenticates a game server via its secret key.
     *
     * <p>Called when a server owner runs /rewardsx secret <key>. The backend
     * validates the key and returns the platform details if valid.
     *
     * <p>Async - returns a CompletableFuture that completes with the backend response
     * (which contains platform_id, name, etc.) or null if authentication failed.
     *
     * @param secretKey the server's secret key from the web dashboard
     * @return a future that completes with the backend response map (or null on failure)
     */
    public CompletableFuture<Map<String, Object>> authenticateServer(String secretKey) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("secret", secretKey);
        api.send("POST", "server/auth", payload, future::complete);
        return future;
    }

    /**
     * Returns the rewards list, with field names mapped to user-friendly keys.
     *
     * <p>The backend returns rewards with keys like "idReward", "nameReward", etc.
     * This method transforms them to "id", "name", "cost", "description" for cleaner
     * usage throughout the codebase.
     *
     * <p>Returns an empty list if rewardsList is null or empty.
     *
     * @return the filtered rewards list
     */
    public List<Map<String, String>> getRewardsList() {
        if (rewardsList == null) return Collections.emptyList();

        return rewardsList.stream()
                .map(original -> {
                    Map<String, String> filtered = new HashMap<>();
                    filtered.put("id", String.valueOf(original.get("idReward")));
                    filtered.put("name", String.valueOf(original.get("nameReward")));
                    filtered.put("cost", String.valueOf(original.get("costReward")));

                    Object desc = original.get("descriptionReward");
                    filtered.put("description", desc != null ? String.valueOf(desc) : "");

                    return filtered;
                })
                .collect(Collectors.toList());
    }

    public void reload(){
        scheduler.cancelAll();
        config.getMainConfig().reload();
        this.intervalTicks = config.getMainConfig().getInt("fetch_interval", 60);
        start();
    }
}