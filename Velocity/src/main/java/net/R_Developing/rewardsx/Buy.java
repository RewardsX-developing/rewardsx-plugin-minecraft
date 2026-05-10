package net.R_Developing.rewardsx;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Buy {
    private final Fetcher fetcher;
    private final Api api;
    private final Platform platform;
    private final Config config;
    private final Messager messager;
    private final ProxySender proxySender;
    private final Bits bits;
    private final Logger logger;
    private final ProxyServer proxy;

    public Buy(ProxyServer proxy, Logger logger, Fetcher fetcher, Api api, Platform platform,
               Config config, Messager messager, ProxySender proxySender, Bits bits) {
        this.fetcher = fetcher;
        this.api = api;
        this.platform = platform;
        this.config = config;
        this.messager = messager;
        this.proxySender = proxySender;
        this.bits = bits;
        this.logger = logger;
        this.proxy = proxy;
    }

    public void send(Player player, String rewardName) {
        String quantity = "1";
        String platformId = platform.getId();
        String userId = config.getUserId(player.getUniqueId());

        if(userId == null) {
            player.sendMessage(messager.getMessage("notConnected"));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", rewardName);
        payload.put("quantity", quantity);
        payload.put("platform", platformId);
        payload.put("userid", userId);

        api.send("buy", payload, result -> {
            if(result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));
                String message = result.getOrDefault("message", "Unknown response").toString().toLowerCase().trim();

                String costStr = result.get("cost").toString();
                int cost = 0;
                if (costStr != null && !costStr.isEmpty()) {
                    try {
                        cost = Integer.parseInt(costStr);
                    } catch(NumberFormatException e) {
                        if(platform.isDebug()) {
                            logger.warn("Invalid cost format received: {}", costStr);
                        }
                    }
                }

                if(success) {
                    if (config.getUserId(player.getUniqueId()) != null) {
                        bits.remove(player, cost);
                        player.sendMessage(messager.getMessage("buySuccess"));
                        if(platform.isDebug()) {
                            logger.info("Buy successful for player: {} - Cost: {}", player.getUsername(), cost);
                        }
                    } else {
                        player.sendMessage(messager.getMessage("notConnected"));
                        if(platform.isDebug()) {
                            logger.warn("Player {} not logged in during buy confirmation", player.getUsername());
                        }
                    }
                } else {
                    if(message.contains("for your platform")) {
                        player.sendMessage(messager.getMessage("ownPlatform"));
                    } else if(message.contains("seconds between buy requests")) {
                        player.sendMessage(messager.getMessage("waitBuy"));
                    } else {
                        player.sendMessage(messager.getMessage("insufficientBalance"));
                    }

                    if(platform.isDebug()) {
                        logger.warn("Buy failed for player: {} - Message: {}", player.getUsername(), message);
                    }
                }
            } else {
                player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
                if(platform.isDebug()) {
                    logger.error("No response received from API for buy request - Player: {}", player.getUsername());
                }
            }
        });
    }

    public void confirm(String userId, String buyId) {
        Player player = config.getPlayerById(userId);
        if(player == null) {
            return;
        }

        if(platform.isDebug()) {
            logger.info("Confirming buy for userId: {} - Player found: {}", userId, player.getUsername());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", buyId);

        api.send("redeempurchase", payload, result -> {
            if(result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));
                String message = result.getOrDefault("message", "Unknown response").toString().toLowerCase().trim();

                if(success) {
                    executeCommand(buyId, player);
                    player.sendMessage(messager.getMessage("rewardReceived"));
                } else {
                    player.sendMessage(messager.custom("&c" + message));
                    if(platform.isDebug()) {
                        logger.warn("Buy confirmation failed - buyId: {} - Message: {}", buyId, message);
                    }
                }
            } else {
                player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
                if(platform.isDebug()) {
                    logger.error("No response received from API for buy confirmation - buyId: {}", buyId);
                }
            }
        });
    }

    public void confirm(String userId, String buyId, String username) {
        if (proxy.getPlayer(username).isEmpty()) {
            return;
        }

        Player player = proxy.getPlayer(username).get();
        if(platform.isDebug()) {
            logger.info("Confirming buy for userId: {} - Player found: {}", userId, player.getUsername());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", buyId);
        payload.put("user", userId);

        api.send("redeempurchase", payload, result -> {
            if(result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));
                String message = result.getOrDefault("message", "Unknown response").toString().toLowerCase().trim();

                if(success) {
                    executeCommand(buyId, player);
                    player.sendMessage(messager.getMessage("rewardReceived"));
                } else {
                    player.sendMessage(messager.custom("&c" + message));
                    if(platform.isDebug()) {
                        logger.warn("Buy confirmation failed - buyId: {} - Message: {}", buyId, message);
                    }
                }
            } else {
                player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
                if(platform.isDebug()) {
                    logger.error("No response received from API for buy confirmation - buyId: {}", buyId);
                }
            }
        });
    }

    private void executeCommand(String rewardId, Player player) {
        if(platform.isDebug()) {
            logger.info("Checking reward configuration for: {}", rewardId);
        }

        if(!config.hasReward(rewardId)) {
            player.sendMessage(messager.getMessage("actionNotFound"));
            if(platform.isDebug()) {
                logger.warn("Action not found for: {}", rewardId);
            }
            return;
        }

        List<String> commands = config.getRewardCommands(rewardId);
        String server = config.getRewardServer(rewardId);

        if(platform.isDebug()) {
            logger.info("Commands: {}", commands);
            logger.info("Server: {}", server);
        }

        if(commands != null && !commands.isEmpty() && server != null) {
            for(String cmd : commands) {
                String processedCmd = processPlaceholders(cmd, player);

                if(server.equalsIgnoreCase("proxy")) {
                    CompletableFuture<Boolean> result = proxy.getCommandManager()
                            .executeAsync(proxy.getConsoleCommandSource(), processedCmd);

                    result.thenAccept(success -> {
                        if(!success && platform.isDebug()) {
                            logger.warn("Failed to execute proxy command for player {}: {}", player.getUsername(), processedCmd);
                        }
                    }).exceptionally(throwable -> {
                        if(platform.isDebug()) {
                            logger.error("Error executing proxy command for player {}: {}", player.getUsername(), processedCmd, throwable);
                        }
                        return null;
                    });
                } else {
                    proxySender.sendCommand(player, server, "RUN", processedCmd);
                }
            }
        } else {
            player.sendMessage(messager.getMessage("actionNotFound"));
            if(platform.isDebug()) {
                if(commands == null || commands.isEmpty()) {
                    logger.warn("Commands for reward '{}' not found or empty!", rewardId);
                }
                if(server == null) {
                    logger.warn("Server for reward '{}' not found or is null!", rewardId);
                }
            }
        }
    }

    private String processPlaceholders(String cmd, Player player) {
        if(cmd == null || player == null) return cmd;

        String processed = cmd.replace("&", "§")
            .replace("%player%", player.getUsername())
            .replace("%uuid%", player.getUniqueId().toString())
            .replace("%player_name%", player.getUsername());

        if(player.getCurrentServer().isPresent()) {
            String currentServer = player.getCurrentServer().get().getServerInfo().getName();
            processed = processed.replace("%server%", currentServer);
        }

        return processed;
    }

    public int getBuys(Player player) {
        String userId = config.getUserId(player.getUniqueId());
        if(userId == null) return 0;
        return fetcher.buysList.getOrDefault(userId, 0);
    }
}