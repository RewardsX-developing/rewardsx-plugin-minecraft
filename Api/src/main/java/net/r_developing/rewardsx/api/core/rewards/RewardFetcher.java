package net.r_developing.rewardsx.api.core.rewards;

import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;

import java.util.*;

/**
 * Fetches pending reward grants from the backend and processes them.
 */
public class RewardFetcher {

    private final Api api;
    private final PlatformLogger logger;
    private final Buy buy;

    public RewardFetcher(Api api, PlatformLogger logger, Buy buy) {
        this.api = api;
        this.logger = logger;
        this.buy = buy;
    }

    public void fetchPendingRewards(String platformId, String serverToken, Set<String> inFlight, RewardConfirmCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("platform", platformId);
        payload.put("token", serverToken);

        // Make an async call to the backend.
        api.send("GET", "success-buys", payload, result -> {
            if (result == null) return;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("buys");

            if (list != null) {
                if (!list.isEmpty()) {
                    String rewardWord = list.size() == 1 ? "reward" : "rewards";
                    logger.info(String.format("Fetched pending %s (%d found).", rewardWord, list.size()));
                }

                int playersToProcess = 0;

                // Process each pending grant.
                for (Map<String, Object> o : list) {
                    String transactionId = Objects.toString(o.get("id"), null);
                    String userId = Objects.toString(o.get("userId"), null);
                    String username = Objects.toString(o.get("username"), null);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rawCommands = (List<Map<String, Object>>) o.get("commands");
                    List<RewardCommand> commands = new ArrayList<>();

                    if (rawCommands != null) {
                        for (Map<String, Object> cmdMap : rawCommands) {
                            String cmdServer = Objects.toString(cmdMap.get("server"), "GLOBAL");
                            String cmdToken = Objects.toString(cmdMap.get("token"), "");

                            logger.debug(">> CHECK TOKEN: Token from JSON = '" + cmdToken + "' | Token from Plugin = '" + serverToken + "'");

                            if (cmdServer.equalsIgnoreCase("GLOBAL") || cmdServer.equalsIgnoreCase("ALL") || cmdToken.equals(serverToken)) {
                                String cmdText = Objects.toString(cmdMap.get("command"), "");
                                boolean reqOnline = true;
                                if (cmdMap.containsKey("requireOnline")) {
                                    reqOnline = Boolean.parseBoolean(Objects.toString(cmdMap.get("requireOnline"), "true"));
                                }
                                commands.add(new RewardCommand(cmdText, reqOnline));
                            }
                        }
                    }

                    String key = userId + ":" + transactionId;
                    if (!inFlight.add(key)) {
                        continue;
                    }

                    if (buy != null && !commands.isEmpty()) {
                        playersToProcess++;
                        callback.confirm(userId, transactionId, username, commands);
                    } else {
                        inFlight.remove(key);
                    }
                }

                if (playersToProcess > 0) {
                    String playerWord = playersToProcess == 1 ? "player" : "players";
                    logger.info(String.format("Executing commands for %d %s...", playersToProcess, playerWord));
                }

            } else {
                logger.warning("Success-buys list is null");
            }
        });
    }

    public interface RewardConfirmCallback {
        void confirm(String userId, String transactionId, String username, List<RewardCommand> commands);
    }
}