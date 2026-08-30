package net.r_developing.rewardsx.api.core.rewards;

public class RewardCommand {
    private final String command;
    private final boolean requireOnline;

    public RewardCommand(String command, boolean requireOnline) {
        this.command = command;
        this.requireOnline = requireOnline;
    }

    public String getCommand() { return command; }
    public boolean isRequireOnline() { return requireOnline; }
}