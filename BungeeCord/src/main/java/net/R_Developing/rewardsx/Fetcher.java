package net.R_Developing.rewardsx;

import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Fetcher {
    private final Plugin plugin;
    private final Api api;
    private final long intervalSeconds; // seconds, not ticks
    private final Config config;
    private final Platform platform;

    @Setter
    private Buy buy;

    public final Map<String, Integer> bitsList = new HashMap<>();
    public final Map<String, Integer> buysList = new HashMap<>();
    public String latestVersion = "";

    public Fetcher(Plugin plugin, Api api, Config config, Platform platform, Buy buy, int intervalSeconds) {
        this.plugin = plugin;
        this.api = api;
        this.intervalSeconds = intervalSeconds;
        this.config = config;
        this.platform = platform;
        this.buy = buy;
    }

    public void start() {
        ProxyServer.getInstance().getScheduler().schedule(
                plugin,
                () -> {
                    // 1. Version check
                    try {
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.spiget.org/v2/resources/121867/versions/latest"))
                                .build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                        String version = obj.get("name").getAsString();
                        latestVersion = version.split(" ")[0];
                    } catch (Exception e) {
                        if (platform.isDebug()) {
                            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                        }
                    }

                    // 2. buys
                    Map<String, Object> buysPayload = new HashMap<>();
                    List<String> userIds = config.getAllUserIds();
                    buysPayload.put("ids", userIds);
                    api.send("getmultiplebuys", buysPayload, result -> {
                        buysList.clear();
                        if (result != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("buys");
                            if (list != null) {
                                for (Map<String, Object> o : list) {
                                    String idPlayer = Objects.toString(o.get("id_player"), null);
                                    if (idPlayer != null) {
                                        int count = 0;
                                        Object countObj = o.get("buys_count");
                                        if (countObj != null) {
                                            if (countObj instanceof Number) {
                                                count = ((Number) countObj).intValue();
                                            } else if (countObj instanceof String) {
                                                try {
                                                    count = Integer.parseInt(((String) countObj).trim());
                                                } catch (NumberFormatException ignored) {}
                                            }
                                        }
                                        buysList.put(idPlayer, count);
                                    }
                                }
                            }
                        }
                    });

                    // 3. adbits
                    Map<String, Object> adbitsPayload = new HashMap<>();
                    adbitsPayload.put("ids", userIds);
                    api.send("getmultipleadbits", adbitsPayload, result -> {
                        bitsList.clear();
                        if (result != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("adbits");
                            if (list != null) {
                                for (Map<String, Object> o : list) {
                                    String idPlayer = Objects.toString(o.get("id_player"), null);
                                    if (idPlayer != null) {
                                        int adbits = 0;
                                        Object adbitsObj = o.get("adbits_player");
                                        if (adbitsObj != null) {
                                            if (adbitsObj instanceof Number) {
                                                adbits = ((Number) adbitsObj).intValue();
                                            } else if (adbitsObj instanceof String) {
                                                try {
                                                    adbits = Integer.parseInt(((String) adbitsObj).trim());
                                                } catch (NumberFormatException ignored) {}
                                            }
                                        }
                                        bitsList.put(idPlayer, adbits);
                                    }
                                }
                            }
                        }
                    });

                    // 4. successbuys
                    Map<String, Object> successBuysPayload = new HashMap<>();
                    successBuysPayload.put("platform", platform.getId());
                    api.send("getsuccessbuys", successBuysPayload, result -> {
                        if (result != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("buys");

                            if (list != null) {
                                for (Map<String, Object> o : list) {
                                    String userId = (String) o.get("userId");
                                    String rewardId = (String) o.get("rewardId");
                                    String username = (String) o.get("username");

                                    if (username != null) {
                                        buy.confirm(userId, rewardId, username);
                                    } else {
                                        buy.confirm(userId, rewardId);
                                    }
                                }
                            }
                        }
                    });
                },
                0L,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }
}