package net.r_developing.rewardsx.api.core.rewards;

import lombok.Setter;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.platform.PlatformCommandExecutor;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.platform.RServer;
import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RewardQueueManager {

    private final Map<String, List<PendingReward>> offlineQueue = new ConcurrentHashMap<>();
    private final RServer server;
    private final PlatformAdapter adapter;
    @Setter
    private PlatformCommandExecutor commandExecutor;
    private final PlatformScheduler scheduler;

    public RewardQueueManager(RServer server, PlatformAdapter adapter, PlatformScheduler scheduler) {
        this.server = server;
        this.adapter = adapter;
        this.scheduler = scheduler;
    }

    public void processReward(String userId, String transactionId, String username, List<RewardCommand> commands, Runnable onComplete) {
        boolean requiresOnline = commands.stream().anyMatch(RewardCommand::isRequireOnline);
        if (adapter.isPlayerOnline(username)) {
            executeCommands(username, commands);
            onComplete.run();
        } else if (!requiresOnline) {
            executeCommands(username, commands);
            onComplete.run();
        } else {
            offlineQueue.computeIfAbsent(username.toLowerCase(), k -> new ArrayList<>())
                    .add(new PendingReward(userId, transactionId, username, commands, onComplete));
        }
    }

    private void executeCommands(String username, List<RewardCommand> commands) {
        scheduler.runSync(() -> {
            RPlayer player = server.getPlayerExact(username);
            String targetName = (player != null) ? player.getPlayerName() : username;

            for (RewardCommand cmd : commands) {
                String finalCmd = cmd.command()
                        .replace("[player]", targetName)
                        .replace("%player%", targetName);

                commandExecutor.dispatchConsoleCommand(finalCmd, player, targetName);
            }
        });
    }

    /**
     * Called from specific platform (Spigot/Bungee)
     * when player Join.
     */
    public void onPlayerJoin(String username) {
        String lowerName = username.toLowerCase();
        List<PendingReward> pending = offlineQueue.remove(lowerName);

        if (pending != null && !pending.isEmpty()) {
            scheduler.runTaskLater(() -> {
                if (!adapter.isPlayerOnline(username)) return;

                for (PendingReward pr : pending) {
                    executeCommands(pr.username, pr.commands);
                    pr.onComplete.run();
                }
            }, 60L);
        }
    }
}