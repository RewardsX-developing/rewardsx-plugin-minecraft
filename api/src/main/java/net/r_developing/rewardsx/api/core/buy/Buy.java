package net.r_developing.rewardsx.api.core.buy;

import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.api.core.Platform;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.rewards.RewardCommand;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the purchase and redemption flow for rewards.
 * <p>
 * send() is called from /rewardsx buy (either with a reward name or to open the GUI).
 * confirm() is called by the webhook listener when the backend confirms a purchase
 * has completed and is ready to grant. This class handles offline players too - if a
 * command marked requireOnline is pending, it blocks until they log in; otherwise
 * it executes immediately.
 * <p>
 * Commands are sourced either from the backend (preferred) or fall back to the
 * local rewards.yml config.
 */
public class Buy {
    /** HTTP POST/PUT client for webhook-style confirm callbacks. */
    private final Api api;
    /** Platform detection (proxy vs local, etc.). */
    private final Platform platform;
    /** Local config files (rewards.yml, config.yml). */
    private final Config config;
    /** Localized messages for player feedback. */
    private final Messager messager;
    /** Console logger. */
    private final PlatformLogger logger;
    /** Access to the running server - used to look up players by name or UUID. */
    private final RServer rServer;
    /** Sends plugin messages across the proxy network to other servers. */
    private final ProxySender proxySender;
    /** Schedules tasks on the main server thread. */
    private final PlatformScheduler scheduler;
    /** Executes commands as the console (with player context substituted in). */
    private final PlatformCommandExecutor commandExecutor;

    /** Plain constructor injection - no logic here, just dependency wiring. */
    public Buy(PlatformAdapter adapter, RServer server, Api api, Platform platform,
               Config config, Messager messager, ProxySender proxySender,
               PlatformScheduler scheduler, PlatformCommandExecutor commandExecutor) {
        this.rServer = server;
        this.api = api;
        this.platform = platform;
        this.config = config;
        this.messager = messager;
        this.logger = adapter.getLogger();
        this.proxySender = proxySender;
        this.scheduler = scheduler;
        this.commandExecutor = commandExecutor;
    }

