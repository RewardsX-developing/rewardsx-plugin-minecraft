package net.r_developing.rewardsx.config;

import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class BukkitYamlConfig implements PlatformConfig {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public BukkitYamlConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
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
    public boolean getBoolean(String path, boolean bool) {
        return config.getBoolean(path, bool);
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
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config file: " + file.getName());
        }
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }

    @Override
    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void set(String path, String value) {
        config.set(path, value);
    }
}