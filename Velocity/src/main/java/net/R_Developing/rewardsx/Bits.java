package net.R_Developing.rewardsx;

import com.velocitypowered.api.proxy.Player;

public class Bits {
    private final Fetcher fetcher;
    private final Config config;

    public Bits(Fetcher fetcher, Config config) {
        this.fetcher = fetcher;
        this.config = config;
    }

    public int get(Player player) {
        String userId = config.getUserId(player.getUniqueId());
        if (userId == null) return 0;
        return fetcher.bitsList.getOrDefault(userId, 0);
    }

    public void remove(Player player, int amount) {
        String userId = config.getUserId(player.getUniqueId());
        if(userId == null || amount <= 0) return;
        int currentBalance = fetcher.bitsList.getOrDefault(userId, 0);
        int newBalance = currentBalance - amount;
        fetcher.bitsList.put(userId, newBalance);
    }
}