package net.r_developing.rewardsx.api.core;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.updater.Version;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Platform {
    private final Config config;
    private final Api api;
    private final PlatformAdapter adapter;

    @Getter
    private String name;
    private volatile Boolean cachedValidation = null;
    private volatile long lastValidationTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    // Iniezione di dipendenze: passiamo l'Adapter e l'API
    public Platform(Config config, Api api, PlatformAdapter adapter) {
        this.config = config;
        this.api = api;
        this.adapter = adapter;
    }

    public String getId() {
        return config.getMainConfig().getString("platform_id");
    }

    public String getKey() {
        return config.getMainConfig().getString("platform_secret");
    }

    public boolean isProxyOrBungee() {
        boolean isProxyMode = config.getMainConfig().getBoolean("proxy", false);
        boolean isBungee = (adapter.type() == PlatformAdapter.Type.BUNGEECORD || adapter.type() == PlatformAdapter.Type.VELOCITY);
        return isBungee || (isProxyMode && adapter.type() == PlatformAdapter.Type.SPIGOT);
    }

    public void checkAndStart(Fetcher fetcher, Messager messager, Version version) {
        adapter.cancelAllTasks(); // Astratto
        cachedValidation = null;
        lastValidationTime = 0;

        boolean isProxyMode = config.getMainConfig().getBoolean("proxy", false);
        boolean isBungee = (adapter.type() == PlatformAdapter.Type.BUNGEECORD || adapter.type() == PlatformAdapter.Type.VELOCITY);

        if (isBungee || (!isProxyMode && adapter.type() == PlatformAdapter.Type.SPIGOT)) {
            adapter.registerCommandsAndEvents();
        } else {
            // Spigot in proxy-mode: non registrare comandi
            adapter.getLogger().info("Running in proxy backend mode: commands handled by Bungee.");
        }

        isValid(valid -> {
            if(valid) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", getId());

                api.send("verifyplatform", payload, res -> {});
                fetcher.start();

                adapter.getLogger().info(toAnsi(String.format(messager.get("welcome"), getName())));

                // Astratto
                adapter.runTaskLater(() -> version.checkVersion(null), 100L);
            } else {
                adapter.getLogger().info(toAnsi(messager.get("configurePlatform")));
            }
        });
    }

    public boolean isDebug() {
        return config.getMainConfig().getBoolean("debug");
    }
    public boolean isProxy() {
        return config.getMainConfig().getBoolean("proxy");
    }
    public boolean isTranslator() {
        return config.getMainConfig().getBoolean("translator");
    }
    public boolean isRewardsXCommandsEnabled() {
        return config.getMainConfig().getBoolean("enable_commands");
    }

    public String getLanguage() {
        String locale = config.getMainConfig().getString("language");
        if(locale == null || locale.isEmpty()) return "en";
        return locale.split("_")[0].toLowerCase();
    }

    public void isValid(Consumer<Boolean> callback) {
        if(!api.init()) {
            callback.accept(false);
            return;
        }

        long now = System.currentTimeMillis();
        if(cachedValidation != null && (now - lastValidationTime) < CACHE_DURATION) {
            if(isDebug()) {
                System.out.println("Using cached validation result: " + cachedValidation);
            }
            callback.accept(cachedValidation);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        api.send("getplatform", payload, response -> {
            if(response == null) {
                System.err.println("Request failed or returned null");
                callback.accept(false);
                return;
            }

            boolean valid = "true".equalsIgnoreCase(String.valueOf(response.get("success")));
            if(valid) {
                @SuppressWarnings("unchecked")
                Map<String, Object> platformObj = (Map<String, Object>) response.get("platform");
                this.name = String.valueOf(platformObj.get("name_server"));
                cachedValidation = true;
                lastValidationTime = System.currentTimeMillis();

                if(isDebug()) {
                    System.out.println("Validation result from API: true, name: " + name);
                }
            }

            callback.accept(valid);
        });
    }

    private static String toAnsi(String msg) {
        return msg.replace("§0","\u001B[30m").replace("§1","\u001B[34m").replace("§2","\u001B[32m")
                .replace("§3","\u001B[36m").replace("§4","\u001B[31m").replace("§5","\u001B[35m")
                .replace("§6","\u001B[33m").replace("§7","\u001B[37m").replace("§8","\u001B[90m")
                .replace("§9","\u001B[94m").replace("§a","\u001B[92m").replace("§b","\u001B[96m")
                .replace("§c","\u001B[91m").replace("§d","\u001B[95m").replace("§e","\u001B[93m")
                .replace("§f","\u001B[97m").replace("§r","\u001B[0m");
    }
}