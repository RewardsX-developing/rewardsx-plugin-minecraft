package net.r_developing.rewardsx;

import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Platform {
    private final Config config;
    private final Api api;
    private final Plugin plugin;

    @Getter
    private String name;
    private volatile Boolean cachedValidation = null;
    private volatile long lastValidationTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    public Platform(Plugin plugin, Config config) {
        this.config = config;
        this.plugin = plugin;
        this.api = Main.getInstance().getApi();
    }

    public String getId() {
        return config.getMainConfig().getString("platform_id");
    }
    public String getKey() {
        return config.getMainConfig().getString("platform_secret");
    }

    public void checkAndStart(Fetcher fetcher, RewardsGUI rewardsGUI, Messager messager, Version version, QuickConnect login, Bits bits, Buy buy, Account account) {
        Bukkit.getScheduler().cancelTasks(plugin);
        cachedValidation = null;
        lastValidationTime = 0;

        registerCommand(new Commands(rewardsGUI, messager, config, version, login, bits, this, fetcher, buy, account));

        isValid(valid -> {
            if(valid) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", getId());

                api.send("verifyplatform", payload, res -> {});
                fetcher.start();
                plugin.getLogger().info(toAnsi(String.format(messager.get("welcome"), getName())));
                Bukkit.getScheduler().runTaskLater(plugin, () -> version.checkVersion(null), 100L);
                Bukkit.getPluginManager().registerEvents(new Events(rewardsGUI, config, messager, this), plugin);
            } else {
                plugin.getLogger().info(toAnsi(messager.get("configurePlatform")));
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
    public boolean isAllowedShowingConnectMessageOnJoin() {
        return config.getMainConfig().getBoolean("show_connect_message_on_join");
    }
    public boolean isRewardsXCommandsEnabled() {
        return config.getMainConfig().getBoolean("enable_commands");
    }

    public String getLanguage() {
        String locale = config.getMainConfig().getString("language");
        if(locale == null || locale.isEmpty()) return "en";
        return locale.split("_")[0].toLowerCase();
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

    private void registerCommand(CommandExecutor executor) {
        if(!isRewardsXCommandsEnabled()) {
            return;
        }

        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            PluginCommand command = constructor.newInstance("rewardsx", plugin);
            command.setExecutor(executor);

            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(plugin.getServer());

            commandMap.register(plugin.getDescription().getName(), command);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
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