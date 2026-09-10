package net.r_developing.rewardsx.api.core.network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Translates message strings to a target language via the backend.
 *
 * <p>Sends a translation request to the RewardsX API and returns a CompletableFuture
 * that completes with the translated text. Currently, the actual API call is commented out,
 * so this class just returns an incomplete future (which will never resolve).
 *
 * <p>This is used by Messager to translate all message keys to match the player's language
 * during async translation loading. Each message is sent individually for translation.
 */
public class Translator {
    /** HTTP client for backend API calls. */
    private final Api api;

    /**
     * Constructor injection - stores a reference to the API client.
     *
     * @param api the HTTP client for backend requests
     */
    public Translator(Api api) {
        this.api = api;
    }

    /**
     * Translates a message to a target language.
     *
     * <p>Builds a payload with the message and target language code, sends it to the
     * backend translate endpoint, and returns a CompletableFuture that completes with
     * the translated text.
     *
     * <p>On success, extracts the "message" field from the response and completes the
     * future with it. On failure, completes the future with an exception.
     *
     * <p>NOTE: The actual API call is currently commented out. This method returns an
     * incomplete future that will never resolve. Once the backend translation endpoint
     * is implemented, uncomment the api.send() block to enable translations.
     *
     * @param message the text to translate
     * @param target  the target language code (e.g. "it", "es", "fr")
     * @return a future that completes with the translated text (or exceptionally on error)
     */
    public CompletableFuture<String> translate(String message, String target) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("target", target);

        /*
        api.send("POST", "translate", payload, result -> {
            try {
                String response = result.get("message").toString();
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        */

        return future;
    }
}