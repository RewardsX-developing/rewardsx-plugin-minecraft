package net.R_Developing.rewardsx;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands implements SimpleCommand {
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

    public Commands(Messager messager, Config config,
                    Version version, QuickConnect quickConnect, Bits bits, Platform platform,
                    Fetcher fetcher, Buy buy, ProxySender proxySender, Account account) {
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
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

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
                if (hasPermission(sender, "rewardsx" +
                        ".reload")) {
                    config.reloadConfigs();
                    messager.reload();
                    platform.checkAndStart(fetcher, messager, version, quickConnect, bits, buy, account, platform);
                    platform.isValid(valid -> this.platformValid = valid);
                    sender.sendMessage(messager.getMessage("reload"));
                } else {
                    sender.sendMessage(messager.getMessage("noPermission"));
                }
                break;

            case "version":
            case "ver":
                if (hasPermission(sender, "rewardsx.version")) {
                    sender.sendMessage(messager.custom(String.format(messager.get("currentVersion"), version.currentVersion())));
                    if (sender instanceof Player player) {
                        version.checkVersion(player);
                    } else {
                        version.checkVersion(null);
                    }
                } else {
                    sender.sendMessage(messager.getMessage("noPermission"));
                }
                break;

            case "purchase":
            case "buy":
                handlePlayerOnlyCommand(sender, () -> {
                    if (limitedMode) {
                        sender.sendMessage(messager.getMessage("platformNotReady"));
                        return;
                    }
                    Player player = (Player) sender;
                    if (config.getUserId(player.getUniqueId()) != null) {
                        if (args.length > 1) {
                            buy.send(player, args[1]);
                        } else {
                            player.getCurrentServer().ifPresent(serverConnection -> {
                                String server = serverConnection.getServerInfo().getName();
                                proxySender.sendCommand(player, server, "OPENGUI", "");
                            });
                        }
                    } else {
                        sender.sendMessage(messager.getMessage("notConnected"));
                    }
                });
                break;

            case "quickconnect":
            case "connect":
                if (limitedMode) {
                    sender.sendMessage(messager.getMessage("platformNotReady"));
                    return;
                }
                if (args.length > 1) {
                    quickConnect.connect(sender, args[1]);
                } else {
                    sender.sendMessage(messager.getMessage("insertCode"));
                }
                break;

            case "quickdisconnect":
            case "disconnect":
                if (limitedMode) {
                    sender.sendMessage(messager.getMessage("platformNotReady"));
                    return;
                }
                quickConnect.disconnect(sender);
                break;

            case "myaccount":
            case "account":
                handlePlayerOnlyCommand(sender, () -> {
                    if (limitedMode) {
                        sender.sendMessage(messager.getMessage("platformNotReady"));
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
    }

    private void handlePlayerOnlyCommand(CommandSource sender, Runnable action) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messager.getMessage("onlyConsole"));
            return;
        }
        action.run();
    }

    private boolean hasPermission(CommandSource sender, String permission) {
        return sender.hasPermission("rewardsx.admin") || sender.hasPermission(permission);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 1) return new ArrayList<>();

        List<String> suggestions = new ArrayList<>(Arrays.asList("reload", "version"));
        if (platformValid) {
            suggestions.addAll(Arrays.asList("buy", "connect", "disconnect", "account"));
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .toList();
    }

    private void sendHelp(CommandSource sender) {
        // Header
        sender.sendMessage(Component.text()
                .append(Component.text("--- ", NamedTextColor.DARK_GRAY))
                .append(Component.text("RewardsX Help ", NamedTextColor.BLUE))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Velocity", NamedTextColor.GREEN))
                .append(Component.text("] ---", NamedTextColor.DARK_GRAY)));

        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx buy ", NamedTextColor.AQUA))
                .append(Component.text("<name> ", NamedTextColor.WHITE))
                .append(Component.text("- Open rewards GUI", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx connect ", NamedTextColor.AQUA))
                .append(Component.text("<code> ", NamedTextColor.WHITE))
                .append(Component.text("- Connect account", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx disconnect ", NamedTextColor.AQUA))
                .append(Component.text("- Disconnect account", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx account ", NamedTextColor.AQUA))
                .append(Component.text("- Account details", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx reload ", NamedTextColor.AQUA))
                .append(Component.text("- Reload config", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx version ", NamedTextColor.AQUA))
                .append(Component.text("- Check version", NamedTextColor.GRAY)));
    }

    private void sendMinimalHelp(CommandSource sender) {
        // Header
        sender.sendMessage(Component.text()
                .append(Component.text("--- ", NamedTextColor.DARK_GRAY))
                .append(Component.text("RewardsX Help ", NamedTextColor.BLUE))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Limited", NamedTextColor.RED))
                .append(Component.text("] ---", NamedTextColor.DARK_GRAY)));

        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx reload ", NamedTextColor.AQUA))
                .append(Component.text("- Reload config (Admin)", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text()
                .append(Component.text("/rewardsx version ", NamedTextColor.AQUA))
                .append(Component.text("- Check version", NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("Platform not ready. Use /rewardsx reload", NamedTextColor.RED));
    }
}