package net.r_developing.rewardsx.api.core.config;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Centralizes access to all configuration files and player data storage.
 * <p>
 * Manages four YAML configs:
 *   - mainConfig - plugin settings (proxy mode, platform key, etc.)
 *   - messagesConfig - localized strings
 *   - rewardsConfig - reward definitions (which commands grant which rewards)
 *   - userDataConfig - persistent player-to-user-id mappings
 * <p>
 * On init, scans the defaults objects (mainConfigDefaults, messagesConfigDefaults)
 * using reflection to find any keys that are missing from disk, then adds them
 * with their default values. This keeps the configs auto-migrating as new keys
 * are added to the code.
 * <p>
 * Also doubles as a player lookup utility - getPlayerById() finds an online player
 * by their RewardsX user ID, scanning the user data storage.
 */
public class Config {
    /** Access to the running server - used to look up players by UUID. */
    private final RServer rServer;
    /** Platform detection and logger access. */
    private final PlatformAdapter adapter;
    /** Console logger. */
    private final PlatformLogger logger;

    /**
     * Default values for mainConfig, injected at construction.
     * This is a plain Java object (e.g. a record or data class) whose fields
     * are scanned for default key-value pairs. Only used during checkMissing().
     */
    private final Object mainConfigDefaults;

    /**
     * Default values for messagesConfig, injected at construction.
     * Same purpose as mainConfigDefaults but for localized strings.
     */
    private final Object messagesConfigDefaults;

    /**
     * The plugin's main settings file (config.yml).
     * Exposed as @Getter for read access throughout the codebase.
     * Contains: proxy mode, platform key, enabled flags, etc.
     */
    @Getter
    private final PlatformConfig mainConfig;

    /**
     * Localized message strings (messages.yml).
     * Exposed as @Getter. Used by Messager to look up translated strings.
     */
    @Getter
    private final PlatformConfig messagesConfig;

    /**
     * Constructor injection. All four config objects and default value holders
     * are passed in by the platform-specific bootstrap code.
     * <p>
     * Runs checkMissing() immediately to auto-add any new config keys that are
     * missing from disk. Also initializes the proxy flag in mainConfig if it
     * doesn't exist.
     */
    public Config(
            RServer server,
            PlatformAdapter adapter,
            Object mainConfigDefaults,
            Object messagesConfigDefaults,
            PlatformConfig mainConfig,
            PlatformConfig messagesConfig) {

        this.rServer = server;
        this.adapter = adapter;
        this.logger = adapter.getLogger();

        this.mainConfigDefaults = mainConfigDefaults;
        this.messagesConfigDefaults = messagesConfigDefaults;

        this.mainConfig = mainConfig;
        this.messagesConfig = messagesConfig;

        // Scan for and add any missing config keys from the defaults.
        checkMissing();

        // If the proxy flag is not set, initialize it based on the platform type.
        // (Note: the logic seems backwards - SPIGOT means local Bukkit, not proxy,
        // yet it sets proxy=false. This should probably be BungeeCord or similar.)
        if (mainConfig.get("proxy") == null) {
            boolean isProxy = adapter.type() == PlatformAdapter.Type.SPIGOT;
            if (isProxy) {
                mainConfig.set("proxy", false);
                mainConfig.save();
            }
        }
    }

    /**
     * Reloads all four config files from disk.
     * <p>
     * Called on /rewardsx reload - this lets admins edit a config file and apply
     * changes without a full server restart. After reloading, checkMissing() is
     * called again to fill in any default keys that may have been manually deleted.
     */
    public void reloadConfigs() {
        mainConfig.reload();
        messagesConfig.reload();
        checkMissing();
    }

    /**
     * Persists the platform secret key to mainConfig and saves to disk.
     * <p>
     * Called when a server owner runs /rewardsx secret <key> to link the server
     * to their RewardsX account. The key is stored so it persists across restarts.
     *
     * @param path      the config path (usually "platform_key")
     * @param secretKey the authentication key from the RewardsX dashboard
     */
    public void setPlatformCredentials(String path, String secretKey) {
        if (mainConfig == null) return;
        mainConfig.set(path, secretKey);
        mainConfig.save();
    }

    /**
     * Scans the default value objects and fills in any missing keys in the
     * actual config files.
     * <p>
     * This is the auto-migration pattern - if a new version of the plugin adds
     * a new config key, this method detects that it's missing and adds it with
     * the default value. No manual config editing required; users just need to
     * reload or restart.
     */
    private void checkMissing() {
        // Process mainConfig defaults (plugin settings).
        if (mainConfigDefaults != null) {
            boolean mainUpdated = checkMissingFields(mainConfigDefaults.getClass(), mainConfigDefaults, mainConfig);
            if (mainUpdated) {
                mainConfig.save();
                logger.info("Main config updated with missing keys.");
            }
        }

        // Process messagesConfig defaults (localized strings).
        if (messagesConfigDefaults != null) {
            boolean msgsUpdated = checkMissingFields(messagesConfigDefaults.getClass(), messagesConfigDefaults, messagesConfig);
            if (msgsUpdated) {
                messagesConfig.save();
                logger.info("Messages config updated with missing keys.");
            }
        }
    }

    /**
     * Uses reflection to scan a class's fields and populate missing config keys.
     * <p>
     * For each field in the defaults class:
     *   1. Get the field name (this becomes the config key)
     *   2. Check if the key already exists in targetConfig
     *   3. If missing, read the default value from the defaults object and add it
     *   4. Mark that an update occurred (so the config can be saved)
     * <p>
     * Failures (bad field access, etc.) are logged but don't stop the process.
     *
     * @param clazz            the defaults class to scan (e.g. MainConfigDefaults.class)
     * @param defaultValuesObj the instance to read default values from
     * @param targetConfig     the actual config file to update
     * @return true if any keys were added (config needs saving)
     */
    private boolean checkMissingFields(Class<?> clazz, Object defaultValuesObj, PlatformConfig targetConfig) {
        boolean updated = false;
        try {
            // Iterate over all declared fields in the defaults class.
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                String key = f.getName();

                // If the key is missing from the actual config...
                if (targetConfig.get(key) == null) {
                    // Read the default value from the defaults object.
                    Object defaultValue = f.get(defaultValuesObj);
                    // If the default is not null, add it to the config.
                    if (defaultValue != null) {
                        targetConfig.set(key, defaultValue);
                        updated = true;
                    }
                }
            }
        } catch (Exception e) {
            // Reflection errors, field access errors, etc. - log and continue.
            logger.warning("Failed to check missing config fields: " + e.getMessage());
        }
        return updated;
    }
}