package net.r_developing.rewardsx.config;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

public class BungeeYamlConfig implements PlatformConfig {
    private final Plugin plugin;
    private final File file;
    private Configuration config;

    public BungeeYamlConfig(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            // Copia il file dalle risorse del jar (Bungee non ha saveResource)
            try {
                if (plugin.getResourceAsStream(fileName) != null) {
                    Files.copy(plugin.getResourceAsStream(fileName), file.toPath());
                } else {
                    // Se non esiste nel jar, crea un file vuoto
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create config file: " + file.getName() + " - " + e.getMessage());
            }
        }

        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load config file: " + file.getName() + " - " + e.getMessage());
            this.config = new Configuration();
        }
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public int getInt(String path, int i) {
        return config.getInt(path, i);
    }

    @Override
    public boolean contains(String key) {
        return config.contains(key);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }

    @Override
    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config file: " + file.getName() + " - " + e.getMessage());
        }
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (!deep) {
            // Chiavi di primo livello: config.getKeys() restituisce Collection<String>
            return new java.util.HashSet<>(config.getKeys());
        }

        // deep == true: chiavi ricorsive con path completo (es. "section.sub.key")
        Set<String> keys = new java.util.HashSet<>();
        collectKeys(config, "", keys);
        return keys;
    }

    private void collectKeys(Configuration section, String prefix, Set<String> keys) {
        for (String key : section.getKeys()) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            keys.add(fullKey);

            Object value = section.get(key);
            if (value instanceof Configuration) {
                collectKeys((Configuration) value, fullKey, keys);
            }
        }
    }

    @Override
    public void reload() {
        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not reload config file: " + file.getName() + " - " + e.getMessage());
        }
    }

    @Override
    public void set(String path, String userId) {
        config.set(path, userId);
    }
}