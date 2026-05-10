package net.R_Developing.rewardsx;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;

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
                                player.sendMessage(messager.getMessage("connectionSuccessful"));
                            } else {
                                player.sendMessage(messager.getMessage("alreadyConnected"));
                            }
                        } else {
                            player.sendMessage(messager.getMessage("invalidCode"));
                        }
                    } else {
                        player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
                    }
                });
            } else {
                sendInsertCode(player);
            }
        } else {
            sender.sendMessage(messager.getMessage("onlyConsole"));
        }
    }

    public void disconnect(CommandSender sender) {
        if(sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;

            if(config.getUserId(player.getUniqueId()) != null) {
                config.removeUserId(player.getUniqueId());
                player.sendMessage(messager.getMessage("disconnectedSuccessful"));
            } else {
                player.sendMessage(messager.getMessage("alreadyDisconnected"));
            }
        } else {
            sender.sendMessage(messager.getMessage("onlyConsole"));
        }
    }

    public boolean isConnected(ProxiedPlayer player) {
        return config.getUserId(player.getUniqueId()) != null;
    }

    public void sendInsertCode(ProxiedPlayer player) {
        String url = "https://accounts.rewardsx.net/auth/quickconnect";
        String rawMessage = messager.get("insertCode");

        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', rawMessage));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        player.sendMessage(component);
    }
}