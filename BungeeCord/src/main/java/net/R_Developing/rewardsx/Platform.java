package net.R_Developing.rewardsx;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Platform {
    private final Plugin plugin;
    private final Config config;
    private final Api api;
    private final ProxySender proxySender;

    @Getter
    private String name;

    private volatile Boolean cachedValidation = null;
    private volatile long lastValidationTime = 0;
    private static final long CACHE_DURATION = 30000;

    public Platform(Plugin plugin, Config config, Api api, ProxySender proxySender) {
        this.plugin = plugin;
        this.config = config;
        this.api = api;
        this.proxySender = proxySender;
    }

    public String getId() {
        return String.valueOf(config.getMainConfig().get("platform_id"));
    }
    public String getKey() {
        return String.valueOf(config.getMainConfig().get("platform_secret"));
    }
    public boolean isAllowedShowingConnectMessageOnJoin() {
        Object value = config.getMainConfig().get("show_connect_message_on_join");
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }
    public boolean isRewardsXCommandEnabled() {
        Object value = config.getMainConfig().get("enable_commands");
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean isDebug() {
        Object value = config.getMainConfig().get("debug");
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public void checkAndStart(Fetcher fetcher, Messager messager, Version version, QuickConnect quickConnect, Bits bits, Buy buy, Platform platform, Account account) {
        if(isRewardsXCommandEnabled()) {
            ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new Commands("rewardsx", messager, config, version, quickConnect, bits, this, fetcher, buy, proxySender, account));
        }

        isValid(valid -> {
            if(valid) {
                Map<String, Object> payload = new HashMap<>();
                api.send("verifyplatform", payload, res -> {});

                if(fetcher != null) {
                    fetcher.start();
                }

                ProxyServer.getInstance().getPluginManager().registerListener(plugin, (Listener) new Events(config, messager, platform));
                ProxyServer.getInstance().getScheduler().schedule(plugin, () -> version.checkVersion(null), 5, TimeUnit.SECONDS);

                plugin.getLogger().info(toAnsi(String.format(messager.get("welcome"), getName())));
            } else {
                plugin.getLogger().warning(toAnsi(messager.get("configurePlatform")));
            }
        });
    }

    void isValid(Consumer<Boolean> callback) {
        if(!api.init()) {
            callback.accept(false);
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
                Map<String, Object> platform = (Map<String, Object>) response.get("platform");
                this.name = String.valueOf(platform.get("name_server"));
                cachedValidation = valid;
                lastValidationTime = System.currentTimeMillis();

                if(isDebug()) {
                    System.out.println("Validation result from API: " + valid + ", name: " + name);
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