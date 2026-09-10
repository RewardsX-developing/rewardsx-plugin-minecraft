package net.r_developing.rewardsx.api.core.platform;

import java.util.Set;

/**
 * Platform-independent abstraction for reading and writing configuration files.
 *
 * <p>Abstracts away file format and platform differences. YAML (Bukkit), TOML (Velocity),
 * JSON, or custom formats - the core code doesn't care. Just ask for a value and get it back.
 *
 * <p>Used for:
 *   - messages.yml - localized strings
 *   - config.yml - plugin settings (interval, debug mode, etc.)
 *
 * <p>Implementations are provided by each platform (SpigotConfig wraps Bukkit's
 * FileConfiguration, VelocityConfig wraps a TOML library, etc.).
 */
public interface PlatformConfig {
    /**
     * Retrieves a string value from the config.
     *
     * <p>Navigates nested paths using dot notation.
     * Example: getString("messages.reward.received") looks for:
     *   messages:
     *     reward:
     *       received: "value here"
     *
     * @param path the config path (dot-separated for nested keys)
     * @return the string value, or null if not found
     */
    String getString(String path);

    /**
     * Retrieves all top-level keys in the config.
     *
     * <p>Used to iterate over entries for operations like auto-migration
     * (checking for missing keys) or building dynamic lists.
     *
     * @param deep if true, returns keys at all nesting levels;
     *             if false, only top-level keys
     * @return a set of key names (maybe empty)
     */
    Set<String> getKeys(boolean deep);

    /**
     * Reloads the config from disk.
     *
     * <p>Called on /rewardsx reload to pick up changes without a restart.
     * Discards in-memory changes that haven't been saved.
     */
    void reload();

    /**
     * Sets a string value in the config (in memory).
     *
     * <p>Does not persist to disk - call save() after making changes to flush them.
     *
     * @param path  the config path
     * @param value the new string value
     */
    void set(String path, String value);

    /**
     * Sets an object value in the config (in memory).
     *
     * <p>Accepts any serializable type (String, Integer, Boolean, List, Map, etc.).
     * The implementation handles type conversion and serialization.
     * Does not persist to disk - call save() after making changes.
     *
     * @param path  the config path
     * @param value the new value (any type)
     */
    void set(String path, Object value);

    /**
     * Persists all in-memory changes to disk.
     *
     * <p>Called after set() calls to make changes permanent. On Bukkit, writes
     * to the YAML file; on Velocity, writes to TOML; etc.
     */
    void save();

    /**
     * Retrieves a boolean value from the config.
     *
     * <p>Parses the value as a boolean (true/false, yes/no, 1/0, etc. depending
     * on the format). Returns a default if the key doesn't exist or is malformed.
     *
     * @param path the config path
     * @param defaultValue the value to return if the key is not found or invalid
     * @return the boolean value, or defaultValue if not found
     */
    boolean getBoolean(String path, boolean defaultValue);

    /**
     * Retrieves a raw object value from the config.
     *
     * <p>Returns the value as-is without type conversion. Useful when the type
     * is unknown or needs to be checked dynamically (e.g. casting to List or Map).
     *
     * @param key the config path
     * @return the raw value, or null if not found
     */
    Object get(String key);

    /**
     * Retrieves a boolean value from the config (no default override).
     *
     * <p>Shorthand for getBoolean(path, false). Used for simple flag lookups.
     *
     * @param path the config path (typically just a key name for debug mode)
     * @return the boolean value, or false if not found
     */
    boolean getBoolean(String path);

    /**
     * Retrieves an integer value from the config.
     *
     * <p>Parses the value as an integer. Returns a default if the key doesn't exist
     * or is not a valid number.
     *
     * @param path the config path
     * @param defaultValue the value to return if the key is not found or invalid
     * @return the integer value, or defaultValue if not found
     */
    int getInt(String path, int defaultValue);

    boolean contains(String key);
}