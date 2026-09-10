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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Platform-agnostic handler for the /rewardsx command.
 * <p>
 * This class holds the actual command logic once; the Bukkit/Spigot, BungeeCord
 * and Velocity modules each wrap it with their own CommandExecutor and forward
 * calls here. Nothing in this file touches a server-specific API directly -
 * everything goes through the abstractions injected in the constructor
 * (RPlayer, PlatformGUI, PlatformLogger, ProxySender, ...).
 * <p>
 * Two entry points:
 *   - executeConsole(...) for the server console (no player attached)
 *   - execute(...)        for in-game players
 */
public class CoreCommands {
    /** Opens the rewards GUI. Implementation differs per platform (Bukkit inventory, etc.). */
    private final PlatformGUI rewardsGUI;
    /** Localized message lookup - messager.get("key") returns the translated string. */
    private final Messager messager;
    /** Config file access: reading, reloading, and storing platform credentials. */
    private final Config config;
    /** Version info + update checks against the RewardsX backend. */
    private final Version version;
    /** Abstraction over the running server: type detection, validity checks, startup. */
    private final Platform platform;
    /** HTTP client for calls to the RewardsX API. */
    private final Fetcher fetcher;
    /** Handles the purchase/redeem flow for a named reward. */
    private final Buy buy;
    /** Console logger (platform-independent). */
    private final PlatformLogger logger;
    /** Sends plugin messages across the proxy (BungeeCord/Velocity) to a backend server. */
    private final ProxySender proxySender;

    /**
     * True once the backend has confirmed this server is a registered/linked platform.
     * Written from an async callback in checkPlatformOnStartup(), read from command
     * threads - hence volatile. Starts false, so the plugin boots in "limited mode"
     * until the check comes back.
     */
    private volatile boolean platformValid = false;

    /**
     * True when running on a proxy (BungeeCord/Velocity) rather than a backend
     * game server. Determines whether the GUI is opened locally or requested
     * over the proxy channel. Also, volatile - refreshed on reload.
     */
    private volatile boolean isProxy = false;

    /** Plain constructor injection - no logic, just wiring up the dependencies. */
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

    /**
     * Called once during plugin enable to prime the cached state flags.
     * <p>
     * The isValid() call is asynchronous (it hits the network), so platformValid
     * is set later from the callback. Any command run in the meantime sees
     * platformValid == false and falls back to limited mode.
     */
    public void checkPlatformOnStartup() {
        this.isProxy = platform.isProxyOrBungee();

        platform.isValid(valid -> {
            this.platformValid = valid;
            if (!valid) {
                // Not fatal: the plugin keeps running, but only reload/version work.
                logger.warning("Platform invalid. Limited mode enabled.");
            }
        });
    }

    /**
     * Console variant of the command. Only the subcommands that make sense without
     * a player are implemented; everything else is rejected.
     * <p>
     *
     * @return true if the command was recognized and handled.
     */
    public boolean executeConsole(String[] args) {

        // Bare "/rewardsx" with no arguments -> print the short console help.
        if (args.length == 0) {
            sendMinimalHelpConsole();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "secret":
                // Links this game server to a RewardsX dashboard account using a secret key.
                // Console-only on purpose - the key should never be typed into in-game chat,
                // where it would be visible in chat logs and to other players.
                if (args.length != 2) {
                    logger.warning("Usage: /rewardsx secret <key>");
                    return true;
                }

                String secretKey = args[1];
                logger.info("Verifying secret key on RewardsX...");

                // Async HTTP call. thenAccept runs on the completing thread, not on the main
                // server thread, so nothing here may touch the server API directly.
                fetcher.authenticateServer(secretKey).thenAccept(response -> {
                    // A response containing "platform_id" means the key was accepted.
                    if (response != null && response.containsKey("platform_id")) {
                        String platformName = (String) response.get("name");

                        // Persist the key so it is reused on the next startup.
                        config.setPlatformCredentials("platform_key", secretKey);

                        this.isProxy = platform.isProxyOrBungee();

                        logger.info("Successfully connect game server with name: " + platformName);
                    } else {
                        logger.warning("Error: Wrong Token of GameServer.");
                    }
                });
                return true;

            case "reload":
            case "rel":
                // Re-read config files, restart the platform connection, and refresh
                // the cached flags. platformValid is updated asynchronously again.
                config.reloadConfigs();
                fetcher.reload();
                platform.checkAndStart(fetcher, messager, version);
                this.isProxy = platform.isProxyOrBungee();
                platform.isValid(valid -> this.platformValid = valid);
                logger.info("Config reloaded from console.");
                return true;

            case "version":
            case "ver":
                logger.info("Running version: " + version.currentVersion());
                return true;

            default:
                // Anything else (e.g. "buy") needs a player context.
                logger.warning("This command is only available for players.");
                return false;
        }
    }

