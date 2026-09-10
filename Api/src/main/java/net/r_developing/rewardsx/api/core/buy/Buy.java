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

import static net.r_developing.rewardsx.api.core.network.Api.BASE_URL;

public class Buy {
    private final Api api;
    private final Platform platform;
    private final Config config;
    private final Messager messager;
    private final PlatformLogger logger;
    private final RServer rServer;
    private final ProxySender proxySender;
    private final PlatformScheduler scheduler;
    private final PlatformCommandExecutor commandExecutor;

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

    public void send(RPlayer player, String rewardName) {
        if (!platform.isProxy()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", player.getPlayerName());

            // The backend handles caching and per-player cooldowns now.
            api.send("POST", "user-initialize", payload, response -> {
                if (response == null) {
                    logger.debug("User initialization request failed or returned null");
                    return;
                }

                boolean success = "true".equalsIgnoreCase(String.valueOf(response.get("success")));
                if (success) {
                    String userToken = String.valueOf(response.get("token"));
                    generatePurchaseToken(userToken, rewardName, 1, player);
                } else {
                    String message = String.valueOf(response.get("message"));
                    logger.debug("Failed to initialize user: " + message);
                    player.sendMessage("§c" + message);
                }
            });
        } else {
            proxySender.sendCommand(player, "BUY", rewardName);
        }
    }

    public void generatePurchaseToken(String userToken, String rewardId, int quantity, RPlayer player) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reward_id", rewardId);
        payload.put("quantity", quantity);
        payload.put("username", player.getPlayerName());

        Map<String, String> headers = new HashMap<>();
        headers.put("X-User-Token", userToken);

        api.send("POST", "generate-purchase-token", headers, payload, response -> {
            if (response == null) {
                logger.debug("Purchase token request failed or returned null");
                return;
            }

            boolean valid = "true".equalsIgnoreCase(String.valueOf(response.get("success")));
            if (valid) {
                String purchaseToken = String.valueOf(response.get("token"));
                sendPurchaseLink(purchaseToken, player);
            } else {
                player.sendMessage("§cError: " + response.get("message"));
            }
        });
    }

    public void sendPurchaseLink(String token, RPlayer player){
        String url = BASE_URL + "confirm-purchase/" + token;
        player.sendMessage("§aConfirm purchase here: §e" + url);
    }

    public void confirm(String userId, String transactionId, String username, List<RewardCommand> commands, Runnable onComplete) {
        RPlayer online = rServer.getPlayerExact(username);
        String targetName = (online != null) ? online.getPlayerName() : username;
        boolean needsOnline = commands.stream().anyMatch(RewardCommand::requireOnline);
        boolean isPlayerOffline = (online == null || !online.isOnline());

        if (isPlayerOffline && needsOnline) {
            logger.warning("Cannot execute reward " + transactionId + ": player offline but command requires them.");
            onComplete.run();
            return;
        }

        if (targetName == null) {
            logger.warning("Cannot resolve a player name for transaction " + transactionId);
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
                            .map(RewardCommand::command)
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
        if (backendCommands != null && !backendCommands.isEmpty()) {
            for (String cmd : backendCommands) {
                cmd = cmd.replace("&", "§");
                commandExecutor.dispatchConsoleCommand(cmd, online, playerName);
            }
        } else {
            if (online != null) {
                online.sendMessage(messager.get("actionNotFound"));
            }
            logger.warning("Action for reward " + rewardId + " not found!");
        }
    }

    public void executeCommand(RPlayer player, String cmd) {
        if (cmd != null && player != null) {
            final String resolvedCmd = cmd.replace("&", "§");
            scheduler.runSync(() -> commandExecutor.dispatchConsoleCommand(resolvedCmd, player, player.getPlayerName()));
        } else {
            if (player != null) player.sendMessage(messager.get("actionNotFound"));
            logger.warning("Action not found, or player is invalid!");
        }
    }
}