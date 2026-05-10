package net.r_developing.rewardsx;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class QuickConnect {
    private final Messager messager;
    private final Api api;
    private final Config config;

    public QuickConnect(Messager messager, Api api, Config config) {
        this.messager = messager;
        this.api = api;
        this.config = config;
    }

    public void connect(CommandSender sender, String code) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (code != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("code", code);

                api.send("quickconnect", payload, result -> {
                    if (result != null) {
                        boolean success = Boolean.parseBoolean((String) result.get("success"));

                        if (success) {
                            if (config.getUserId(player.getUniqueId()) == null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> user = (Map<String, Object>) result.get("user");
                                String id = user.getOrDefault("id_user", "Unknown ID").toString();

                                config.saveUserId(player.getUniqueId(), id);
                                player.sendMessage(messager.get("connectionSuccessful"));
                            } else {
                                player.sendMessage(messager.get("alreadyConnected"));
                            }
                        } else {
                            player.sendMessage(messager.get("invalidCode"));
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "INTERNAL ERROR: No response from server.");
                    }
                });
            } else {
                sendInsertCode(player);
            }
        } else {
            sender.sendMessage(messager.get("onlyConsole"));
        }
    }

    public void disconnect(CommandSender sender) {
        if(sender instanceof Player) {
            Player player = (Player) sender;

            if(config.getUserId(player.getUniqueId()) != null) {
                config.removeUserId(player.getUniqueId());
                player.sendMessage(messager.get("disconnectedSuccessful"));
            } else {
                player.sendMessage(messager.get("alreadyDisconnected"));
            }
        } else {
            sender.sendMessage(messager.get("onlyConsole"));
        }
    }

    public boolean isConnected(Player player) {
        return config.getUserId(player.getUniqueId()) != null;
    }

    public void sendInsertCode(Player player) {
        String url = "https://accounts.rewardsx.net/auth/quickconnect";
        String rawMessage = messager.get("insertCode");
        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', rawMessage));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        player.spigot().sendMessage(component);
    }
}