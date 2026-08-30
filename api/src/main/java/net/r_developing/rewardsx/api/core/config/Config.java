package net.r_developing.rewardsx.api.core.config;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;

import java.lang.reflect.Field;
import java.util.*;

public class Config {
    private final RServer rServer;
    private final PlatformAdapter adapter;
    private final PlatformLogger logger;

    private final Object mainConfigDefaults;
    private final Object messagesConfigDefaults;

    @Getter
    private final PlatformConfig mainConfig;
    @Getter
    private final PlatformConfig messagesConfig;
    @Getter
    private final PlatformConfig rewardsConfig;
    private final PlatformConfig userDataConfig;

    public Config(
            RServer server,
            PlatformAdapter adapter,
            Object mainConfigDefaults,
            Object messagesConfigDefaults,
            PlatformConfig mainConfig,
            PlatformConfig messagesConfig,
            PlatformConfig rewardsConfig,
            PlatformConfig userDataConfig) {

        this.rServer = server;
        this.adapter = adapter;
        this.logger = adapter.getLogger();

        this.mainConfigDefaults = mainConfigDefaults;
        this.messagesConfigDefaults = messagesConfigDefaults;

        this.mainConfig = mainConfig;
        this.messagesConfig = messagesConfig;
        this.rewardsConfig = rewardsConfig;
        this.userDataConfig = userDataConfig;

        checkMissing();

        if (mainConfig.get("proxy") == null) {
            boolean isProxy = adapter.type() == PlatformAdapter.Type.SPIGOT;
            if(isProxy) {
                mainConfig.set("proxy", false);
                mainConfig.save();
            }
        }
    }

    public void saveUserId(UUID playerUUID, String userId) {
        if (userDataConfig == null) return;
        userDataConfig.set(playerUUID.toString(), userId);
        userDataConfig.save();
    }

    public String getUserId(UUID playerUUID) {
        if (userDataConfig == null) return null;
        return userDataConfig.getString(playerUUID.toString());
    }

    public UUID getUUIDById(String userId) {
        if (userDataConfig == null || userId == null) return null;
        for (String key : userDataConfig.getKeys(false)) {
            if (userId.equals(userDataConfig.getString(key))) {
                try { return UUID.fromString(key); }
                catch (IllegalArgumentException e) { return null; }
            }
        }
        return null;
    }

    public RPlayer getPlayerById(String userId) {
        if (userDataConfig == null || userId == null) return null;
        return userDataConfig.getKeys(false).stream()
                .filter(key -> userId.equals(userDataConfig.getString(key)))
                .map(key -> {
                    try { return rServer.getPlayer(UUID.fromString(key)); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
        userDataConfig.save();
    }

    public void reloadConfigs() {
        mainConfig.reload();
        messagesConfig.reload();
        rewardsConfig.reload();
        userDataConfig.reload();
        checkMissing();
    }

    public void setPlatformCredentials(String path, String secretKey) {
        if (mainConfig == null) return;
        mainConfig.set(path, secretKey);
        mainConfig.save();
    }

    private void checkMissing() {
        if (mainConfigDefaults != null) {
            boolean mainUpdated = checkMissingFields(mainConfigDefaults.getClass(), mainConfigDefaults, mainConfig);
            if (mainUpdated) {
                mainConfig.save();
                logger.info("Main config updated with missing keys.");
            }
        }

        if (messagesConfigDefaults != null) {
            boolean msgsUpdated = checkMissingFields(messagesConfigDefaults.getClass(), messagesConfigDefaults, messagesConfig);
            if (msgsUpdated) {
                messagesConfig.save();
                logger.info("Messages config updated with missing keys.");
            }
        }
    }

    private boolean checkMissingFields(Class<?> clazz, Object defaultValuesObj, PlatformConfig targetConfig) {
        boolean updated = false;
        try {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                String key = f.getName();
                if (targetConfig.get(key) == null) {
                    Object defaultValue = f.get(defaultValuesObj);
                    if (defaultValue != null) {
                        targetConfig.set(key, defaultValue);
                        updated = true;
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to check missing config fields: " + e.getMessage());
        }
        return updated;
    }
}