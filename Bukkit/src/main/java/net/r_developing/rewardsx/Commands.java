package net.r_developing.rewardsx;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, TabCompleter {
    private final RewardsGUI rewardsGUI;
    private final Messager messager;
    private final Config config;
    private final Version version;
    private final QuickConnect quickConnect;
    private final Bits bits;
    private final Platform platform;
    private final Fetcher fetcher;
    private final Buy buy;
    private final Account account;

    private volatile boolean platformValid = false;  // Startup validation
    private volatile boolean isProxy = false;        // Cached proxy state

    public Commands(RewardsGUI rewardsGUI, Messager messager, Config config, Version version,
                    QuickConnect quickConnect, Bits bits, Platform platform, Fetcher fetcher,
                    Buy buy, Account account) {
        this.rewardsGUI = rewardsGUI;
        this.messager = messager;
        this.config = config;
        this.version = version;
        this.quickConnect = quickConnect;
        this.bits = bits;
        this.platform = platform;
        this.fetcher = fetcher;
        this.buy = buy;
        this.account = account;

        // Check both on startup
        checkPlatformOnStartup();
    }

    private void checkPlatformOnStartup() {
        // Check proxy state first (synchronous)
        this.isProxy = platform.isProxy();

        // Then async validation
        platform.isValid(valid -> {
            this.platformValid = valid;
            if (!valid) {
                System.err.println("[RewardsX] Platform invalid. Limited mode enabled.");
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean limitedMode = isProxy || !platformValid;

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
                    platform.checkAndStart(fetcher, rewardsGUI, messager, version, quickConnect, bits, buy, account);
                    this.isProxy = platform.isProxy();
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

            case "buy":
            case "purchase":
                handlePlayerOnlyCommand(sender, () -> {
                    if (limitedMode) {
                        sender.sendMessage(messager.get("platformNotReady"));
                        return;
                    }
                    Player player = (Player) sender;
                    if (quickConnect.isConnected(player)) {
                        if (args.length > 1) {
                            buy.send(player, args[1]);
                        } else {
                            rewardsGUI.open(player);
                        }
                    } else {
                        sender.sendMessage(messager.get("notConnected"));
                    }
                });
                break;

            case "quickconnect":
            case "connect":
                if (limitedMode) {
                    sender.sendMessage(messager.get("platformNotReady"));
                    return true;
                }
                if (args.length > 1) {
                    quickConnect.connect(sender, args[1]);
                } else if (sender instanceof Player player) {
                    quickConnect.sendInsertCode(player);
                }
                break;

            case "disconnect":
            case "quickdisconnect":
                if (limitedMode) {
                    sender.sendMessage(messager.get("platformNotReady"));
                    return true;
                }
                quickConnect.disconnect(sender);
                break;

            case "myaccount":
            case "account":
                handlePlayerOnlyCommand(sender, () -> {
                    if (limitedMode) {
                        sender.sendMessage(messager.get("platformNotReady"));
                        return;
                    }
                    account.sendDetails((Player) sender);
                });
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

    private void handlePlayerOnlyCommand(CommandSender sender, Runnable action) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messager.get("onlyPlayer"));
            return;
        }
        action.run();
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.isOp() || sender.hasPermission("rewardsx.admin") || sender.hasPermission(permission);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return new ArrayList<>();

        List<String> suggestions = new ArrayList<>(Arrays.asList("reload", "version"));
        if (!isProxy && platformValid) {
            suggestions.addAll(Arrays.asList("buy", "connect", "disconnect", "account"));
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8--- §9RewardsX Help §8[§bBukkit§8] ---");
        sender.sendMessage("");
        sender.sendMessage("§8§l• §b/rewardsx buy §f[name] §8- §7Open rewards GUI");
        sender.sendMessage("§8§l• §b/rewardsx connect §f<code> §8- §7Connect account");
        sender.sendMessage("§8§l• §b/rewardsx disconnect §8- §7Disconnect account");
        sender.sendMessage("§8§l• §b/rewardsx account §8- §7View account details");
        sender.sendMessage("§8§l• §b/rewardsx reload §8- §7Reload config");
        sender.sendMessage("§8§l• §b/rewardsx version §8- §7Check version");
    }

    private void sendMinimalHelp(CommandSender sender) {
        sender.sendMessage("§8--- §9RewardsX Help §8[§cLimited§8] ---");
        sender.sendMessage("");
        sender.sendMessage("§8§l• §b/rewardsx reload §8- §7Reload config (Admin)");
        sender.sendMessage("§8§l• §b/rewardsx version §8- §7Check version");
        sender.sendMessage("");
        sender.sendMessage("§c" + (isProxy ? "Proxy mode" : "Platform invalid") + ". Use /rewardsx reload");
    }
}