    /**
     * Called when a player runs /rewardsx buy [reward-name].
     * <p>
     * If on a local backend, this just prepares a payload (currently stubbed - TODO).
     * If on a proxy, it forwards the request to the player's connected backend server
     * via plugin message, which will handle the purchase flow there.
     *
     * @param player     the player initiating the purchase
     * @param rewardName optional reward name; if null, the full GUI is opened instead
     */
    public void send(RPlayer player, String rewardName) {
        if (!platform.isProxy()) {
            // Local backend path - build the request payload.
            String quantity = "1";
            String platformId = platform.getId();
            String userId = config.getUserId(player.getUniqueId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", rewardName);
            payload.put("quantity", quantity);
            payload.put("platform", platformId);
            payload.put("userid", userId);

            // TODO: get the reward id to direct the player to the web purchase page.
        } else {
            // Proxy path - ask the player's backend server to handle it.
            proxySender.sendCommand(player, "BUY", rewardName);
        }
    }

    /**
     * Called by the webhook listener on the backend when a purchase is confirmed
     * and ready to grant.
     * <p>
     * Validates the player is online (if any command requires it), resolves their name,
     * confirms the purchase with the backend, and then executes the reward commands
     * either immediately (if online) or marks them offline for later execution.
     *
     * @param userId        the backend user ID from the transaction
     * @param transactionId  unique ID for this purchase
     * @param username      fallback player name if the user is not online
     * @param commands      list of reward commands to execute (from backend)
     * @param onComplete    callback to run when the grant is done (success or failure)
     */
    public void confirm(String userId, String transactionId, String username, List<RewardCommand> commands, Runnable onComplete) {
        // Try to find the player online by user ID, then by username as fallback.
        RPlayer online = config.getPlayerById(userId);
        if (online == null && username != null) {
            online = rServer.getPlayerExact(username);
        }

        // Prefer the online player object, else fall back to the username from the backend.
        String targetName = (online != null) ? online.getPlayerName() : username;
        // Check if any command in the list has the requireOnline flag set.
        boolean needsOnline = commands.stream().anyMatch(RewardCommand::requireOnline);
        // Determine if the resolved player is currently offline.
        boolean isPlayerOffline = (online == null || !online.isOnline());

        // Reject the purchase if it needs the player online, but they are not.
        if (isPlayerOffline && needsOnline) {
            logger.warning("Cannot execute reward " + transactionId + " for user " + userId + " (" + username + "): player offline but a command requires them to be online.");
            onComplete.run();
            return;
        }

        // Fail if we could not resolve a player name at all (no username, no online player).
        if (targetName == null) {
            logger.warning("Cannot resolve a player name for transaction " + transactionId + " / user " + userId);
            onComplete.run();
            return;
        }

        // Build the payload to confirm the purchase on the backend.
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", transactionId);
        payload.put("user", userId);
        payload.put("token", config.getMainConfig().getString("platform_key"));

        // Capture final references for use in the async callback.
        final RPlayer finalOnline = online;
        final String finalName = targetName;

        // Send the confirmation to the backend - async.
        api.send("POST", "complete-buy", payload, result -> {
            if (result == null) {
                logger.warning("No response from server for transaction " + transactionId);
                onComplete.run();
                return;
            }

            boolean success = Boolean.parseBoolean(Objects.toString(result.get("success"), "false"));

            // The callback runs off the main thread, so schedule the actual reward
            // execution back onto it (commands may touch the server).
            scheduler.runSync(() -> {
                if (success) {
                    // Extract the command strings from the RewardCommand objects.
                    List<String> rawCommandStrings = commands.stream()
                            .map(RewardCommand::command)
                            .collect(Collectors.toList());

                    executeRewardCommands(transactionId, finalName, finalOnline, rawCommandStrings);
                    // Notify the player if they are still online.
                    if (finalOnline != null && finalOnline.isOnline()) {
                        finalOnline.sendMessage(messager.get("rewardReceived"));
                    }
                } else {
                    logger.warning("complete-buy failed for transaction " + transactionId);
                }

                onComplete.run();
            });
        });
    }

    /**
     * Executes all the commands associated with a reward grant.
     * <p>
     * Commands come from the backend (preferred). If the backend provided no commands,
     * fall back to the local rewards.yml config. Each command is run as console with
     * the player name substituted in placeholders.
     *
     * @param rewardId        the reward ID (used for config fallback lookup)
     * @param playerName      the player's name (used in command placeholders)
     * @param online          the online player object, if available (maybe null)
     * @param backendCommands the commands from the backend (maybe null or empty)
     */
    private void executeRewardCommands(String rewardId, String playerName, RPlayer online, List<String> backendCommands) {
        List<String> commandsToExecute = backendCommands;

        // If the backend gave us no commands, try the local config.
        // This delegates to a helper method that safely extracts the list from config.
        if (commandsToExecute == null || commandsToExecute.isEmpty()) {
            commandsToExecute = getCommandsFromConfig(rewardId);
        }

        if (commandsToExecute != null && !commandsToExecute.isEmpty()) {
            // Execute each command - replace Minecraft color codes (&) with section symbols (§).
            for (String cmd : commandsToExecute) {
                cmd = cmd.replace("&", "§");
                commandExecutor.dispatchConsoleCommand(cmd, online, playerName);
            }
        } else {
            // No commands found anywhere.
            if (online != null) {
                online.sendMessage(messager.get("actionNotFound"));
            }
            logger.warning("Action for reward " + rewardId + " not found, please configure it on the web dashboard!");
        }
    }

    /**
     * Safely extracts the command list for a reward ID from the local config.
     * <p>
     * This helper method handles the messiness of config file access - null checks,
     * type validation (the config value might not be a List), and graceful fallback
     * to an empty list if anything goes wrong. It also filters out non-String elements
     * in case the YAML config has mixed types.
     *
     * @param rewardId the reward ID to look up (e.g. "rank-vip")
     * @return a list of command strings, or an empty list if not found or invalid
     */
    private List<String> getCommandsFromConfig(String rewardId) {
        // Get the rewards config section - may be null if not yet loaded.
        var rewardsConfig = config.getRewardsConfig();
        if (rewardsConfig == null) {
            return List.of();
        }

        // Look up the config entry by key (e.g. "rank-vip.commands").
        Object value = rewardsConfig.get(rewardId + ".commands");
        if (value == null) {
            // Key doesn't exist - no commands configured for this reward.
            return List.of();
        }

        // Type guard - the config entry must be a List. If someone put a String or
        // other type in the YAML, reject it and return empty.
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        // Copy the List and filter to Strings only, ignoring any bad entries.
        // This protects against YAML entries like:
        //   rank-vip.commands:
        //     - "give [player] diamond"
        //     - 42                       <-- accidentally an int
        //     - "say hello"
        List<String> result = new ArrayList<>(list.size());
        for (Object elem : list) {
            if (elem instanceof String s) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Utility to execute a single command on the main thread with a player context.
     * <p>
     * Used by the web dashboard or other integrations to trigger an action
     * without a full purchase flow.
     *
     * @param player the player on whom to run the command
     * @param cmd    the command string (e.g. "give [player] diamond")
     */
    public void executeCommand(RPlayer player, String cmd) {
        if (config.getRewardsConfig() == null) return;

        if (cmd != null && player != null) {
            // Prepare the command - replace Minecraft color codes.
            final String resolvedCmd = cmd.replace("&", "§");

            // Schedule it onto the main thread. Note the inline lambda here - equivalent
            // to the earlier version, just more concise.
            scheduler.runSync(() -> commandExecutor.dispatchConsoleCommand(resolvedCmd, player, player.getPlayerName()));
        } else {
            if (player != null)
                player.sendMessage(messager.get("actionNotFound"));
            logger.warning("Action not found, or player is invalid!");
        }
    }
}