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
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Fetcher {
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;
    private final Api api;
    private final long intervalTicks;
    private final Config config;
    private final Platform platform;

    @Setter
    private Buy buy;

    private volatile List<Map<String, String>> rewardsList = Collections.emptyList();
    public final Map<String, Integer> bitsList = new HashMap<>();
    public final Map<String, Integer> buysList = new HashMap<>();
    public String latestVersion = "";
    @Setter
    @Getter
    public RewardFetcher rewardFetcher;

    @Getter
    public final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public Fetcher(PlatformAdapter adapter, PlatformScheduler scheduler, Api api, Config config, Platform platform, Buy buy, int intervalSeconds) {
        this.logger = adapter.getLogger();
        this.scheduler = scheduler;
        this.api = api;
        this.intervalTicks = intervalSeconds * 20L;
        this.config = config;
        this.platform = platform;
        this.buy = buy;
    }

    public void start() {
        scheduler.runAsyncRepeatingTask(() -> {

            Map<String, Object> rewardsPayload = new HashMap<>();
            rewardsPayload.put("platform", platform.getId());

            api.send("GET", "rewards", rewardsPayload, result -> {
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> list = (List<Map<String, String>>) result.get("rewards");
                    rewardsList = list != null ? list : Collections.emptyList();
                }
            });

            String secret = config.getMainConfig().getString("platform_key");

            rewardFetcher.fetchPendingRewards(platform.getId(), secret, inFlight, (userId, rewardId, username, commands) -> {
                buy.confirm(userId, rewardId, username, commands, () -> {
                    inFlight.remove(userId + ":" + rewardId);
                });
            });

            try (InputStream in = new URL("https://api.spiget.org/v2/resources/121867/versions/latest").openStream();
                 Scanner scanner = new Scanner(in)) {

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

    public void removeInFlight(String key) {
        inFlight.remove(key);
    }

    public CompletableFuture<Boolean> linkAccount(UUID uuid, String username, String code) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Map<String, Object> payload = new HashMap<>();
        payload.put("uuid", uuid.toString());
        payload.put("username", username);
        payload.put("code", code);

        api.send("POST", "linkaccount", payload, result -> {
            if (result != null && "true".equalsIgnoreCase(String.valueOf(result.get("success")))) {
                future.complete(true);
            } else {
                future.complete(false);
            }
        });

        return future;
    }

    public CompletableFuture<Map<String, Object>> authenticateServer(String secretKey) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("secret", secretKey);
        api.send("POST", "server/auth", payload, future::complete);
        return future;
    }

    public List<Map<String, String>> getRewardsList() {
        if(rewardsList == null) return Collections.emptyList();

        return rewardsList.stream()
                .map(original -> {
                    Map<String, String> filtered = new HashMap<>();
                    filtered.put("id", original.get("id_reward"));
                    filtered.put("name", original.get("name_reward"));
                    filtered.put("cost", original.get("cost_reward"));
                    filtered.put("description", original.get("description_reward"));
                    return filtered;
                })
                .collect(Collectors.toList());
    }

    private int parseNumber(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt(((String) obj).trim());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}