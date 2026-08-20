package net.r_developing.rewardsx.api.core.updater;

import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;

public class Version {
    private final Fetcher fetcher;
    private final Messager messager;
    private final PlatformAdapter plugin;

    public Version(Fetcher fetcher, Messager messager, PlatformAdapter plugin) {
        this.fetcher = fetcher;
        this.messager = messager;
        this.plugin = plugin;
    }

    public void checkVersion(RPlayer sender) {
        if (!fetcher.latestVersion.contains(currentVersion())) {
            if (sender != null) {
                sender.sendMessage(outOfDate());
            }
        }
    }

    public String currentVersion() {
        return plugin.getPluginVersion();
    }

    private String outOfDate() {
        return String.format(
                messager.get("outOfDate"),
                fetcher.latestVersion, currentVersion(),
                "https://www.spigotmc.org/resources/rewardsx-%E2%AD%90-%E2%80%A2-best-rewards-system-spigot-bungeecord-and-velocity-support.121867/",
                "https://modrinth.com/plugin/rewardsx/version/4dMw3uIl"
        );
    }
}