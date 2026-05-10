package net.r_developing.rewardsx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Translator {
    private final Api api;

    public Translator(Api api) {
        this.api = api;
    }

    public CompletableFuture<String> translate(String message, String target) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("target", target);

        api.send("translate", payload, result -> {
            try {
                String response = result.get("message").toString();
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