    /** Short usage list printed to the console. */
    private void sendMinimalHelpConsole() {
        logger.info("--- RewardsX Help [Console] ---");
        logger.info("/rewardsx secret <key> - Link this server to the web dashboard");
        logger.info("/rewardsx reload - Reload config");
        logger.info("/rewardsx version - Check version");
    }

    /**
     * In-game variant of the command.
     *
     * @param sender the player (wrapped in the platform-neutral RPlayer type)
     * @param args   subcommand and its arguments
     * @return always true once the command is enabled - the platform wrapper uses
     *         this to decide whether to print its own usage message.
     */
    public boolean execute(RPlayer sender, String[] args) {
        // Limited mode = the backend has not (yet) confirmed this server.
        // Reward-related subcommands are blocked while in this state.
        boolean limitedMode = !platformValid;

        // Bare "/rewardsx" -> help screen, which version depends on the mode.
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
                // Admin-only. Same refresh sequence as the console version.
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
                    // First line: the version currently running.
                    sender.sendMessage(String.format(messager.get("currentVersion"), version.currentVersion()));
                    // Then an async check against the update endpoint, which messages
                    // the player again if a newer build exists.
                    version.checkVersion(sender);
                } else {
                    sender.sendMessage(messager.get("noPermission"));
                }
                break;

            case "buy":
            case "purchase":
                // Blocked until the server is verified, since rewards come from the backend.
                if (limitedMode) {
                    sender.sendMessage(messager.get("platformNotReady"));
                    return true;
                }

                // Guard against a non-player sender being routed here by mistake.
                if (sender.isPlayer()) {
                    if (args.length > 1) {
                        // "/rewardsx buy <name>" - skip the GUI and go straight to that reward.
                        buy.send(sender, args[1]);
                    } else {
                        // No reward named - open the browse GUI.
                        if (isProxy) {
                            // On a proxy there is no inventory to open, so ask the
                            // backend server the player is on to open it via plugin message.
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
                // Unknown subcommand - show help rather than an error.
                //
                // NOTE: "connect" is advertised in sendHelp() and offered in tab
                // completion, but there is no case for it here, so it lands in this
                // default branch and just reprints the help.
                if (limitedMode) {
                    sendMinimalHelp(sender);
                } else {
                    sendHelp(sender);
                }
        }
        return true;
    }

    /**
     * Tab completion for the first argument only.
     * <p>
     * Returns an empty list for deeper arguments (so "/rewardsx buy <tab>" suggests
     * nothing). "buy" is only offered once the platform is verified.
     * Note this does not filter by permission or by isCommandsEnabled.
     */
    public List<String> getTabCompletions(String[] args) {
        if (args.length != 1) return new ArrayList<>();

        List<String> suggestions = new ArrayList<>(Arrays.asList("reload", "version"));
        if (platformValid) {
            suggestions.add("buy");
        }

        // Prefix-match what the player has typed so far (case-insensitive), then sort.
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    /** "rewardsx.admin" acts as a wildcard that grants every specific permission. */
    private boolean hasPermission(RPlayer sender, String permission) {
        return sender.hasPermission("rewardsx.admin") || sender.hasPermission(permission);
    }

    /**
     * Full help screen shown when the platform is verified.
     * The section codes are Minecraft color/format codes (§8 dark gray, §9 blue,
     * §b aqua, §f white, §7 gray, §l bold).
     */
    private void sendHelp(RPlayer sender) {
        // Header tells the admin which side of the network they are on.
        String platformName = isProxy ? "Proxy" : "Bukkit";
        sender.sendMessage("§8--- §9RewardsX Help §8[§b" + platformName + "§8] ---");
        sender.sendMessage("");
        sender.sendMessage("§8§l• §b/rewardsx buy §f[name] §8- §7Open rewards GUI");
        sender.sendMessage("§8§l• §b/rewardsx reload §8- §7Reload config");
        sender.sendMessage("§8§l• §b/rewardsx version §8- §7Check version");
    }

    /**
     * Reduced help screen for limited mode: only the two subcommands that still
     * work are listed, plus a hint on how to recover (link the server, then reload).
     */
    private void sendMinimalHelp(RPlayer sender) {
        sender.sendMessage("§8--- §9RewardsX Help §8[§cLimited§8] ---");
        sender.sendMessage("");
        sender.sendMessage("§8§l• §b/rewardsx reload §8- §7Reload config (Admin)");
        sender.sendMessage("§8§l• §b/rewardsx version §8- §7Check version");
        sender.sendMessage("");
        sender.sendMessage("§cPlatform invalid. Use /rewardsx reload");
    }
}