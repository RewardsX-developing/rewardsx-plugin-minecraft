package net.r_developing.rewardsx.api.core.gui;

import net.r_developing.rewardsx.api.core.player.RPlayer;

/**
 * Platform-agnostic contract for GUI rendering.
 *
 * <p>This interface defines the minimal API that all platform-specific GUI implementations
 * must provide. The actual rendering (Spigot inventory, Velocity web UI, etc.) is handled
 * by subclasses; this interface just says "open a GUI for a player" and "reload on config change".
 *
 * <p>Implementations are injected into CoreCommands and Buy so they can request a GUI
 * without knowing whether they are running on Spigot, Velocity, or another platform.
 */
public interface PlatformGUI {

    /**
     * Opens the rewards GUI for the specified player.
     *
     * <p>On Spigot/Bukkit, this opens a native inventory window.
     * On a proxy (Velocity), this either opens a web iframe or sends a plugin message
     * to the player's connected backend server to open it there.
     *
     * <p>This method is called:
     *   - When a player runs /rewardsx buy (without a reward name)
     *   - When a web dashboard action triggers a GUI open
     *   - When a player clicks "browse rewards" in a menu
     *
     * @param player the player to show the GUI to
     */
    void open(RPlayer player);

    /**
     * Reloads the GUI after a config reload.
     *
     * <p>Called when a server admin runs /rewardsx reload. Subclasses can use this
     * to refresh any cached configuration, message strings, or other data that may
     * have changed on disk.
     *
     * <p>Does not require a player context - it is a global refresh operation.
     */
    void reload();
}