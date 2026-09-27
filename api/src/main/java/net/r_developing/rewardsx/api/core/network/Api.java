package net.r_developing.rewardsx.api.core.network;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.r_developing.rewardsx.api.core.Platform;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class Api {
    /** Base URL for all API requests. */
    public static final String BASE_URL = "https://api.rewardsx.net/v2/";

    /** Endpoint on the main website used to generate short links and track affiliates. */
    public static final String SHORTENER_URL = "https://rewardsx.net/api/links";

    /** HTTP client (connection pooling, retry logic). */
    private final OkHttpClient client;

    @Setter
    @Getter
    public Platform platform = null;

    /** Platform ID (assigned by RewardsX backend). */
    private String id;

    /** Platform secret key (used for authorization). */
    private String key;

    /** Constructor - just instantiates the OkHttpClient. */
    public Api() {
        this.client = new OkHttpClient();
    }

    public boolean init() {
        if (platform == null) {
            System.err.println("Api.init() called but platform is null!");
            return false;
        }

        id = platform.getId();
        key = platform.getKey();

        if (id == null || key == null) {
            System.err.println("RewardsX init requires both id and key");
            return false;
        }

        return true;
    }

    // =========================================================================
    // REWARDSX.NET/P/XXXXXXX
    // =========================================================================

    /**
     * Generates a short purchase URL (https://rewardsx.net/p/XXXXXX) without an explicit affiliate.
     * Any affiliate cookie already stored in the player's browser will still be tracked.
     *
     * @param token       the purchase token
     * @param urlCallback callback invoked with the short URL (or fallback URL on error)
     */
    public void shortenPurchaseLink(String token, Consumer<String> urlCallback) {
        shortenPurchaseLink(token, null, urlCallback);
    }

    /**
     * Generates a short purchase URL (https://rewardsx.net/p/XXXXXX) with an optional affiliate code.
     * Automatically falls back to BASE_URL + "confirm-purchase/" + token if the shortener fails.
     *
     * @param token       the purchase token
     * @param affiliateId optional affiliate code (can be null)
     * @param urlCallback callback invoked with the short URL (or fallback URL on error)
     */
    public void shortenPurchaseLink(String token, String affiliateId, Consumer<String> urlCallback) {
        String fallbackUrl = BASE_URL + "confirm-purchase/" + token;

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "purchase");
            payload.addProperty("token", token);

            if (affiliateId != null && !affiliateId.trim().isEmpty()) {
                payload.addProperty("affiliateId", affiliateId.trim());
            }

            String jsonString = payload.toString();

            if(platform.isDebug()) {
                System.out.println("[RewardsX-Shortener DEBUG] POST: " + SHORTENER_URL);
                System.out.println("[RewardsX-Shortener DEBUG] Payload: " + jsonString);
            }

            RequestBody body = RequestBody.create(
                    jsonString,
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SHORTENER_URL)
                    .header("User-Agent", "RewardsX-Plugin/1.0")
                    .header("Accept", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    if(platform.isDebug()) System.err.println("[RewardsX-Shortener ERROR] Failed to connect: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    urlCallback.accept(fallbackUrl);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    try (response) {
                        int code = response.code();
                        String finalUrl = response.request().url().toString();
                        String responseBody = response.body() != null ? response.body().string() : "NULL";

                        if(platform.isDebug()) {
                            System.out.println("[RewardsX-Shortener DEBUG] HTTP Status: " + code);
                            System.out.println("[RewardsX-Shortener DEBUG] Final URL: " + finalUrl);
                        }

                        if (response.priorResponse() != null) {
                            if(platform.isDebug()) {
                                System.out.println("[RewardsX-Shortener WARN] Redirect! Initial Status : "
                                        + response.priorResponse().code() + " -> Final Method: " + response.request().method());
                            }
                        }

                        if(platform.isDebug())
                            System.out.println("[RewardsX-Shortener DEBUG] Response Body: " + responseBody);

                        if (!response.isSuccessful() || response.body() == null) {
                            if(platform.isDebug()) System.err.println("[RewardsX-Shortener ERROR] Request failed with HTTP code " + code);
                            urlCallback.accept(fallbackUrl);
                            return;
                        }

                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                        if (json.has("shortUrl") && !json.get("shortUrl").isJsonNull()) {
                            String shortUrl = json.get("shortUrl").getAsString();
                            if(platform.isDebug()) System.out.println("[RewardsX-Shortener DEBUG] Short URL created: " + shortUrl);
                            urlCallback.accept(shortUrl);
                        } else {
                            if(platform.isDebug()) System.err.println("[RewardsX-Shortener ERROR] 'shortUrl' missing on JSON: " + responseBody);
                            urlCallback.accept(fallbackUrl);
                        }
                    } catch (Throwable e) {
                        if(platform.isDebug()) System.err.println("[RewardsX-Shortener ERROR] Error: " + e.getMessage());
                        e.printStackTrace();
                        urlCallback.accept(fallbackUrl);
                    }
                }
            });
        } catch (Throwable e) {
            if(platform.isDebug()) System.err.println("[RewardsX-Shortener ERROR] Error: " + e.getMessage());
            e.printStackTrace();
            urlCallback.accept(fallbackUrl);
        }
    }

    // =========================================================================
    // API.REWARDSX.NET
    // =========================================================================

    public void send(String method, String endpoint, Map<String, Object> payload, Consumer<Map<String, Object>> result) {
        send(method, endpoint, null, payload, result);
    }

    public void send(String method, String endpoint, Map<String, String> headers, Map<String, Object> payload, Consumer<Map<String, Object>> result) {
        if (id == null || key == null) {
            if (platform.isDebug()) System.err.println("API not initialized. Call init(platform, apiKey) first");
            result.accept(null);
            return;
        }

        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put("platform", id);

        sendRequest(method, endpoint, headers, payload, responseBody -> {
            if (responseBody == null) {
                result.accept(null);
                return;
            }

            try {
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                Map<String, Object> resultMap = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    resultMap.put(entry.getKey(), convertJsonElement(entry.getValue()));
                }

                result.accept(resultMap);
                if (platform.isDebug()) System.out.println(resultMap);
            } catch (Exception e) {
                if (platform.isDebug()) System.err.println("Failed in API to parse response: " + e.getMessage());
                result.accept(null);
            }
        });
    }

    private void sendRequest(String method, String endpoint, Map<String, String> headers, Map<String, Object> payload, Consumer<String> onSuccess) {
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .addHeader("Authorization", "key " + key)
                    .addHeader("platform", id);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            if (platform.isDebug()) {
                System.out.println("Requesting API (" + method.toUpperCase() + ") with credentials: id=" + id);
            }

            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(BASE_URL + endpoint)).newBuilder();
            if (payload != null) {
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    if (entry.getValue() != null) {
                        urlBuilder.addQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
            requestBuilder.url(urlBuilder.build().toString());

            if (method.equalsIgnoreCase("GET")) {
                requestBuilder.get();
            } else if (method.equalsIgnoreCase("POST")) {
                Gson gson = new Gson();
                String jsonPayload = gson.toJson(payload != null ? payload : new HashMap<>());
                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.get("application/json; charset=utf-8")
                );
                requestBuilder.post(body);
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    if (platform.isDebug()) System.err.println(method.toUpperCase() + " request failed: " + e.getMessage());
                    onSuccess.accept(null);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseBody = null;
                    try (response) {
                        if (response.body() != null) {
                            responseBody = response.body().string();
                        }
                    }

                    if (platform.isDebug()) {
                        System.out.println("Endpoint: " + endpoint);
                        System.out.println("Response code: " + response.code());
                        System.out.println("Response body: " + responseBody);
                    }

                    if (!response.isSuccessful()) {
                        if (platform.isDebug())
                            System.err.println(method.toUpperCase() + " request failed with code: " + response.code() + ", endpoint: " + endpoint);
                        onSuccess.accept(null);
                        return;
                    }

                    onSuccess.accept(responseBody);
                }
            });
        } catch (Exception e) {
            if (platform.isDebug()) System.err.println("Failed to create request: " + e.getMessage());
            onSuccess.accept(null);
        }
    }

    private Object convertJsonElement(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.getAsString();
        } else if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(convertJsonElement(item));
            }
            return list;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertJsonElement(entry.getValue()));
            }
            return map;
        }
        return null;
    }
}