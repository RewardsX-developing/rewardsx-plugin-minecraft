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
import net.r_developing.rewardsx.api.core.rewards.RewardQueueManager;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Fetcher {
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;
    private final Api api;
    private long intervalTicks;
    private final Config config;
    private final Platform platform;
    @Getter
    private final RewardQueueManager queueManager;

    @Setter
    private Buy buy;
    private volatile List<Map<String, Object>> rewardsList = Collections.emptyList();

    public final Map<String, Integer> bitsList = new HashMap<>();
    public final Map<String, Integer> buysList = new HashMap<>();
    public String latestVersion = "";

    @Setter
    @Getter
    public RewardFetcher rewardFetcher;

    @Getter
    public final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public Fetcher(PlatformAdapter adapter, PlatformScheduler scheduler, Api api, Config config, Platform platform,
                   Buy buy, int intervalSeconds, RewardQueueManager queueManager) {
        this.logger = adapter.getLogger();
        this.scheduler = scheduler;
        this.api = api;
        this.intervalTicks = intervalSeconds * 20L;
        this.config = config;
        this.platform = platform;
        this.buy = buy;
        this.queueManager = queueManager;
    }

    public void start() {
        scheduler.runAsyncRepeatingTask(() -> {

            // --- Fetch the rewards list ---
            Map<String, Object> rewardsPayload = new HashMap<>();
            rewardsPayload.put("platform", platform.getId());

            api.send("GET", "rewards", rewardsPayload, result -> {
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("rewards");
                    rewardsList = list != null ? list : Collections.emptyList();
                }
            });

            // --- Fetch and process pending reward grants ---
            String secret = config.getMainConfig().getString("platform_key");

            rewardFetcher.fetchPendingRewards(platform.getId(), secret, inFlight, (userId, rewardId, username, commands) -> {
                queueManager.processReward(userId, rewardId, username, commands, () -> {
                    buy.confirm(userId, rewardId, username, () -> {
                        inFlight.remove(userId + ":" + rewardId);
                    });
                });
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
                latestVersion = version.split(" ")[0];

            } catch (Exception e) {
                if (platform.isDebug()) {
                    logger.warning("Failed to check for updates: " + e.getMessage());
                }
            }
        }, 0L, intervalTicks);
    }

    public CompletableFuture<Map<String, Object>> authenticateServer(String secretKey) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("secret", secretKey);
        api.send("POST", "server/auth", payload, future::complete);
        return future;
    }

    public List<Map<String, String>> getRewardsList() {
        if (rewardsList == null) return Collections.emptyList();

        return rewardsList.stream()
                .map(original -> {
                    Map<String, String> filtered = new HashMap<>();

                    Object idObj = original.get("idReward");
                    filtered.put("id", idObj != null ? String.valueOf(idObj) : "");

                    Object nameObj = original.get("nameReward");
                    filtered.put("name", nameObj != null ? String.valueOf(nameObj) : "");

                    Object costObj = original.get("costReward");
                    filtered.put("cost", costObj != null ? String.valueOf(costObj) : "0");

                    Object desc = original.get("descriptionReward");
                    filtered.put("description", desc != null ? String.valueOf(desc) : "");

                    Object catObj = original.get("category");
                    filtered.put("category", catObj != null ? String.valueOf(catObj) : "Default");

                    return filtered;
                })
                .collect(Collectors.toList());
    }

    public void reload(){
        scheduler.cancelAll();
        config.getMainConfig().reload();
        int interval = config.getMainConfig().getInt("fetch_interval", 60);
        if(interval < 60){
            logger.warning("WARNING: The fetch_interval in config must be over 60. Your fetch_interval: " + interval + ". Setting it as 60.");
            interval = 60;
            config.getMainConfig().set("fetch_interval", 60);
            config.getMainConfig().save();
        }
        this.intervalTicks = interval * 20L;
        start();
    }
}