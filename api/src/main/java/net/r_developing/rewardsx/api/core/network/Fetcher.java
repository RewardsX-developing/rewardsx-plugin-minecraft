package net.r_developing.rewardsx.api.core.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Setter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.Platform;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
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

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    // Costruttore con Dependency Injection per usare componenti Platform-Agnostic
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
        // Usiamo l'astrazione asincrona invece di Bukkit.getScheduler()
        scheduler.runAsyncRepeatingTask(() -> {

            // 1. rewards
            Map<String, Object> rewardsPayload = new HashMap<>();
            rewardsPayload.put("platform", platform.getId());
            api.send("getrewards", rewardsPayload, result -> {
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> list = (List<Map<String, String>>) result.get("rewards");
                    rewardsList = list != null ? list : Collections.emptyList();
                }
            });

            // 4. successbuys
            if (!platform.isProxy()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("platform", platform.getId());
                api.send("getsuccessbuys", payload, result -> {
                    if (result != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("buys");

                        if (list != null) {
                            for (Map<String, Object> o : list) {
                                String userId   = Objects.toString(o.get("userId"), null);
                                String rewardId = Objects.toString(o.get("rewardId"), null);
                                String username = Objects.toString(o.get("username"), null);

                                String key = userId + ":" + rewardId;
                                if (!inFlight.add(key)) continue;

                                if (username != null) buy.confirm(userId, rewardId, username);
                                else                  buy.confirm(userId, rewardId);
                            }
                        } else {
                            logger.warning("Successbuys list is null"); // Astrazione logger
                        }
                    }
                });
            }

            // Version check
            try (InputStream in = new URL("https://api.spiget.org/v2/resources/121867/versions/latest").openStream();
                 Scanner scanner = new Scanner(in)) {

                String json = scanner.useDelimiter("\\A").next();
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String version = obj.get("name").getAsString();
                latestVersion = version.split(" ")[0];

            } catch (Exception e) {
                if (platform.isDebug()) {
                    logger.warning("Failed to check for updates: " + e.getMessage()); // Astrazione logger
                }
            }
        }, 0L, intervalTicks);
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

    // Utility per pulire il codice di parsing
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