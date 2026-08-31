package net.r_developing.rewardsx.api.core;

import net.r_developing.rewardsx.api.Configs.MainConfig;
import net.r_developing.rewardsx.api.Configs.MessagesConfig;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.platform.*;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.api.core.proxy.ProxySender;
import net.r_developing.rewardsx.api.core.rewards.RewardFetcher;
import net.r_developing.rewardsx.api.core.updater.Version;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Translator;

/**
 * Abstract base class for platform-specific RewardsX implementations.
 *
 * <p>Defines the initialization sequence and dependency graph for the entire plugin.
 * Each platform (Spigot, Velocity, BungeeCord) extends this and provides concrete
 * implementations of the abstract dependency methods.
 *
 * <p>Dependency injection flow (onEnable):
 *   1. Config (reads from disk)
 *   2. Platform (backend validation, language/proxy detection)
 *   3. API (sets platform reference)
 *   4. Messager (message translation with platform + translator)
 *   5. Fetcher (backend polling, rewards list caching)
 *   6. Buy (purchase/grant execution)
 *   7. RewardFetcher (pending grants from backend)
 *   8. GUI (platform-specific rendering)
 *   9. Version (update checking)
 *   10. CoreCommands (/rewardsx command handler)
 *   11. Adapter.setupDependencies (wire up GUI, commands, proxy listener)
 *   12. RewardPollingTask (periodic polling)
 *   13. Platform.checkAndStart (validate registration, start services)
 *
 * <p>Template method pattern: onEnable and onDisable are called by the platform
 * (Bukkit, Velocity, etc.); subclasses provide concrete implementations via
 * abstract methods.
 */
public abstract class RewardsXCore {

    // ===== Core Instances (Shared across all components) =====

    /** Platform detection, validation, and configuration. */
    protected Platform platform;

    /** GUI implementation (platform-specific). */
    protected PlatformGUI rewardsGUI;

    /** Config file management and player data storage. */
    protected Config config;

    /** /rewardsx command handler. */
    protected CoreCommands coreCommands;

    /** Localized message strings and translation. */
    protected Messager messager;

    /** Backend polling for rewards list and pending grants. */
    protected Fetcher fetcher;

    /** Purchase/grant execution handler. */
    protected Buy buy;

    /** Update checker (Spigot integration). */
    protected Version version;

    /** Periodic task that fetches pending rewards from backend. */
    protected RewardPollingTask pollingTask;

    // ===== Abstract Methods (Implemented by Platform-Specific Subclasses) =====
    // These provide the platform-specific implementations needed for dependency injection.

    /**
     * Returns the platform-specific logger implementation.
     */
    protected abstract PlatformLogger getPlatformLogger();

    /**
     * Returns the platform-specific task scheduler.
     */
    protected abstract PlatformScheduler getScheduler();

    /**
     * Returns the platform-specific server access (player lookup, command dispatch).
     */
    protected abstract RServer getRServer();

    /**
     * Returns the platform adapter (type detection, event registration, utilities).
     */
    protected abstract PlatformAdapter getAdapter();

    /**
     * Returns the HTTP client for backend API calls.
     */
    protected abstract Api getApi();

    /**
     * Returns the main config wrapper (config.yml abstraction).
     */
    protected abstract PlatformConfig getMainConfigWrapper();

    /**
     * Returns the messages config wrapper (messages.yml abstraction).
     */
    protected abstract PlatformConfig getMessagesConfigWrapper();

    /**
     * Returns the rewards config wrapper (rewards.yml abstraction).
     */
    protected abstract PlatformConfig getRewardsConfigWrapper();

    /**
     * Returns the user data config wrapper (userdata.yml abstraction).
     */
    protected abstract PlatformConfig getUserDataConfigWrapper();

    /**
     * Returns the message translator (backend translation client).
     */
    protected abstract Translator getTranslator();

    /**
     * Returns the proxy sender (sends plugin messages across proxy network).
     */
    protected abstract ProxySender getProxySender();

    /**
     * Returns the command executor (executes commands as console with substitution).
     */
    protected abstract PlatformCommandExecutor getCommandExecutor();

    /**
     * Creates and returns the platform-specific GUI implementation.
     */
    protected abstract PlatformGUI createGui();

    /**
     * Returns the platform-specific proxy listener (handles incoming plugin messages).
     */
    protected abstract AbstractProxyListener getCoreProxyListener();

    /**
     * Returns the reward polling task (fetches pending grants periodically).
     */
    protected abstract RewardPollingTask getPollingTask();

