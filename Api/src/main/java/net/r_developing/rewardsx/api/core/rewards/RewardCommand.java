package net.r_developing.rewardsx.api.core.rewards;

/**
 * Represents a single command to execute as part of a reward grant.
 *
 * <p>A reward may consist of multiple commands - for example, a rank upgrade might
 * give a title, coins, and broadcast a message. Each command is wrapped in this record
 * along with metadata about whether the player must be online to receive it.
 *
 * <p>Immutable by design - records are final and all fields are immutable.
 * Automatically generates constructor, getters, equals, hashCode, and toString.
 *
 * <p>Usage:
 *   List&lt;RewardCommand&gt; commands = List.of(
 *     new RewardCommand("give [player] diamond", false),
 *     new RewardCommand("say [player] got a diamond!", false),
 *     new RewardCommand("some-rank-command [player]", true)
 *   );
 *   buy.confirm(userId, transactionId, username, commands, () -> {...});
 */
public record RewardCommand(
        /*
         * The command string to execute.
         *
         * <p>May contain placeholders like [player] or %player% which get substituted
         * at execution time. Example: "give [player] diamond 64"
         */
        String command,

        /*
         * Whether this command requires the player to be online.
         *
         * <p>If true, the entire grant is blocked if the player is not online.
         * If false, the command executes immediately even if the player is offline
         * (assuming the command can work offline).
         *
         * <p>Typical use cases:
         *   - false: "give [player] diamond" - gives item to offline inventory
         *   - false: "say Congratulations [player]!" - broadcast message
         *   - true: "playsound ...entity.player.levelup" - needs player online to hear sound
         *   - true: "execute-rank-command [player]" - rank plugin may need online player
         *
         * <p>Buy.confirm() checks this flag before granting. If any command in the list
         * has requireOnline=true and the player is offline, the entire grant is rejected.
         */
        boolean requireOnline
) {
    // Record automatically provides:
    //   - Constructor: RewardCommand(String command, boolean requireOnline)
    //   - Getters: command(), requireOnline()
    //   - equals, hashCode, toString
    // All fields are implicitly final and immutable.
}