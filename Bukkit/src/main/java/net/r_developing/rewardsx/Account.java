package net.r_developing.rewardsx;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

    public void sendDetails(Player player) {
        if (quickConnect.isConnected(player)) {
            getDetails(player, (username, email) -> {
                player.sendMessage("§8--- §9RewardsX §8[§bMy Account§8] ---");
                player.sendMessage("");
                player.sendMessage("§8§l• §9Username: §7" + username);
                player.sendMessage("§8§l• §9Email: §7" + email);
                player.sendMessage("§8§l• §9Bits: §7" + bits.get(player));
                player.sendMessage("§8§l• §9Purchases: §7" + buy.getBuys(player));
            });
        } else {
            player.sendMessage(messager.get("notConnected"));
        }
    }

    private void getDetails(Player player, BiConsumer<String, String> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", config.getUserId(player.getUniqueId()));

        api.send("getuser", payload, result -> {
            if (result != null) {
                boolean success = Boolean.parseBoolean((String) result.get("success"));

                if (success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user = (Map<String, Object>) result.get("user");

                    String username = user.getOrDefault("name_user",  "Unknown").toString();
                    String email    = user.getOrDefault("email_user", "Unknown").toString();
                    callback.accept(username, email);
                } else {
                    player.sendMessage(messager.get("invalidCode"));
                }
            } else {
                player.sendMessage(ChatColor.RED + "INTERNAL ERROR: No response from server.");
            }
        });
    }
}
