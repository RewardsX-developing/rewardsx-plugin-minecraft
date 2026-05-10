package net.r_developing.rewardsx;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public void send(Player player, String rewardName) {
        if (!platform.isProxy()) {
            String quantity = "1";
            String platformId = platform.getId();
            String userId = config.getUserId(player.getUniqueId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", rewardName);
            payload.put("quantity", quantity);
            payload.put("platform", platformId);
            payload.put("userid", userId);

            api.send("buy", payload, result -> {
                if (result != null) {
                    Object successObj = result.get("success");
                    boolean success = successObj != null && Boolean.parseBoolean(Objects.toString(successObj, "false"));

                    String message = Objects.toString(result.get("message"), "Unknown response").toLowerCase().trim();

                    if (success) {
                        String costStr = Objects.toString(result.get("cost"), "0");
                        int cost;
                        try {
                            cost = Integer.parseInt(costStr.trim());
                        } catch (NumberFormatException ignored) {
                            cost = 0;
                        }

                        final int finalCost = cost;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (config.getUserId(player.getUniqueId()) != null) {
                                bits.remove(player, finalCost);
                                player.sendMessage(messager.get("buySuccess"));
                            } else {
                                player.sendMessage(messager.get("notConnected"));
                            }
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (message.contains("for your platform")) {
                                player.sendMessage(messager.get("ownPlatform"));
                            } else if (message.contains("seconds between buy requests")) {
                                player.sendMessage(messager.get("waitBuy"));
                            } else {
                                player.sendMessage(messager.get("insufficientBalance"));
                            }
                        });
                    }
                } else {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(ChatColor.RED + "INTERNAL ERROR: No response from server.")
                    );
                }
            });
        } else {
            proxySender.sendCommand(player, "BUY", rewardName);
        }
    }

    public void confirm(String userId, String buyId) {
        Player player = config.getPlayerById(userId);
        if (player == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", buyId);
        payload.put("user", userId);

        api.send("redeempurchase", payload, result -> {
            if (result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));
                String message = result.getOrDefault("message", "Unknown response").toString().toLowerCase();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        executeCommand(buyId, player);
                        player.sendMessage(messager.get("rewardReceived"));
                    } else {
                        player.sendMessage(ChatColor.RED + message);
                    }
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "INTERNAL ERROR: No response from server.")
                );
            }
        });
    }

    public void confirm(String userId, String buyId, String username) {
        Player player = Bukkit.getPlayerExact(username);
        if (player == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", buyId);
        payload.put("user", userId);

        api.send("redeempurchase", payload, result -> {
            if (result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));
                String message = result.getOrDefault("message", "Unknown response").toString().toLowerCase().trim();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        executeCommand(buyId, player);
                        player.sendMessage(messager.get("rewardReceived"));
                    } else {
                        player.sendMessage(messager.custom("&c" + message));
                    }
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."))
                );
            }
        });
    }

    private void executeCommand(String rewardId, Player player) {
        if (config.getRewardsConfig() == null) return;
        List<String> commands = config.getRewardsConfig().getStringList(rewardId + ".commands");

        if (commands != null && !commands.isEmpty() && player != null) {
            for (String cmd : commands) {
                cmd = cmd.replace("&", "§").replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        } else {
            assert player != null;
            player.sendMessage(messager.get("actionNotFound"));
            plugin.getLogger().warning("Action for reward " + rewardId + " not found, please add it!");
        }
    }

    public void executeCommand(Player player, String cmd) {
        if (config.getRewardsConfig() == null) return;

        if (cmd != null && player != null) {
            final String resolvedCmd = buildCommand(player, cmd);
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCmd);
                if (!success) plugin.getLogger().warning("Failed to execute command: " + resolvedCmd);
            });
        } else {
            if (player != null)
                player.sendMessage(messager.get("actionNotFound"));
            plugin.getLogger().warning("Action not found, or player is invalid!");
        }
    }

    private String buildCommand(Player player, String cmd) {
        cmd = cmd.replace("&", "§").replace("%player%", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            cmd = PlaceholderAPI.setPlaceholders(player, cmd);
        return cmd;
    }

    public int getBuys(Player player) {
        String userId = config.getUserId(player.getUniqueId());
        if (userId == null) return 0;
        return fetcher.buysList.getOrDefault(userId, 0);
    }
}