package net.r_developing.rewardsx.api.core.buy;

import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.api.core.Platform;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.rewards.RewardCommand;

import java.util.*;
import java.util.stream.Collectors;

public class Buy {
    private final Fetcher fetcher;
    private final Api api;
    private final Platform platform;
    private final Config config;
    private final Messager messager;
    private final PlatformLogger logger;
    private final RServer rServer;
    private final ProxySender proxySender;
    private final PlatformScheduler scheduler;
    private final PlatformCommandExecutor commandExecutor;

    public Buy(PlatformAdapter adapter, RServer server, Fetcher fetcher, Api api, Platform platform,
               Config config, Messager messager, ProxySender proxySender,
               PlatformScheduler scheduler, PlatformCommandExecutor commandExecutor) {
        this.fetcher = fetcher;
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

    public void send(RPlayer player, String rewardName) {
        if (!platform.isProxy()) {
            String quantity = "1";
            String platformId = platform.getId();
            String userId = config.getUserId(player.getUniqueId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", rewardName);
            payload.put("quantity", quantity);
            payload.put("platform", platformId);
            payload.put("userid", userId);

            // TODO: ottenere reward id per reindirizzarlo al sito web.
        } else {
            proxySender.sendCommand(player, "BUY", rewardName);
        }
    }

    public void confirm(String userId, String transactionId, String username, List<RewardCommand> commands, Runnable onComplete) {
        RPlayer online = config.getPlayerById(userId);
        if (online == null && username != null) {
            online = rServer.getPlayerExact(username);
        }

        String targetName = (online != null) ? online.getPlayerName() : username;
        boolean needsOnline = commands.stream().anyMatch(RewardCommand::isRequireOnline);
        boolean isPlayerOffline = (online == null || !online.isOnline());

        if (isPlayerOffline && needsOnline) {
            logger.warning("Cannot execute reward " + transactionId + " for user " + userId + " (" + username + "): player offline but a command requires them to be online.");
            onComplete.run();
            return;
        }

        if (targetName == null) {
            logger.warning("Cannot resolve a player name for transaction " + transactionId + " / user " + userId);
            onComplete.run();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", transactionId);
        payload.put("user", userId);
        payload.put("token", config.getMainConfig().getString("platform_key"));

        final RPlayer finalOnline = online;
        final String finalName = targetName;

        api.send("POST", "complete-buy", payload, result -> {
            if (result == null) {
                logger.warning("No response from server for transaction " + transactionId);
                onComplete.run();
                return;
            }

            boolean success = Boolean.parseBoolean(Objects.toString(result.get("success"), "false"));

            scheduler.runSync(() -> {
                if (success) {
                    List<String> rawCommandStrings = commands.stream()
                            .map(RewardCommand::getCommand)
                            .collect(Collectors.toList());

                    executeRewardCommands(transactionId, finalName, finalOnline, rawCommandStrings);
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

    private void executeRewardCommands(String rewardId, String playerName, RPlayer online, List<String> backendCommands) {
        List<String> commandsToExecute = backendCommands;

        if (commandsToExecute == null || commandsToExecute.isEmpty()) {
            if (config.getRewardsConfig() != null) {
                commandsToExecute = (List<String>) config.getRewardsConfig().get(rewardId + ".commands");
            }
        }

        if (commandsToExecute != null && !commandsToExecute.isEmpty()) {
            for (String cmd : commandsToExecute) {
                cmd = cmd.replace("&", "§");
                commandExecutor.dispatchConsoleCommand(cmd, online, playerName);
            }
        } else {
            if (online != null) {
                online.sendMessage(messager.get("actionNotFound"));
            }
            logger.warning("Action for reward " + rewardId + " not found, please configure it on the web dashboard!");
        }
    }

    public void executeCommand(RPlayer player, String cmd) {
        if (config.getRewardsConfig() == null) return;

        if (cmd != null && player != null) {
            final String resolvedCmd = cmd.replace("&", "§");

            scheduler.runSync(() -> {
                commandExecutor.dispatchConsoleCommand(resolvedCmd, player, player.getPlayerName());
            });
        } else {
            if (player != null)
                player.sendMessage(messager.get("actionNotFound"));
            logger.warning("Action not found, or player is invalid!");
        }
    }

    public int getBuys(RPlayer player) {
        String userId = config.getUserId(player.getUniqueId());
        if (userId == null) return 0;
        return fetcher.buysList.getOrDefault(userId, 0);
    }
}