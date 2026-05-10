package net.R_Developing.rewardsx;

import com.velocitypowered.api.command.CommandMeta;
import lombok.Getter;
import org.slf4j.Logger;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Platform {
    private final Main plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Config config;
    private final Api api;
    private final ProxySender proxySender;

    @Getter
    private String name;

    private volatile Boolean cachedValidation = null;
    private volatile long lastValidationTime = 0;
    private static final long CACHE_DURATION = 30000;

    public Platform(Main plugin, Config config, Api api, ProxySender proxySender) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
        this.logger = plugin.getLogger();
        this.config = config;
        this.api = api;
        this.proxySender = proxySender;
    }

    public String getId() {
        Object id = config.getMainConfig().get("platform_id");
        return id != null ? id.toString() : "";
    }

    public String getKey() {
        Object id = config.getMainConfig().get("platform_secret");
        return id != null ? id.toString() : "";
    }

    public boolean isDebug() {
        Object value = config.getMainConfig().get("debug");
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean isAllowedShowingConnectMessageOnJoin() {
        Object value = config.getMainConfig().get("show_connect_message_on_join");
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean isRewardsXCommandEnabled() {
        Object value = config.getMainConfig().get("enable_commands");
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public void checkAndStart(Fetcher fetcher, Messager messager, Version version, QuickConnect quickConnect, Bits bits, Buy buy, Account account, Platform platform) {
        Commands commands = new Commands(messager, config, version, quickConnect, bits, this, fetcher, buy, proxySender, account);
        CommandMeta commandMeta = proxy.getCommandManager()
                .metaBuilder("rewardsx")
                .aliases("rad")
                .build();

        if(isRewardsXCommandEnabled()) {
            proxy.getCommandManager().register(commandMeta, commands);
        }

        isValid(valid -> {
            if(valid) {
                Map<String, Object> payload = new HashMap<>();
                api.send("verifyplatform", payload, res -> {});

                if(fetcher != null) {
                    fetcher.start();
                }

                proxy.getEventManager().register(plugin, new Events(config, messager, platform));
                proxy.getScheduler().buildTask(plugin, () -> version.checkVersion(null))
                        .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                        .schedule();

                logger.info(toAnsi(String.format(messager.get("welcome"), getName())));
            } else {
                logger.warn(toAnsi(messager.get("configurePlatform")));
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
                cachedValidation = true;
                lastValidationTime = System.currentTimeMillis();

                if(isDebug()) {
                    System.out.println("Validation result from API: " + true + ", name: " + name);
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
