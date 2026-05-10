package net.R_Developing.rewardsx;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commands extends Command implements TabExecutor {

    private final Messager messager;
    private final Config config;
    private final Version version;
    private final QuickConnect quickConnect;
    private final Bits bits;
    private final Platform platform;
    private final Fetcher fetcher;
    private final Buy buy;
    private final ProxySender proxySender;
    private final Account account;

    private volatile boolean platformValid = false;  // Startup validation flag

    public Commands(String name, Messager messager, Config config,
                    Version version, QuickConnect quickConnect, Bits bits, Platform platform,
                    Fetcher fetcher, Buy buy, ProxySender proxySender, Account account) {
        super(name);
        this.messager = messager;
        this.config = config;
        this.version = version;
        this.quickConnect = quickConnect;
        this.bits = bits;
        this.platform = platform;
        this.fetcher = fetcher;
        this.buy = buy;
        this.proxySender = proxySender;
        this.account = account;

        checkPlatformOnStartup();
    }

    private void checkPlatformOnStartup() {
        platform.isValid(valid -> {
            this.platformValid = valid;
            if (!valid) {
                System.err.println("[RewardsX] Platform invalid. Limited mode enabled.");
            }
        });
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean limitedMode = !platformValid;

        if (args.length == 0) {
            if (limitedMode) {
                sendMinimalHelp(sender);
            } else {
                sendHelp(sender);
            }
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
            case "rel":
                if (hasPermission(sender, "rewardsx.reload")) {
                    config.reloadConfigs();
                    messager.reload();
                    platform.checkAndStart(fetcher, messager, version, quickConnect, bits, buy, platform, account);
                    platform.isValid(valid -> this.platformValid = valid);
                    sender.sendMessage(messager.custom(messager.get("reload")));
                } else {
                    sender.sendMessage(messager.custom(messager.get("noPermission")));
                }
                break;

            case "version":
            case "ver":
                if (hasPermission(sender, "rewardsx.version")) {
                    sender.sendMessage(messager.custom(String.format(messager.get("currentVersion"), version.currentVersion())));
                    if (sender instanceof ProxiedPlayer player) {
                        version.checkVersion(player);
                    } else {
                        version.checkVersion(null);
                    }
                } else {
                    sender.sendMessage(messager.custom(messager.get("noPermission")));
                }
                break;

            case "purchase":
            case "buy":
                handlePlayerOnlyCommand(sender, () -> {
                    if (limitedMode) {
                        sender.sendMessage(messager.custom(messager.get("platformNotReady")));
                        return;
                    }
                    ProxiedPlayer player = (ProxiedPlayer) sender;
                    if (config.getUserId(player.getUniqueId()) != null) {
                        if (args.length > 1) {
                            buy.send(player, args[1]);
                        } else {
                            String server = player.getServer().getInfo().getName();
                            proxySender.sendCommand(player, server, "OPENGUI", "");
                        }
                    } else {
                        sender.sendMessage(messager.custom(messager.get("notLogin")));
                    }
                });
                break;

            case "quickconnect":
            case "connect":
                if (limitedMode) {
                    sender.sendMessage(messager.custom(messager.get("platformNotReady")));
                    return;
                }
                if (args.length > 1) {
                    quickConnect.connect(sender, args[1]);
                } else {
                    sender.sendMessage(messager.custom(messager.get("insertCode")));
                }
                break;

            case "quickdisconnect":
            case "disconnect":
                if (limitedMode) {
                    sender.sendMessage(messager.custom(messager.get("platformNotReady")));
                    return;
                }
                quickConnect.disconnect(sender);
                break;

            case "myaccount":
            case "account":
                handlePlayerOnlyCommand(sender, () -> {
                    if (limitedMode) {
                        sender.sendMessage(messager.custom(messager.get("platformNotReady")));
                        return;
                    }
                    account.sendDetails((ProxiedPlayer) sender);
                });
                break;

            default:
                if (limitedMode) {
                    sendMinimalHelp(sender);
                } else {
                    sendHelp(sender);
                }
        }
    }

    private void handlePlayerOnlyCommand(CommandSender sender, Runnable action) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(messager.custom(messager.get("onlyConsole")));
            return;
        }
        action.run();
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission("rewardsx.admin") || sender.hasPermission(permission);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return new ArrayList<>();

        List<String> suggestions = new ArrayList<>(Arrays.asList("reload", "version"));
        if (platformValid) {
            suggestions.addAll(Arrays.asList("buy", "connect", "disconnect", "account"));
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8--- §9RewardsX Help §8[§aProxy§8] ---")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs buy §f[name] §8- §7Buy reward")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs connect §f<code> §8- §7Connect account")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs disconnect §8- §7Disconnect account")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs account §8- §7Account details")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs reload §8- §7Reload config")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs version §8- §7Check version")));
    }

    private void sendMinimalHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8--- §6RewardsX Help §8[§cLimited§8] ---")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs reload §8- §7Reload config (Admin)")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§8§l• §b/rewardsxs version §8- §7Check version")));
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "§cPlatform not ready. Use /rewardsxs reload.")));
    }
}