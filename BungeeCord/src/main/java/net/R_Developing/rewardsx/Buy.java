package net.R_Developing.rewardsx;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Buy {
    private final Fetcher fetcher;
    private final Api api;
    private final Platform platform;
    private final Config config;
    private final Messager messager;
    private final Plugin plugin;
    private final ProxySender proxySender;
    private final Bits bits;

    public Buy(Plugin plugin, Fetcher fetcher, Api api, Platform platform, Config config, Messager messager, ProxySender proxySender, Bits bits) {
        this.fetcher = fetcher;
        this.api = api;
        this.platform = platform;
        this.config = config;
        this.messager = messager;
        this.plugin = plugin;
        this.proxySender = proxySender;
        this.bits = bits;
    }

    public void send(ProxiedPlayer player, String rewardName) {
        String quantity = "1";
        String platformId = platform.getId();
        String userId = config.getUserId(player.getUniqueId());

        if(config.getUserId(player.getUniqueId()) == null)
            player.sendMessage(messager.getMessage("notConnected"));

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
                    } catch(NumberFormatException ignored) {}
                }

                if(success) {
                    if(config.getUserId(player.getUniqueId()) != null) {
                        bits.remove(player, cost);
                        player.sendMessage(messager.getMessage("buySuccess"));
                    } else {
                        player.sendMessage(messager.getMessage("notConnected"));
                    }
                } else {
                    if(message.contains("for your platform")) {
                        player.sendMessage(messager.getMessage("ownPlatform"));
                    } else if(message.contains("seconds between buy requests")) {
                        player.sendMessage(messager.getMessage("waitBuy"));
                    } else {
                        player.sendMessage(messager.getMessage("insufficientBalance"));
                    }
                }
            } else {
                player.sendMessage(messager.custom("INTERNAL ERROR: No response from server."));
            }
        });
    }

    public void confirm(String userId, String buyId) {
        ProxiedPlayer player = config.getPlayerById(userId);
        if(player == null) {
            return;
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
                }
            } else {
                player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
            }
        });
    }

    public void confirm(String userId, String buyId, String username) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(username);
        if(player == null) {
            return;
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
                }
            } else {
                player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
            }
        });
    }

    private void executeCommand(String rewardId, ProxiedPlayer player) {
        if(config.getRewardsConfig() == null) return;

        List<String> commands = config.getRewardsConfig().getStringList(rewardId + ".commands");
        String server = config.getRewardsConfig().getString(rewardId + ".server");

        if(platform.isDebug()) {
            plugin.getLogger().info("Commands: " + commands);
            plugin.getLogger().info("Server: " + server);
        }

        if(!commands.isEmpty() && server != null) {
            for(String cmd : commands) {
                String processedCmd = cmd.replace("%player%", player.getName());

                if(server.equalsIgnoreCase("proxy")) {
                    ProxyServer.getInstance().getPluginManager().dispatchCommand(
                            ProxyServer.getInstance().getConsole(), processedCmd);
                } else {
                    proxySender.sendCommand(player, server, "RUN", processedCmd);
                }
            }
        } else {
            player.sendMessage(messager.getMessage("actionNotFound"));
            plugin.getLogger().warning("Action for reward " + rewardId + " not found, please add it!");
        }
    }

    public int getBuys(ProxiedPlayer player) {
        String userId = config.getUserId(player.getUniqueId());
        if(userId == null) return 0;
        return fetcher.buysList.getOrDefault(userId, 0);
    }
}