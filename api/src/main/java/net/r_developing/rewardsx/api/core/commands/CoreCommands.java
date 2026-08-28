package net.r_developing.rewardsx.api.core.commands;

import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.api.core.updater.Version;
import net.r_developing.rewardsx.api.core.Platform;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

public class CoreCommands {
    private final PlatformGUI rewardsGUI;
    private final Messager messager;
    private final Config config;
    private final Version version;
    private final Platform platform;
    private final Fetcher fetcher;
    private final Buy buy;
    private final PlatformLogger logger;
    private final ProxySender proxySender;

    private volatile boolean platformValid = false;
    private volatile boolean isProxy = false;
    private boolean isCommandsEnabled = true;

    public CoreCommands(PlatformGUI rewardsGUI, Messager messager, Config config, Version version,
                        Platform platform, Fetcher fetcher, Buy buy,
                        PlatformLogger logger, ProxySender proxySender) {
        this.rewardsGUI = rewardsGUI;
        this.messager = messager;
        this.config = config;
        this.version = version;
        this.platform = platform;
        this.fetcher = fetcher;
        this.buy = buy;
        this.logger = logger;
        this.proxySender = proxySender;
    }

    public void checkPlatformOnStartup() {
        this.isProxy = platform.isProxyOrBungee();
        platform.isValid(valid -> {
            this.platformValid = valid;
            if (!valid) {
                logger.warning("Platform invalid. Limited mode enabled.");
            }
        });

        this.isCommandsEnabled = platform.isRewardsXCommandsEnabled();
    }

    public boolean executeConsole(String name, String[] args) {

        if (args.length == 0) {
            sendMinimalHelpConsole();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
            case "rel":
                config.reloadConfigs();
                platform.checkAndStart(fetcher, messager, version);
                this.isProxy = platform.isProxyOrBungee();
                platform.isValid(valid -> this.platformValid = valid);
                logger.info("Config reloaded from console.");
                return true;

            case "connect":
                logger.warning("The 'connect' command is only available for players in-game.");
                return true;

            case "version":
            case "ver":
                logger.info("Running version: " + version.currentVersion());
                return true;

            default:
                logger.warning("This command is only available for players.");
                return false;
        }
    }

    private void sendMinimalHelpConsole() {
        logger.info("--- RewardsX Help [Console] ---");
        logger.info("/rewardsx reload - Reload config");
        logger.info("/rewardsx version - Check version");
    }

    public boolean execute(RPlayer sender, String[] args) {
        boolean limitedMode = !platformValid;

        if(!isCommandsEnabled){
            sender.sendMessage(ChatColor.RED + "Commands are disabled. Enable it on config!");
            return false;
        }

        if (args.length == 0) {
            if (limitedMode) {
                sendMinimalHelp(sender);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
            case "rel":
                if (hasPermission(sender, "rewardsx.reload")) {
                    config.reloadConfigs();
                    platform.checkAndStart(fetcher, messager, version);
                    this.isProxy = platform.isProxyOrBungee();
                    platform.isValid(valid -> this.platformValid = valid);
                    sender.sendMessage(messager.get("reload"));
                } else {
                    sender.sendMessage(messager.get("noPermission"));
                }
                break;

            case "version":
            case "ver":
                if (hasPermission(sender, "rewardsx.version")) {
                    sender.sendMessage(String.format(messager.get("currentVersion"), version.currentVersion()));
                    version.checkVersion(sender);
                } else {
                    sender.sendMessage(messager.get("noPermission"));
                }
                break;

            case "connect":
                if (limitedMode) {
                    sender.sendMessage(messager.get("platformNotReady"));
                    return true;
                }

                if (!sender.isPlayer()) {
                    sender.sendMessage(messager.get("onlyPlayer"));
                    return true;
                }

                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /rewardsx connect <code>");
                    return true;
                }

                String code = args[1].toUpperCase();
                sender.sendMessage(ChatColor.YELLOW + "Connecting your account to web interface...");

                fetcher.linkAccount(sender.getUniqueId(), sender.getPlayerName(), code).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(ChatColor.GREEN + "Account connected successfully!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid or expired connection code. Please generate a new one on the web interface.");
                    }
                });
                break;

            case "buy":
            case "purchase":
                if (limitedMode) {
                    sender.sendMessage(messager.get("platformNotReady"));
                    return true;
                }
                if (sender.isPlayer()) {
                    if (args.length > 1) {
                        buy.send(sender, args[1]);
                    } else {
                        if (isProxy) {
                            proxySender.sendCommand(sender, "OPENGUI", "");
                        } else {
                            rewardsGUI.open(sender);
                        }
                    }
                } else {
                    sender.sendMessage(messager.get("onlyPlayer"));
                }
                break;

            default:
                if (limitedMode) {
                    sendMinimalHelp(sender);
                } else {
                    sendHelp(sender);
                }
        }
        return true;
    }

    public List<String> getTabCompletions(String[] args) {
        if (args.length != 1) return new ArrayList<>();

        List<String> suggestions = new ArrayList<>(Arrays.asList("reload", "version"));
        if (platformValid) {
            suggestions.add("buy");
            suggestions.add("connect");
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean hasPermission(RPlayer sender, String permission) {
        return sender.hasPermission("rewardsx.admin") || sender.hasPermission(permission);
    }

    private void sendHelp(RPlayer sender) {
        String platformName = isProxy ? "Proxy" : "Bukkit";
        sender.sendMessage("§8--- §9RewardsX Help §8[§b" + platformName + "§8] ---");
        sender.sendMessage("");
        sender.sendMessage("§8§l• §b/rewardsx buy §f[name] §8- §7Open rewards GUI");
        sender.sendMessage("§8§l• §b/rewardsx connect §f<code> §8- §7Link account to web interface");
        sender.sendMessage("§8§l• §b/rewardsx reload §8- §7Reload config");
        sender.sendMessage("§8§l• §b/rewardsx version §8- §7Check version");
    }

    private void sendMinimalHelp(RPlayer sender) {
        sender.sendMessage("§8--- §9RewardsX Help §8[§cLimited§8] ---");
        sender.sendMessage("");
        sender.sendMessage("§8§l• §b/rewardsx reload §8- §7Reload config (Admin)");
        sender.sendMessage("§8§l• §b/rewardsx version §8- §7Check version");
        sender.sendMessage("");
        sender.sendMessage("§cPlatform invalid. Use /rewardsx reload");
    }
}