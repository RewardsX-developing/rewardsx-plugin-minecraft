package net.R_Developing.rewardsx;

import lombok.Getter;
import net.R_Developing.rewardsx.Configs.MainConfig;
import net.R_Developing.rewardsx.Configs.MessagesConfig;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

public class Config {
    private final Plugin plugin;
    private final File data;

    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;

    private File mainConfigFile;
    private Configuration mainConfigFileConfig;

    private File messagesConfigFile;
    private Configuration messagesConfigFileConfig;

    private File userDataFile;
    private Configuration userDataConfig;

    @Getter
    private File rewardsFile;

    @Getter
    private Configuration rewardsConfig;

    public Config(Plugin plugin, MainConfig mainConfig, MessagesConfig messagesConfig, File data) {
        this.plugin = plugin;
        this.data = data;
        this.mainConfig = mainConfig;
        this.messagesConfig = messagesConfig;

        loadConfigs();
        loadUserData();
        checkMissing();
    }

    private void saveDefaultIfMissing(String name) {
        File targetFile = new File(data, name);

        if (!data.exists() && !data.mkdirs()) {
            plugin.getLogger().severe("Failed to create plugin data folder: " + data.getAbsolutePath());
            return;
        }

        if (!targetFile.exists()) {
            plugin.getLogger().info("Saving default config: " + name);
            try (InputStream in = plugin.getResourceAsStream(name)) {
                if (in != null) {
                    java.nio.file.Files.copy(in, targetFile.toPath());
                } else {
                    plugin.getLogger().warning("Could not find resource in jar: " + name);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save default config " + name + ": " + e.getMessage());
            }
        }
    }

    public void loadConfigs() {
        String mainPath = "main.yml";
        saveDefaultIfMissing(mainPath);
        mainConfigFile = new File(data, mainPath);
        mainConfigFileConfig = loadYaml(mainConfigFile);

        String messagesPath = "messages.yml";
        saveDefaultIfMissing(messagesPath);
        messagesConfigFile = new File(data, messagesPath);
        messagesConfigFileConfig = loadYaml(messagesConfigFile);

        String rewardsPath = "rewards.yml";
        saveDefaultIfMissing(rewardsPath);
        rewardsFile = new File(data, rewardsPath);
        rewardsConfig = loadYaml(rewardsFile);
    }

    public void loadUserData() {
        String userDataPath = "userdata.yml";
        saveDefaultIfMissing(userDataPath);
        userDataFile = new File(data, userDataPath);
        userDataConfig = loadYaml(userDataFile);
    }

    private Configuration loadYaml(File file) {
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load config: " + file.getName());
            return new Configuration();
        }
    }

    public Configuration getMainConfig() {
        return mainConfigFileConfig;
    }

    public Configuration getMessagesConfig() {
        return messagesConfigFileConfig;
    }

    public void saveUserId(UUID playerUUID, String userId) {
        if (userDataConfig == null) return;
        userDataConfig.set(playerUUID.toString(), userId);
        saveUserData();
    }

    public String getUserId(UUID playerUUID) {
        if (userDataConfig == null) return null;
        return userDataConfig.getString(playerUUID.toString());
    }

    public ProxiedPlayer getPlayerById(String userId) {
        if (userDataConfig == null || userId == null) return null;
        for (String key : userDataConfig.getKeys()) {
            if (userId.equals(userDataConfig.getString(key))) {
                try {
                    UUID uuid = UUID.fromString(key);
                    return plugin.getProxy().getPlayer(uuid);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    public UUID getUUIDById(String userId) {
        if (userDataConfig == null || userId == null) return null;
        for (String key : userDataConfig.getKeys()) {
            if (userId.equals(userDataConfig.getString(key))) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    public List<String> getAllUserIds() {
        List<String> userIds = new ArrayList<>();
        if (userDataConfig == null) return userIds;
        for (String key : userDataConfig.getKeys()) {
            String userId = userDataConfig.getString(key);
            if (userId != null)
                userIds.add(userId);
        }
        return userIds;
    }

    public void removeUserId(UUID playerUUID) {
        if(userDataConfig == null) return;
        userDataConfig.set(playerUUID.toString(), null);
        saveUserData();
    }

    public boolean hasUserId(UUID playerUUID) {
        return userDataConfig.contains(playerUUID.toString());
    }

    private void saveUserData() {
        if (userDataConfig == null || userDataFile == null) return;
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(userDataConfig, userDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save userdata.yml: " + e.getMessage());
        }
    }

    public void reloadConfigs() {
        loadConfigs();
        checkMissing();
    }

    private void checkMissing() {
        updateMissingFields(MainConfig.class, mainConfig, mainConfigFileConfig, mainConfigFile, "Main config");
        updateMissingFields(MessagesConfig.class, messagesConfig, messagesConfigFileConfig, messagesConfigFile, "Messages config");
    }

    private void updateMissingFields(Class<?> clazz, Object source, Configuration config, File file, String logName) {
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String key = field.getName();
                if (config.get(key) == null && field.get(source) != null) {
                    config.set(key, field.get(source));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
            plugin.getLogger().info(logName + " updated.");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + logName + ": " + e.getMessage());
        }
    }
}
