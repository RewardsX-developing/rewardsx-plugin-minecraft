package net.r_developing.rewardsx.api.core.buy;

import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.api.core.Platform;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.platform.*;

import java.util.*;

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

    public void confirm(String userId, String buyId, String username) {
        RPlayer online = config.getPlayerById(userId);
        if (online == null && username != null) {
            online = rServer.getPlayerExact(username);
        }

        boolean offlineAllowed = config.getMainConfig().getBoolean("offline_commands_enabled");

        String targetName = (online != null) ? online.getPlayerName() : username;

        if (online == null && !offlineAllowed) {
            logger.warning(
                    "Skipping reward " + buyId + " for user " + userId +
                            " (username=" + username + "): player offline and offline_commands_enabled=false");
            return;
        }

        if (targetName == null) {
            logger.warning("Cannot resolve a player name for buy " + buyId + " / user " + userId);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", buyId);
        payload.put("user", userId);

        final RPlayer finalOnline = online;
        final String finalName = targetName;

        api.send("redeempurchase", payload, result -> {
            if (result == null) {
                logger.warning("No response from server for buy " + buyId);
                // Usiamo scheduler astratto
                scheduler.runSync(() -> {
                    if (finalOnline != null)
                        finalOnline.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
                });
                return;
            }

            boolean success = Boolean.parseBoolean(Objects.toString(result.get("success"), "false"));
            String message = Objects.toString(result.get("message"), "Unknown response").toLowerCase().trim();

            scheduler.runSync(() -> {
                if (success) {
                    executeRewardCommands(buyId, finalName, finalOnline);
                    if (finalOnline != null) finalOnline.sendMessage(messager.get("rewardReceived"));
                } else {
                    logger.warning("redeempurchase failed for " + buyId + ": " + message);
                    if (finalOnline != null) finalOnline.sendMessage(messager.custom("&c" + message));
                }
            });
        });
    }

    public void confirm(String userId, String buyId) {
        confirm(userId, buyId, null);
    }

    private void executeRewardCommands(String rewardId, String playerName, RPlayer online) {
        if (config.getRewardsConfig() == null) return;
        List<String> commands = (List<String>) config.getRewardsConfig().get(rewardId + ".commands");

        if (commands != null && !commands.isEmpty()) {
            for (String cmd : commands) {
                // Il rimpiazzo basico di colori è ok nel core.
                cmd = cmd.replace("&", "§");

                // Deleghiamo l'esecuzione e il parsing dei placeholder all'adapter specifico
                commandExecutor.dispatchConsoleCommand(cmd, online, playerName);
            }
        } else {
            if (online != null) {
                online.sendMessage(messager.get("actionNotFound"));
            }
            logger.warning("Action for reward " + rewardId + " not found, please add it!");
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