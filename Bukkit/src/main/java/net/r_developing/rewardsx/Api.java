package net.r_developing.rewardsx;

import com.google.gson.*;
import lombok.Setter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class Api {
    private static final String BASE_URL = "https://proxy.rewardsx.net/v1/";
    private final OkHttpClient client;
    @Setter
    public Platform platform = null;
    private String id;
    private String key;

    public Api() {
        this.client = new OkHttpClient();
    }

    public boolean init() {
        id = platform.getId();
        key = platform.getKey();

        if(id == null || key == null) {
            System.err.println("RewardsX init requires both id and key");
            return false;
        }

        return true;
    }

    public void send(String endpoint, Map<String, Object> payload, Consumer<Map<String, Object>> result) {
        if(id == null || key == null) {
            if(platform.isDebug()) System.err.println("API not initialized. Call init(platform, apiKey) first");
            result.accept(null);
            return;
        }

        payload.put("platform", id);
        sendRequest(endpoint, payload, responseBody -> {
            if(responseBody == null) {
                result.accept(null);
                return;
            }

            try {
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                Map<String, Object> resultMap = new HashMap<>();

                for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    resultMap.put(entry.getKey(), convertJsonElement(entry.getValue()));
                }

                result.accept(resultMap);
                if(platform.isDebug()) System.out.println(resultMap);
            } catch(Exception e) {
                if(platform.isDebug()) System.err.println("Failed in API to parse response: " + e.getMessage() + e.getClass());
                result.accept(null);
            }
        });
    }

    private void sendRequest(String endpoint, Map<String, Object> payload, Consumer<String> onSuccess) {
        try {
            Gson gson = new Gson();
            String jsonPayload = gson.toJson(payload != null ? payload : new HashMap<>());

            RequestBody body = RequestBody.create(
                    MediaType.get("application/json; charset=utf-8"),
                    jsonPayload
                        );

            if(platform.isDebug()) {
                System.out.println("Requesting API with credentials:\nid: " + id + "\nkey: " + key);
            }

            Request request = new Request.Builder()
                    .url(BASE_URL + endpoint)
                    .addHeader("Authorization", "key " + key)
                    .addHeader("platform", id)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    if (platform.isDebug()) System.err.println("POST request failed: " + e.getMessage());
                    onSuccess.accept(null);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseBody = null;
                    try(response) {
                        if(response.body() != null) {
                            responseBody = response.body().string();
                        }
                    }

                    if(platform.isDebug()) {
                        System.out.println("Endpoint: " + endpoint);
                        System.out.println("Response code: " + response.code());
                        System.out.println("Response body: " + responseBody);
                    }

                    if(!response.isSuccessful()) {
                        if(platform.isDebug())
                            System.err.println("POST request failed with code: " + response.code() + ", endpoint: " + endpoint);
                        onSuccess.accept(null);
                        return;
                    }

                    onSuccess.accept(responseBody);
                }
            });
        } catch(Exception e) {
            if(platform.isDebug()) System.err.println("Failed to create request: " + e.getMessage());
            onSuccess.accept(null);
        }
    }

    private Object convertJsonElement(JsonElement element) {
        if(element.isJsonNull()) {
            return null;
        } else if(element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.getAsString();
        } else if(element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for(JsonElement item : element.getAsJsonArray()) {
                list.add(convertJsonElement(item));
            }
            return list;
        } else if(element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            for(Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertJsonElement(entry.getValue()));
            }
            return map;
        }
        return null;
    }
}

