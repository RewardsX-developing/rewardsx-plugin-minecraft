package net.R_Developing.rewardsx;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.R_Developing.rewardsx.Configs.MainConfig;
import net.R_Developing.rewardsx.Configs.MessagesConfig;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    private final Main plugin;
    private final Logger logger;
    private final Path dataDir;

    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;

    private File mainConfigFile;
    private Map<String, Object> mainConfigData;

    private File messagesConfigFile;
    private Map<String, Object> messagesConfigData;

    private File userDataFile;
    private Map<String, Object> userData;

    @Getter
    private File rewardsFile;
    private Map<String, Object> rewardsData;

    private final Yaml yaml;

    public Config(Main plugin, MainConfig mainConfig, MessagesConfig messagesConfig, Path dataDir) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataDir = dataDir;
        this.mainConfig = mainConfig;
        this.messagesConfig = messagesConfig;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);

        loadConfigs();
        loadUserData();
        checkMissing();
    }

    private void saveDefaultIfMissing(String name) {
        File target = dataDir.resolve(name).toFile();

        if(!dataDir.toFile().exists() && !dataDir.toFile().mkdirs()) {
            logger.error("Failed to create plugin data folder: {}", dataDir);
            return;
        }

        if (!target.exists()) {
            logger.info("Saving default config: {}", name);
            try (InputStream in = Main.class.getResourceAsStream("/" + name)) {
                if (in != null) {
                    try (OutputStream out = new FileOutputStream(target)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                } else {
                    logger.warn("Could not find resource in JAR: {}", name);
                }
            } catch (IOException e) {
                logger.error("Failed to save default config {}: {}", name, e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(File file) {
        try(FileInputStream fis = new FileInputStream(file)) {
            Object data = yaml.load(fis);
            if(data instanceof Map)
                return (Map<String, Object>) data;
        } catch(IOException e) {
            logger.error("Failed to load config: {}", file.getName());
        }
        return new HashMap<>();
    }

    private void saveYaml(Map<String, Object> data, File file) {
        try(FileWriter writer = new FileWriter(file)) {
            yaml.dump(data, writer);
        } catch(IOException e) {
            logger.warn("Could not save {}: {}", file.getName(), e.getMessage());
        }
    }

    public void loadConfigs() {
        String mainPath = "main.yml";
        saveDefaultIfMissing(mainPath);
        mainConfigFile = dataDir.resolve(mainPath).toFile();
        mainConfigData = loadYaml(mainConfigFile);

        String messagesPath = "messages.yml";
        saveDefaultIfMissing(messagesPath);
        messagesConfigFile = dataDir.resolve(messagesPath).toFile();
        messagesConfigData = loadYaml(messagesConfigFile);

        String rewardsPath = "rewards.yml";
        saveDefaultIfMissing(rewardsPath);
        rewardsFile = dataDir.resolve(rewardsPath).toFile();
        rewardsData = loadYaml(rewardsFile);
    }

    public void loadUserData() {
        String userDataPath = "userdata.yml";
        saveDefaultIfMissing(userDataPath);
        userDataFile = dataDir.resolve(userDataPath).toFile();
        userData = loadYaml(userDataFile);
    }

    public Map<String, Object> getMainConfig() { return mainConfigData; }
    public Map<String, Object> getMessagesConfig() { return messagesConfigData; }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getReward(String rewardId) {
        if (rewardsData == null || rewardId == null) return null;

        Object rewardObj = rewardsData.get(rewardId);
        if (rewardObj instanceof Map) {
            return (Map<String, Object>) rewardObj;
        }
        return null;
    }

    public List<String> getRewardCommands(String rewardId) {
        Map<String, Object> reward = getReward(rewardId);
        if(reward == null) return null;

        Object cmds = reward.get("commands");
        if(cmds instanceof List<?> rawList) {
            return rawList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
        }
        if(cmds instanceof String) return Collections.singletonList((String) cmds);

        Object cmd = reward.get("command");
        return cmd == null ? null : Collections.singletonList(cmd.toString());
    }

    public String getRewardServer(String rewardId) {
        Map<String, Object> reward = getReward(rewardId);
        if (reward == null) return null;

        Object serverObj = reward.get("server");
        return serverObj != null ? serverObj.toString() : null;
    }

    public boolean hasReward(String rewardId) {
        return rewardsData != null && rewardsData.containsKey(rewardId);
    }

    public void saveUserId(UUID playerUUID, String userId) {
        if(userData == null) return;
        userData.put(playerUUID.toString(), userId);
        saveUserData();
    }

    public String getUserId(UUID playerUUID) {
        if(userData == null || playerUUID == null) return null;
        Object value = userData.get(playerUUID.toString());
        return value == null ? null : value.toString();
    }

    public Player getPlayerById(String userId) {
        if(userData == null || userId == null) return null;
        for(String key : userData.keySet()) {
            if(userId.equals(String.valueOf(userData.get(key)))) {
                try {
                    UUID uuid = UUID.fromString(key);
                    return plugin.getProxy().getPlayer(uuid).orElse(null);
                } catch(IllegalArgumentException ignore) {}
            }
        }
        return null;
    }

    public UUID getUUIDById(String userId) {
        if(userData == null || userId == null) return null;
        for(String key : userData.keySet()) {
            if(userId.equals(String.valueOf(userData.get(key)))) {
                try {
                    return UUID.fromString(key);
                } catch(IllegalArgumentException ignore) {}
            }
        }
        return null;
    }

    public List<String> getAllUserIds() {
        List<String> userIds = new ArrayList<>();
        if(userData == null) return userIds;
        for(Object val : userData.values()) {
            if(val != null)
                userIds.add(String.valueOf(val));
        }
        return userIds;
    }

    public void removeUserId(UUID playerUUID) {
        if(userData == null || playerUUID == null) return;
        userData.remove(playerUUID.toString());
        saveUserData();
    }

    public boolean hasUserId(UUID playerUUID) {
        return userData != null && playerUUID != null && userData.containsKey(playerUUID.toString());
    }

    private void saveUserData() {
        if(userData == null || userDataFile == null) return;
        saveYaml(userData, userDataFile);
    }

    // --- Configuration management methods

    public void reloadConfigs() {
        loadConfigs();
        checkMissing();
        logger.info("All configurations reloaded successfully");
    }

    private void checkMissing() {
        updateMissingFields(MainConfig.class, mainConfig, mainConfigData, mainConfigFile, "Main config");
        updateMissingFields(MessagesConfig.class, messagesConfig, messagesConfigData, messagesConfigFile, "Messages config");
    }

    private void updateMissingFields(Class<?> clazz, Object source, Map<String,Object> config, File file, String logName) {
        boolean updated = false;
        for(Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String key = field.getName();
                if(!config.containsKey(key) && field.get(source) != null) {
                    config.put(key, field.get(source));
                    updated = true;
                }
            } catch(IllegalAccessException e) {
                logger.error("Error updating missing field in {}: {}", logName, e.getMessage());
            }
        }
        if (updated) {
            saveYaml(config, file);
            logger.info("{} updated.", logName);
        }
    }
}
