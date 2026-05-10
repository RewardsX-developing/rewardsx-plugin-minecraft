package net.r_developing.rewardsx;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PApi extends PlaceholderExpansion {
    private final Version version;
    private final Fetcher fetcher;
    private final Plugin plugin;
    private final Messager messager;
    private final Config config;

    private Map<String, Integer> cachedBitsList;
    private Map<String, Integer> cachedBuysList;
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 30000;

    public PApi(Version version, Fetcher fetcher, Plugin plugin, Messager messager, Config config) {
        this.version = version;
        this.fetcher = fetcher;
        this.plugin = plugin;
        this.messager = messager;
        this.config = config;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rewardsx";
    }

    @Override
    public @NotNull String getAuthor() {
        return "R_Developing";
    }

    @Override
    public @NotNull String getVersion() {
        return version.currentVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if(player == null) return "No player";

        try {
            String playerId = config.getUserId(player.getUniqueId());

            switch (identifier) {
                case "player_bits":
                    return String.valueOf(fetcher.bitsList.getOrDefault(playerId, 0));
                case "player_buys":
                    return String.valueOf(fetcher.buysList.getOrDefault(playerId, 0));
                case "player_rank_bits":
                    return String.valueOf(getPlayerRank(playerId, fetcher.bitsList));
                case "player_rank_buys":
                    return String.valueOf(getPlayerRank(playerId, fetcher.buysList));
            }

            if(identifier.startsWith("topbits_")) {
                return handleTopPlaceholder(identifier, "topBitsValue", "topBitsNoData", true);
            } else if (identifier.startsWith("topbuys_")) {
                return handleTopPlaceholder(identifier, "topBuysValue", "topBuysNoData", false);
            }

            return "Invalid usage";
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing placeholder: " + identifier + " - " + e.getMessage());
            return "Error";
        }
    }

    private String handleTopPlaceholder(String identifier, String valueKey, String noDataKey, boolean isBits) {
        String[] parts = identifier.split("_");
        if (parts.length < 2) return "Invalid usage";

        try {
            int index = Integer.parseInt(parts[1]) - 1;
            Map<String, Integer> sortedList = getCachedSortedList(isBits);
            List<Map.Entry<String, Integer>> entryList = new ArrayList<>(sortedList.entrySet());

            if (index >= 0 && index < entryList.size()) {
                Map.Entry<String, Integer> entry = entryList.get(index);
                String userId = entry.getKey();
                Integer value = entry.getValue();

                String playerName = config.getOfflinePlayer(userId).getName();
                if (playerName == null) playerName = "Unknown";

                String formatString = messager.getNoPrefix(valueKey);
                return formatString
                    .replace("%index%", String.valueOf(index + 1))
                    .replace("%value%", String.valueOf(value))
                    .replace("%player%", playerName);
            } else {
                String formatString = messager.getNoPrefix(noDataKey);
                return formatString.replace("%index%", String.valueOf(index + 1));
            }
        } catch(NumberFormatException e) {
            return "Invalid number";
        }
    }

    private Map<String, Integer> getCachedSortedList(boolean isBits) {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_DURATION) {
            cachedBitsList = sortFromHigher(fetcher.bitsList);
            cachedBuysList = sortFromHigher(fetcher.buysList);
            lastCacheUpdate = now;
        }

        return isBits ? cachedBitsList : cachedBuysList;
    }

    private int getPlayerRank(String playerId, Map<String, Integer> dataMap) {
        Map<String, Integer> sorted = sortFromHigher(dataMap);
        List<String> playerIds = new ArrayList<>(sorted.keySet());
        int rank = playerIds.indexOf(playerId) + 1;
        return Math.max(rank, 0);
    }

    private static LinkedHashMap<String, Integer> sortFromHigher(Map<String, Integer> map) {
        return map.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
}
