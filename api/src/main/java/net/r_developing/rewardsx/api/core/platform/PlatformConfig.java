package net.r_developing.rewardsx.api.core.platform;

import java.util.Set;

/**
 * Astrae la lettura dei file di configurazione (messages.yml / messages.toml)
 */
public interface PlatformConfig {
    String getString(String path);
    Set<String> getKeys(boolean b);
    void reload();

    void set(String path, String value);

    void set(String path, Object value);

    void save();

    boolean getBoolean(String path, boolean bool);

    Object get(String key);

    boolean getBoolean(String debug);

    int getInt(String path, int i);
}