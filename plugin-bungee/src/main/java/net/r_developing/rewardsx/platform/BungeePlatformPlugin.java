package net.r_developing.rewardsx.platform;

import net.md_5.bungee.api.plugin.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformPlugin;

public class BungeePlatformPlugin implements PlatformPlugin {
    private final Plugin plugin;

    public BungeePlatformPlugin(Plugin plugin) {
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