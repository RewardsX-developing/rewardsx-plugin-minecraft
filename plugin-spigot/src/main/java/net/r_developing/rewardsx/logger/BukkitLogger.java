package net.r_developing.rewardsx.logger;

import net.r_developing.rewardsx.api.Configs.MainConfig;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitLogger implements PlatformLogger {
    private final JavaPlugin plugin;
    private final Config config;

    public BukkitLogger(JavaPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void debug(String message) {
        if(config.getMainConfig().getBoolean("debug", false)){
            plugin.getLogger().info("§b[DEBUG]§r " + message);
        }
    }

    @Override
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }
}