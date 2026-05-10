package net.r_developing.rewardsx;

import lombok.Getter;
import net.r_developing.rewardsx.Configs.MainConfig;
import net.r_developing.rewardsx.Configs.MessagesConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public class Config {
    private final Plugin plugin;
    private final Path data;

    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;

    private File mainConfigFile;
    private FileConfiguration mainConfigFileConfig;

    private File messagesConfigFile;
    private FileConfiguration messagesConfigFileConfig;

    private File userDataFile;
    private FileConfiguration userDataConfig;

    @Getter
    private File rewardsFile;
    @Getter
    private FileConfiguration rewardsConfig;

    public Config(Plugin plugin, MainConfig mainConfig, MessagesConfig messagesConfig, Path data) {
        this.plugin = plugin;
        this.data = data;
        this.mainConfig = mainConfig;
        this.messagesConfig = messagesConfig;

        loadConfigs();
        loadUserData();
        checkMissing();
    }

    private void saveDefaultIfMissing(String name) {
        File targetFile = new File(data.toFile(), name);
        if (!targetFile.exists()) {
            plugin.saveResource(name, false);
        }
    }

    public void loadConfigs() {
        String mainPath = "main.yml";
        saveDefaultIfMissing(mainPath);
        mainConfigFile = new File(data.toFile(), mainPath);
        mainConfigFileConfig = YamlConfiguration.loadConfiguration(mainConfigFile);

        String messagesPath = "messages.yml";
        saveDefaultIfMissing(messagesPath);
        messagesConfigFile = new File(data.toFile(), messagesPath);
        messagesConfigFileConfig = YamlConfiguration.loadConfiguration(messagesConfigFile);

        String rewardsPath = "rewards.yml";
        saveDefaultIfMissing(rewardsPath);
        rewardsFile = new File(data.toFile(), rewardsPath);
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public void loadUserData() {
        String userDataPath = "userdata.yml";
        saveDefaultIfMissing(userDataPath);
        userDataFile = new File(data.toFile(), userDataPath);
        userDataConfig = YamlConfiguration.loadConfiguration(userDataFile);
    }

    public FileConfiguration getMainConfig() {
        return mainConfigFileConfig;
    }

    public FileConfiguration getMessagesConfig() {
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

    public Player getPlayerById(String userId) {
        if (userDataConfig == null || userId == null) return null;
        return userDataConfig.getKeys(false).stream()
                .filter(key -> userId.equals(userDataConfig.getString(key)))
                .map(key -> {
                    try { return Bukkit.getPlayer(UUID.fromString(key)); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public UUID getUUIDById(String userId) {
        if (userDataConfig == null || userId == null) return null;
        return userDataConfig.getKeys(false).stream()
                .filter(key -> userId.equals(userDataConfig.getString(key)))
                .map(key -> {
                    try { return UUID.fromString(key); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public OfflinePlayer getOfflinePlayer(String userId) {
        UUID uuid = getUUIDById(userId);
        if (uuid == null) return null;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.hasPlayedBefore() ? player : null;
    }

    public List<String> getAllUserIds() {
        if (userDataConfig == null) return Collections.emptyList();
        List<String> userIds = new ArrayList<>();
        for (String key : userDataConfig.getKeys(false)) {
            String id = userDataConfig.getString(key);
            if (id != null) userIds.add(id);
        }
        return userIds;
    }

    public void removeUserId(UUID playerUUID) {
        if (userDataConfig == null) return;
        userDataConfig.set(playerUUID.toString(), null);
        saveUserData();
    }

    private void saveUserData() {
        if (userDataConfig == null || userDataFile == null) return;
        try {
            userDataConfig.save(userDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save userdata.yml: " + e.getMessage());
        }
    }

    public void reloadConfigs() {
        loadConfigs();
        checkMissing();
    }

    private void checkMissing() {
        try {
            for (Field f : MainConfig.class.getDeclaredFields()) {
                f.setAccessible(true);
                String key = f.getName();
                if (mainConfigFileConfig.get(key) == null && f.get(mainConfig) != null) {
                    mainConfigFileConfig.set(key, f.get(mainConfig));
                }
            }
            mainConfigFileConfig.save(mainConfigFile);
            plugin.getLogger().info("Main config updated.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (Field f : MessagesConfig.class.getDeclaredFields()) {
                f.setAccessible(true);
                String key = f.getName();
                if (messagesConfigFileConfig.get(key) == null && f.get(messagesConfig) != null) {
                    messagesConfigFileConfig.set(key, f.get(messagesConfig));
                }
            }
            messagesConfigFileConfig.save(messagesConfigFile);
            plugin.getLogger().info("Messages config updated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

