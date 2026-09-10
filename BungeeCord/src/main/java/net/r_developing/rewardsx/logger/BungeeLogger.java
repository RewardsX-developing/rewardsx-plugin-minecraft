package net.r_developing.rewardsx.logger;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;

public class BungeeLogger implements PlatformLogger {
    private final Plugin plugin;
    private final PlatformConfig config;

    public BungeeLogger(Plugin plugin, PlatformConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void info(String msg) {
        plugin.getLogger().info(msg);
    }

    @Override
    public void debug(String message) {
        if(config != null && config.getBoolean("debug", false)){
            plugin.getLogger().info("§b[DEBUG]§r " + message);
        }
    }


    @Override
    public void warning(String msg) {
        plugin.getLogger().warning(msg);
    }
}