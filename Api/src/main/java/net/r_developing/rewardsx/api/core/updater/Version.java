package net.r_developing.rewardsx.api.core.updater;

import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;

/**
 * Checks for plugin updates and notifies players when a newer version is available.
 *
 * <p>Fetcher periodically fetches the latest version from Spiget and caches it
 * in fetcher.latestVersion. This class compares the cached latest version against
 * the current plugin version and displays an update notification if needed.
 *
 * <p>Called when:
 *   - A player runs /rewardsx version
 *   - Manually via checkVersion() from core command handlers
 *
 * <p>Notifications are only shown to players if a newer version is available.
 */
public class Version {
    /** Fetcher that caches the latest version from Spiget. */
    private final Fetcher fetcher;
    /** Messager for localized update notification text. */
    private final Messager messager;
    /** Platform adapter for reading the current plugin version. */
    private final PlatformAdapter plugin;

    /**
     * Constructor injection.
     *
     * @param fetcher  the fetcher (provides latestVersion cache)
     * @param messager localized strings
     * @param plugin   platform adapter (provides current version)
     */
    public Version(Fetcher fetcher, Messager messager, PlatformAdapter plugin) {
        this.fetcher = fetcher;
        this.messager = messager;
        this.plugin = plugin;
    }

    /**
     * Checks if a newer version is available and notifies the player.
     *
     * <p>Compares the cached latest version (fetched by Fetcher) against the
     * current running version. If they differ, an update notification is sent
     * to the player (if provided) with download links.
     *
     * <p>The comparison is simple string contains check - if latestVersion does not
     * contain currentVersion as a substring, a newer version is assumed to exist.
     *
     * <p>Note: This is a basic comparison and may have edge cases:
     *   - currentVersion "1.0" vs latestVersion "1.0.1" - detected as out of date (correct)
     *   - currentVersion "1.1.0" vs latestVersion "1.0.5" - NOT detected (downgrade not detected)
     *   - Empty versions - may not trigger notification
     *
     * @param sender the player to notify (if null, no message is sent)
     */
    public void checkVersion(RPlayer sender) {
        // Compare versions - if current is not in latest, assume newer version exists.
        if (!fetcher.latestVersion.contains(currentVersion())) {
            // Send notification to the player.
            if (sender != null) {
                sender.sendMessage(outOfDate());
            }
        }
    }

    /**
     * Returns the currently running plugin version.
     *
     * <p>Reads from the plugin's metadata (plugin.yml on Spigot, velocity-plugin.json
     * on Velocity, etc.).
     *
     * @return the current version string (e.g. "1.0.0", "2.1.3-SNAPSHOT")
     */
    public String currentVersion() {
        return plugin.getPluginVersion();
    }

    /**
     * Builds the update notification message with download links.
     *
     * <p>Uses the messager to get a localized template (outOfDate), then formats it
     * with the latest version, current version, and download URLs (Spigot and Modrinth).
     *
     * <p>The "outOfDate" message template should be something like:
     *   "A new version of RewardsX is available! Latest: {0} | Current: {1} | Download: {2} or {3}"
     *
     * <p>URLs are hardcoded for Spigot and Modrinth resource pages.
     *
     * @return the formatted update notification message
     */
    private String outOfDate() {
        return String.format(
                messager.get("outOfDate"),
                fetcher.latestVersion,
                currentVersion(),
                "https://www.spigotmc.org/resources/rewardsx-%E2%AD%90-%E2%80%A2-best-rewards-system-spigot-bungeecord-and-velocity-support.121867/",
                "https://modrinth.com/plugin/rewardsx/version/4dMw3uIl"
        );
    }
}