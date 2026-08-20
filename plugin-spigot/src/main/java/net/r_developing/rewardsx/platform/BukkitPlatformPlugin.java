package net.r_developing.rewardsx.platform;

import net.r_developing.rewardsx.api.core.platform.PlatformPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlatformPlugin implements PlatformPlugin {
    private final JavaPlugin plugin;

    public BukkitPlatformPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return plugin.getDescription().getName();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public Object getPlugin() {
        return plugin;
    }

}