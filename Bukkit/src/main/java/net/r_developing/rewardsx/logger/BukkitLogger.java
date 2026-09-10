package net.r_developing.rewardsx.logger;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import org.bukkit.ChatColor;

public class BukkitLogger implements PlatformLogger {
    private final Plugin plugin;
    private final PlatformConfig config;

    public BukkitLogger(Plugin plugin, PlatformConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void debug(String message) {
        if(config != null && config.getBoolean("debug", false)){
            plugin.getLogger().info(ChatColor.AQUA + "[DEBUG] " + ChatColor.WHITE + message);
        }
    }

    @Override
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

}