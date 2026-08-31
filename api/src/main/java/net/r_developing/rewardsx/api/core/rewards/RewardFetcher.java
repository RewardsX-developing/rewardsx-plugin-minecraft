package net.r_developing.rewardsx.api.core.rewards;

import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;

import java.util.*;

/**
 * Fetches pending reward grants from the backend and processes them.
 *
 * <p>Called periodically (by RewardPollingTask) to check if any players have
 * completed purchases and are waiting for their rewards. For each pending grant,
 * filters the commands based on server token and server scope, then invokes a
 * callback to execute the grant.
 *
 * <p>Command filtering:
 *   - Server scope: "GLOBAL", "ALL", or matching the current server's token
 *   - Only commands that match the filter are included in the grant
 *   - This allows the same reward to have server-specific or global commands
 *
 * <p>Example backend response:
 *   {
 *     "buys": [
 *       {
 *         "id": "trans-123",
 *         "userId": "user-456",
 *         "username": "PlayerName",
 *         "commands": [
 *           {
 *             "command": "give [player] diamond",
 *             "requireOnline": false,
 *             "server": "GLOBAL"
 *           },
 *           {
 *             "command": "rank-set [player] vip",
 *             "requireOnline": true,
 *             "token": "server-token-789",
 *             "server": "SPECIFIC"
 *           }
 *         ]
 *       }
 *     ]
 *   }
 */
public class RewardFetcher {

    /** HTTP client for backend API calls. */
    private final Api api;
    /** Console logger. */
    private final PlatformLogger logger;
    /** Handler for reward grant execution. */
    private final Buy buy;

    /**
     * Constructor injection.
     *
     * @param api    HTTP client for backend
     * @param logger console logger
     * @param buy    reward grant handler
     */
    public RewardFetcher(Api api, PlatformLogger logger, Buy buy) {
        this.api = api;
        this.logger = logger;
        this.buy = buy;
    }

    /**
     * Fetches pending rewards from the backend and processes each one.
     *
     * <p>Makes an async HTTP call to the "success-buys" endpoint with the platform ID.
     * For each pending grant in the response:
     *   1. Extracts transaction ID, user ID, and username
     *   2. Filters commands based on server scope and token
     *   3. Adds the grant to inFlight to prevent duplicate processing
     *   4. Invokes the callback to execute the grant (if commands exist)
     *
     * <p>Filtering logic for commands:
     *   - Includes a command if:
     *     - Its server scope is "GLOBAL" or "ALL" (applies everywhere), OR
     *     - Its token matches the current server's token (server-specific)
     *   - Commands with other tokens are excluded
     *   - This prevents a "rank-vip" command for server-A from running on server-B
     *
     * @param platformId  the server's platform ID (used to filter buys on the backend)
     * @param serverToken the server's secret token (used to filter commands for this server)
     * @param inFlight    a set to track grants being processed (format: "userId:transactionId")
     *                    The method adds each grant to this set and the callback removes it when done
     * @param callback    invoked for each pending grant with filtered commands
     */
    public void fetchPendingRewards(String platformId, String serverToken, Set<String> inFlight, RewardConfirmCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("platform", platformId);
        payload.put("token", serverToken);

        // Make an async call to the backend.
        api.send("GET", "success-buys", payload, result -> {
            if (result == null) return;

            // Extract the "buys" list from the response.
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("buys");

            if (list != null) {
                // Process each pending grant.
                for (Map<String, Object> o : list) {
                    // Extract grant metadata.
                    String transactionId = Objects.toString(o.get("id"), null);
                    String userId = Objects.toString(o.get("userId"), null);
                    String username = Objects.toString(o.get("username"), null);

                    // Extract and filter the commands for this grant.
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rawCommands = (List<Map<String, Object>>) o.get("commands");
                    List<RewardCommand> commands = new ArrayList<>();

                    if (rawCommands != null) {
                        for (Map<String, Object> cmdMap : rawCommands) {
                            // Extract command metadata.
                            String cmdServer = Objects.toString(cmdMap.get("server"), "GLOBAL");
                            String cmdToken = Objects.toString(cmdMap.get("token"), "");

                            // Debug logging for token matching (helpful for troubleshooting).
                            logger.debug(">> CHECK TOKEN: Token from JSON = '" + cmdToken + "' | Token from Plugin = '" + serverToken + "'");

                            // Filter: include command if it's global or token matches.
                            if (cmdServer.equalsIgnoreCase("GLOBAL") || cmdServer.equalsIgnoreCase("ALL") || cmdToken.equals(serverToken)) {
                                String cmdText = Objects.toString(cmdMap.get("command"), "");
                                // Parse the requireOnline flag (defaults to true for safety).
                                boolean reqOnline = true;
                                if (cmdMap.containsKey("requireOnline")) {
                                    reqOnline = Boolean.parseBoolean(Objects.toString(cmdMap.get("requireOnline"), "true"));
                                }
                                commands.add(new RewardCommand(cmdText, reqOnline));
                            }
                        }
                    }

                    // Create a unique key for this grant and try to add it to inFlight.
                    String key = userId + ":" + transactionId;
                    if (!inFlight.add(key)) {
                        // Grant is already being processed - skip it.
                        continue;
                    }

                    // Invoke the callback to execute the grant (if there are commands and buy is not null).
                    if (buy != null && !commands.isEmpty()) {
                        callback.confirm(userId, transactionId, username, commands);
                    } else {
                        // No commands or buy is null - remove from inFlight immediately.
                        inFlight.remove(key);
                    }
                }
            } else {
                logger.warning("Success-buys list is null");
            }
        });
    }

    /**
     * Functional interface for processing a pending reward grant.
     *
     * <p>Called once for each pending grant fetched from the backend.
     * The implementation (typically Buy.confirm()) receives the filtered commands
     * and executes them.
     */
    public interface RewardConfirmCallback {
        /**
         * Process a pending reward grant.
         *
         * @param userId        the RewardsX user ID
         * @param transactionId  unique ID for this purchase
         * @param username      the player's username
         * @param commands      list of filtered commands to execute (server-scope filtered)
         */
        void confirm(String userId, String transactionId, String username, List<RewardCommand> commands);
    }
}