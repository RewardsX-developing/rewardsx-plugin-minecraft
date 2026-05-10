package net.R_Developing.rewardsx;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Account {
    private final QuickConnect quickConnect;
    private final Messager messager;
    private final Bits bits;
    private final Buy buy;
    private final Config config;
    private final Api api;

    public Account(QuickConnect quickConnect, Messager messager, Bits bits, Buy buy, Config config, Api api) {
        this.quickConnect = quickConnect;
        this.messager = messager;
        this.bits = bits;
        this.buy = buy;
        this.config = config;
        this.api = api;
    }

    public void sendDetails(ProxiedPlayer player) {
        if (quickConnect.isConnected(player)) {
            getDetails(player, (username, email) -> {
                // Header
                player.sendMessage(net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(
                        Component.text("--- ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("RewardsX ", NamedTextColor.BLUE))
                                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                                .append(Component.text("My Account", NamedTextColor.AQUA))
                                .append(Component.text("] ---", NamedTextColor.DARK_GRAY))
                ));

                player.sendMessage(net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(Component.empty()));

                // Username
                player.sendMessage(net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(
                        Component.text("• ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("Username: ", NamedTextColor.BLUE))
                                .append(Component.text(username, NamedTextColor.GRAY))
                ));

                // Email
                player.sendMessage(net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(
                        Component.text("• ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("Email: ", NamedTextColor.BLUE))
                                .append(Component.text(email, NamedTextColor.GRAY))
                ));

                // AdBits
                player.sendMessage(net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(
                        Component.text("• ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("AdBits: ", NamedTextColor.BLUE))
                                .append(Component.text(String.valueOf(bits.get(player)), NamedTextColor.GRAY))
                ));

                // Purchases
                player.sendMessage(net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(
                        Component.text("• ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("Purchases: ", NamedTextColor.BLUE))
                                .append(Component.text(String.valueOf(buy.getBuys(player)), NamedTextColor.GRAY))
                ));
            });
        } else {
            player.sendMessage(messager.custom(messager.get("notConnected")));
        }
    }

    private void getDetails(ProxiedPlayer player, BiConsumer<String, String> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", config.getUserId(player.getUniqueId()));

        api.send("getuser", payload, result -> {
            if (result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));

                if (success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user = (Map<String, Object>) result.get("user");

                    String username = user.getOrDefault("name_user", "Unknown").toString();
                    String email = user.getOrDefault("email_user", "Unknown").toString();
                    callback.accept(username, email);
                } else {
                    player.sendMessage(messager.custom(messager.get("invalidCode")));
                }
            } else {
                player.sendMessage(messager.custom("&cINTERNAL ERROR: No response from server."));
            }
        });
    }
}