package net.r_developing.rewardsx.api.core.rewards;

import java.util.List;

public class PendingReward {
    public final String userId;
    public final String transactionId;
    public final String username;
    public final List<RewardCommand> commands;
    public final Runnable onComplete;

    public PendingReward(String userId, String transactionId, String username, List<RewardCommand> commands, Runnable onComplete) {
        this.userId = userId;
        this.transactionId = transactionId;
        this.username = username;
        this.commands = commands;
        this.onComplete = onComplete;
    }
}