    /**
     * Plugin enable handler - called by the platform when the plugin loads.
     *
     * <p>Orchestrates the full initialization sequence:
     *   1. Config initialization
     *   2. Platform initialization and backend validation setup
     *   3. Messager initialization (with translator support)
     *   4. Fetcher initialization (backend polling)
     *   5. Buy initialization (purchase/grant handler)
     *   6. RewardFetcher initialization (pending grant fetching)
     *   7. GUI initialization (platform-specific rendering)
     *   8. Version checker initialization (update checking)
     *   9. CoreCommands initialization (/rewardsx command)
     *   10. Dependency injection via adapter.setupDependencies()
     *   11. RewardPollingTask initialization (periodic polling setup)
     *   12. Platform startup sequence (validation, service startup)
     *   13. Final checks (platform startup confirmation)
     *
     * <p>All exceptions are caught and logged - if anything fails, onDisable is called
     * to clean up gracefully.
     */
    public void onEnable() {
        getPlatformLogger().info("Initializing RewardsX Core...");

        try {
            // --- Step 1: Initialize Config ---
            // Reads from disk, auto-migrates missing keys from defaults, provides unified config access.
            this.config = new Config(
                    getRServer(),
                    getAdapter(),
                    new MainConfig(),        // Default values for main config
                    new MessagesConfig(),    // Default values for messages config
                    getMainConfigWrapper(),
                    getMessagesConfigWrapper(),
                    getRewardsConfigWrapper(),
                    getUserDataConfigWrapper()
            );

            getPlatformLogger().info("Config initialized.");

            // --- Step 2: Initialize Platform ---
            // Detects platform type, manages validation state, orchestrates startup.
            this.platform = new Platform(config, getApi(), getAdapter());
            getPlatformLogger().info("Platform initialized: " + (this.platform != null));

            // --- Step 3: Wire Platform to API ---
            // The API needs to know the platform for debug mode and platform ID.
            getApi().setPlatform(platform);

            // --- Step 4: Initialize Messager ---
            // Handles localized strings and optional translation to player's language.
            this.messager = new Messager(
                    getMessagesConfigWrapper(),
                    platform,
                    getTranslator(),
                    getScheduler(),
                    getAdapter()
            );

            // --- Step 5: Initialize Fetcher ---
            // Backend polling client - fetches rewards list and update checks.
            this.fetcher = new Fetcher(
                    getAdapter(),
                    getScheduler(),
                    getApi(),
                    config,
                    platform,
                    null,           // Buy is set later
                    60              // Poll interval: 60 seconds
            );

            // --- Step 6: Initialize Buy and Wire to Fetcher ---
            // Handles purchase/grant execution. Needs Fetcher for player stats.
            this.buy = new Buy(
                    getAdapter(),
                    getRServer(),
                    getApi(),
                    platform,
                    config,
                    messager,
                    getProxySender(),
                    getScheduler(),
                    getCommandExecutor()
            );

            // Create the reward fetcher (fetches pending grants from backend).
            RewardFetcher rewardFetcher = new RewardFetcher(getApi(), getPlatformLogger(), buy);

            // Wire cross-dependencies: Fetcher needs Buy for executing grants.
            fetcher.setBuy(buy);
            fetcher.setRewardFetcher(rewardFetcher);

            // --- Step 7: Initialize GUI ---
            // Platform-specific GUI rendering (inventory on Spigot, web on Velocity, etc.).
            this.rewardsGUI = createGui();

            // --- Step 8: Initialize Version Checker ---
            // Checks for plugin updates via Spigot integration.
            this.version = new Version(fetcher, messager, getAdapter());

            // --- Step 9: Initialize CoreCommands ---
            // /rewardsx command handler (buy, connect, reload, version, etc.).
            this.coreCommands = new CoreCommands(
                    rewardsGUI,
                    messager,
                    config,
                    version,
                    platform,
                    fetcher,
                    buy,
                    getPlatformLogger(),
                    getProxySender()
            );

            // --- Step 10: Inject Core Dependencies into Adapter ---
            // The adapter now has references to GUI, commands, and proxy listener
            // so it can register them with the platform and wire up event handlers.
            getAdapter().setupDependencies(rewardsGUI, coreCommands, getCoreProxyListener());

            // --- Step 11: Initialize RewardPollingTask ---
            // Dedicated task for periodic polling of pending rewards.
            this.pollingTask = new RewardPollingTask(
                    getScheduler(),
                    rewardFetcher,
                    this.config,
                    this.buy,
                    this.fetcher.getInFlight()
            );

            // --- Step 12: Platform Startup Sequence ---
            // Validates server registration, starts services, registers commands.
            // This is async - the callback continues in the platform.isValid() callback.
            platform.checkAndStart(fetcher, messager, version);

            // --- Step 13: Final Checks ---
            // Verify platform state and cache initial flags.
            coreCommands.checkPlatformOnStartup();

            getPlatformLogger().info("RewardsX Core enabled successfully!");

        } catch (Exception e) {
            // If anything fails, log the error and shut down gracefully.
            getPlatformLogger().warning("Failed to enable RewardsX Core: " + e.getMessage());
            e.printStackTrace();
            onDisable();
        }
    }

    /**
     * Plugin disable handler - called by the platform when the plugin unloads.
     *
     * <p>Logs a shutdown message. Platform-specific cleanup (canceling tasks, closing
     * connections, etc.) is handled by the platform adapter's cancelAllTasks() method
     * (called during Platform.checkAndStart() on the next startup, or on actual shutdown).
     */
    public void onDisable() {
        getPlatformLogger().info("RewardsX is shutting down...");
    }
}