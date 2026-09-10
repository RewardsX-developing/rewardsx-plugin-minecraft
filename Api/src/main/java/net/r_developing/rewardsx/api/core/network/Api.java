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

/**
 * HTTP client for the RewardsX backend API.
 *
 * <p>Sends requests to <a href="https://api.rewardsx.net/v2/"></a> and handles responses.
 * All requests include platform credentials (id and key) as headers, and are
 * executed asynchronously with a callback pattern. Responses are parsed from JSON
 * and converted to nested Maps/Lists for easy access.
 *
 * <p>Supports GET and POST methods. GET requests put payload data in query parameters;
 * POST requests put payload in the JSON body.
 *
 * <p>Thread-safe - OkHttpClient handles all async work internally, and callbacks
 * are invoked off the main thread. The plugin should not block on these callbacks.
 */
public class Api {
    /** Base URL for all API requests. */
    public static final String BASE_URL = "https://api.rewardsx.net/v2/";

    /** HTTP client (connection pooling, retry logic). */
    private final OkHttpClient client;

    /**
     * Platform reference - used to read id/key and check debug mode.
     * Injected after construction via the setter.
     */
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

    /**
     * Initializes the API with credentials from the platform.
     *
     * <p>Must be called once before any send() calls. Extracts the platform ID
     * and secret key from the Platform object and validates that both are present.
     *
     * @return true if initialization succeeded, false if missing credentials
     */
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

    /**
     * Sends an HTTP request to the API and processes the JSON response.
     *
     * <p>This is the high-level entry point. It:
     *   1. Validates that the API is initialized (id and key are set)
     *   2. Adds the platform ID to the payload
     *   3. Calls sendRequest() to execute the HTTP call async
     *   4. Parses the JSON response into nested Maps and Lists
     *   5. Invokes the result callback with the parsed data (or null on failure)
     *
     * @param method    "GET" or "POST"
     * @param endpoint  the API endpoint (e.g. "complete-buy"), appended to BASE_URL
     * @param payload   request data (will be null-checked and converted to empty map if needed)
     * @param result    callback to invoke with the parsed response (maybe null on error)
     */
    public void send(String method, String endpoint, Map<String, Object> payload, Consumer<Map<String, Object>> result) {
        send(method, endpoint, null, payload, result);
    }

    /**
     * Sends an HTTP request to the API and processes the JSON response.
     *
     * <p>This is the high-level entry point. It:
     *   1. Validates that the API is initialized (id and key are set)
     *   2. Adds the platform ID to the payload
     *   3. Calls sendRequest() to execute the HTTP call async
     *   4. Parses the JSON response into nested Maps and Lists
     *   5. Invokes the result callback with the parsed data (or null on failure)
     *
     * @param method    "GET" or "POST"
     * @param endpoint  the API endpoint (e.g. "complete-buy"), appended to BASE_URL
     * @param headers   custom HTTP headers to include in the request (can be null)
     * @param payload   request data (will be null-checked and converted to empty map if needed)
     * @param result    callback to invoke with the parsed response (maybe null on error)
     */
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

    /**
     * Low-level HTTP request execution.
     *
     * <p>Builds an OkHttp Request with:
     *   - Authorization header (the secret key)
     *   - Platform header (the platform ID)
     *   - Custom headers (if provided)
     *   - URL with query parameters (for GET) or JSON body (for POST)
     *
     * <p>Enqueues the request async - when the response arrives, onSuccess is
     * invoked with the response body string (or null on error).
     *
     * @param method      "GET" or "POST"
     * @param endpoint    the API path (e.g. "complete-buy")
     * @param headers     custom HTTP headers to append (can be null)
     * @param payload     request data
     * @param onSuccess   callback to invoke with the response body (or null on error)
     */
    private void sendRequest(String method, String endpoint, Map<String, String> headers, Map<String, Object> payload, Consumer<String> onSuccess) {
        try {
            // Start building the request - add auth headers.
            Request.Builder requestBuilder = new Request.Builder()
                    .addHeader("Authorization", "key " + key)
                    .addHeader("platform", id);

            // Add any custom headers provided
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            if (platform.isDebug()) {
                System.out.println("Requesting API (" + method.toUpperCase() + ") with credentials: id=" + id);
            }

            // Build the full URL with query parameters (for GET) or will be overridden for POST.
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(BASE_URL + endpoint)).newBuilder();
            if (payload != null) {
                // For GET requests, add all payload entries as query parameters.
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    if (entry.getValue() != null) {
                        urlBuilder.addQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
            requestBuilder.url(urlBuilder.build().toString());

            // Set the HTTP method and body.
            if (method.equalsIgnoreCase("GET")) {
                requestBuilder.get();
            } else if (method.equalsIgnoreCase("POST")) {
                // For POST, serialize the payload to JSON and send in the body.
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

            // Build and enqueue the request.
            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                /**
                 * Called when the network request fails (no response received).
                 */
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    if (platform.isDebug()) System.err.println(method.toUpperCase() + " request failed: " + e.getMessage());
                    onSuccess.accept(null);
                }

                /**
                 * Called when the network request succeeds (a response was received).
                 * Note: a successful network response does not mean a successful API call
                 * (the backend may return an error status code).
                 */
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseBody = null;
                    // Read the response body and close the response.
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

                    // Check the HTTP status code - only 2xx codes are considered successful.
                    if (!response.isSuccessful()) {
                        if (platform.isDebug())
                            System.err.println(method.toUpperCase() + " request failed with code: " + response.code() + ", endpoint: " + endpoint);
                        onSuccess.accept(null);
                        return;
                    }

                    // Invoke the callback with the response body.
                    onSuccess.accept(responseBody);
                }
            });
        } catch (Exception e) {
            if (platform.isDebug()) System.err.println("Failed to create request: " + e.getMessage());
            onSuccess.accept(null);
        }
    }

    /**
     * Recursively converts a Gson JsonElement to a Java object.
     *
     * <p>Handles:
     *   - null -> null
     *   - primitives (String, number, boolean) -> String
     *   - arrays -> List&lt;Object&gt;
     *   - objects -> Map&lt;String, Object&gt;
     *
     * <p>Note: numbers and booleans are converted to Strings (lossy for numbers).
     * If the API returns typed data, consider using Gson's TypeToken and proper
     * model classes instead.
     *
     * @param element a JsonElement from a parsed JsonObject
     * @return the converted Java object
     */
    private Object convertJsonElement(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            // Convert all primitives to String (numbers, booleans, strings).
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.getAsString();
        } else if (element.isJsonArray()) {
            // Recursively convert array elements.
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(convertJsonElement(item));
            }
            return list;
        } else if (element.isJsonObject()) {
            // Recursively convert object fields.
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertJsonElement(entry.getValue()));
            }
            return map;
        }
        return null;
    }
}