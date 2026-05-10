package net.R_Developing.rewardsx;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

public class Version {
    private final Main plugin;
    private final Fetcher fetcher;
    private final Messager messager;
    private final Logger logger;

    public Version(Main plugin, Fetcher fetcher, Messager messager) {
        this.plugin = plugin;
        this.fetcher = fetcher;
        this.messager = messager;
        this.logger = plugin.getLogger();
    }

    public void checkVersion(Player player) {
        String current = currentVersion();
        if(!current.contains(fetcher.latestVersion)) {
            if(player != null) {
                player.sendMessage(messager.custom(outOfDate()));
            } else {
                logger.info(toAnsi(outOfDate()));
            }
        }
    }

    public String currentVersion() {
        return plugin.getProxy().getPluginManager()
            .fromInstance(plugin)
            .map(c -> c.getDescription().getVersion().orElse("unknown"))
            .orElse("unknown");
    }

    private String outOfDate() {
        return String.format(
            messager.get("outOfDate"),
            fetcher.latestVersion,
            currentVersion(),
            "https://www.spigotmc.org/resources/rewardsx-%E2%AD%90-%E2%80%A2-best-rewards-system-spigot-bungeecord-and-velocity-support.121867/",
            "https://modrinth.com/plugin/rewardsx/version/4dMw3uIl"
        );
    }

    private static String toAnsi(String msg) {
        return msg.replace("§0", "\u001B[30m").replace("§1", "\u001B[34m").replace("§2", "\u001B[32m")
            .replace("§3", "\u001B[36m").replace("§4", "\u001B[31m").replace("§5", "\u001B[35m")
            .replace("§6", "\u001B[33m").replace("§7", "\u001B[37m").replace("§8", "\u001B[90m")
            .replace("§9", "\u001B[94m").replace("§a", "\u001B[92m").replace("§b", "\u001B[96m")
            .replace("§c", "\u001B[91m").replace("§d", "\u001B[95m").replace("§e", "\u001B[93m")
            .replace("§f", "\u001B[97m").replace("§r", "\u001B[0m");
    }
}