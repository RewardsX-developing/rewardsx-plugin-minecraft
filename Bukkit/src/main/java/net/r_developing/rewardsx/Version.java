package net.r_developing.rewardsx;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class Version {
    private final Fetcher fetcher;
    private final Messager messager;

    public Version(Fetcher fetcher, Messager messager) {
        this.fetcher = fetcher;
        this.messager = messager;
    }

    public void checkVersion(CommandSender sender) {
        if(!fetcher.latestVersion.contains(currentVersion()))
            if(sender != null) sender.sendMessage(outOfDate());
    }

    public String currentVersion() {
        return Main.instance.getVersion();
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