package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.util.List;

/**
 * Abstraction for command execution and tab completion.
 *
 * <p>Provides platform-independent methods to:
 *   - Execute commands as the console with player name substitution
 *   - Execute player commands
 *   - Execute console commands
 *   - Provide tab completion suggestions
 *
 * <p>Implementations handle platform-specific differences in command execution
 * (Bukkit's Bukkit.dispatchCommand(), Velocity's CommandManager, etc.).
 *
 * <p>This interface is used by Buy to execute reward grant commands, and by
 * CoreCommands to handle /rewardsx subcommands.
 */
public interface PlatformCommandExecutor {

    /**
     * Executes a command as the console with player name substitution.
     *
     * <p>Replaces placeholders in the command string with actual values before
     * executing. Common placeholders:
     *   - %player% or [player] - replaced with the player's name
     *   - %uuid% or [uuid] - replaced with the player's UUID
     *
     * <p>The command is executed with full console privileges, regardless of
     * the target player's permissions. Used for reward grant execution.
     *
     * <p>If targetPlayer is provided and online, it takes precedence over targetName
     * for placeholder substitution. targetName is used as a fallback if the player
     * is offline.
     *
     * @param command       the command to execute (without the leading slash)
     *                      e.g. "give [player] diamond"
     * @param targetPlayer  the online player to use for placeholders, or null if offline
     * @param targetName    the player's name (used if targetPlayer is null or offline)
     */
    void dispatchConsoleCommand(String command, RPlayer targetPlayer, String targetName);

    /**
     * Executes a command sent by a player.
     *
     * <p>Called when a player runs a command. The platform implementation checks
     * the player's permissions and executes accordingly. Unlike dispatchConsoleCommand(),
     * this respects the player's permission level.
     *
     * @param sender the player executing the command (must be online)
     * @param args   the command arguments (e.g. ["buy", "rank-vip"])
     * @return true if the command was handled, false if not recognized
     */
    boolean execute(RPlayer sender, String[] args);

    /**
     * Executes a command sent from the console.
     *
     * <p>Called when a console command is run. Similar to execute() but for
     * console input instead of a player. No permission checks are performed.
     *
     * @param senderName the console name (typically "Console" or "Server")
     * @param args       the command arguments
     * @return true if the command was handled, false if not recognized
     */
    boolean executeConsole(String senderName, String[] args);

    /**
     * Provides tab completion suggestions for a partially typed command.
     *
     * <p>Called by the platform when a player presses TAB while typing a command.
     * Returns a filtered list of suggestions based on what the player has typed so far.
     *
     * <p>Example:
     *   Player types: /rewardsx bu
     *   Returns: ["buy", "bugs"]
     *
     * <p>The filtering (case-insensitive prefix matching) is typically done by
     * the platform's command system after this method returns.
     *
     * @param args the partially typed command arguments
     * @return a list of tab completion suggestions, or an empty list if none
     */
    List<String> getTabCompletions(String[] args);
}