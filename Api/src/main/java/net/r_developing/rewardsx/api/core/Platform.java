package net.r_developing.rewardsx.api.core;

import lombok.Getter;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Api;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.network.RewardPollingTask;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.updater.Version;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Core platform facade and initialization orchestrator.
 *
 * <p>Holds configuration values, validates the server's registration with the backend,
 * and orchestrates the startup sequence. Bridges between the platform-specific code
 * (via PlatformAdapter) and the core logic (Fetcher, API, Config, etc.).
 *
 * <p>Responsibilities:
 *   - Read config values (ID, key, language, debug mode, etc.)
 *   - Detect running platform (Spigot, BungeeCord, Velocity)
 *   - Validate server registration with backend
 *   - Cache validation results to avoid repeated API calls
 *   - Orchestrate startup: cancel tasks, start polling, register commands, start fetcher
 */
public class Platform {
    /** Config file access. */
    private final Config config;
    /** HTTP client for backend API. */
    private final Api api;
    /** Platform-specific utilities. */
    private final PlatformAdapter adapter;

    /** Server name (from backend) - cached after validation. */
    @Getter
    private String name;

    /** Cached validation result (true/false). Volatile because it's updated from async callback. */
    private volatile Boolean cachedValidation = null;

    /** Timestamp of last validation check. */
    private volatile long lastValidationTime = 0;

    /** How long to cache validation results (30 seconds) before re-checking with backend. */
    private static final long CACHE_DURATION = 30000; // 30 seconds

    /**
     * Constructor injection.
     *
     * @param config  config file access
     * @param api     HTTP client
     * @param adapter platform-specific utilities
     */
    public Platform(Config config, Api api, PlatformAdapter adapter) {
        this.config = config;
        this.api = api;
        this.adapter = adapter;
    }

    /**
     * Returns the server's platform ID.
     *
     * <p>Set via /rewardsx secret <key> during first-time setup. Used to identify
     * this server to the backend.
     *
     * @return the platform ID from config
     */
    public String getId() {
        return config.getMainConfig().getString("platform_id");
    }

    /**
     * Returns the server's secret key (deprecated name).
     *
     * <p>Same as getPlatformKey() - returns "platform_key" from config.
     * This is a legacy method name for backward compatibility.
     *
     * @return the secret key from config
     */
    public String getKey() {
        return config.getMainConfig().getString("platform_secret");
    }

    /**
     * Detects whether the server is running as a proxy backend or on a proxy network.
     *
     * <p>Returns true if:
     *   - Running on BungeeCord or Velocity (both are proxy servers), OR
     *   - Running on Spigot WITH proxy mode enabled in config
     *
     * <p>Used to determine where GUIs are opened (local or via proxy message) and
     * whether to register commands locally or delegate to proxy.
     *
     * @return true if running as a proxy or in proxy-backend mode
     */
    public boolean isProxyOrBungee() {
        boolean isProxyMode = config.getMainConfig().getBoolean("proxy", false);
        boolean isBungee = (adapter.type() == PlatformAdapter.Type.BUNGEECORD || adapter.type() == PlatformAdapter.Type.VELOCITY);
        return isBungee || (isProxyMode && adapter.type() == PlatformAdapter.Type.SPIGOT);
    }

    /**
     * Orchestrates the full startup sequence.
     *
     * <p>Called once when the plugin enables. Performs these steps:
     *   1. Cancel any existing tasks (cleanup from previous run)
     *   2. Clear cached validation so next check is fresh
     *   3. Start the reward polling task
     *   4. Register commands and events (unless in proxy-backend mode)
     *   5. Validate server registration with backend
     *   6. If valid: start fetcher, log welcome message, check for updates
     *   7. If invalid: prompt admin to link server with /rewardsx secret
     *
     * <p>The validation is async - the callback continues the startup once the
     * backend responds.
     *
     * @param fetcher  the reward fetcher (started if validation succeeds)
     * @param messager message strings
     * @param version  version checker (run after validation)
     */
    public void checkAndStart(Fetcher fetcher, Messager messager, Version version) {
        // Cancel all previously scheduled tasks to avoid duplicates.
        adapter.cancelAllTasks();
        // Clear cached validation to force a fresh check.
        cachedValidation = null;
        lastValidationTime = 0;

        // Start the reward polling task.
        RewardPollingTask pollingTask = adapter.getInstance().getPollingTask();
        pollingTask.start();

        // Determine if this server should register commands locally.
        boolean isProxyMode = config.getMainConfig().getBoolean("proxy", false);
        boolean isBungee = (adapter.type() == PlatformAdapter.Type.BUNGEECORD || adapter.type() == PlatformAdapter.Type.VELOCITY);

        // Register commands and events unless running as a Spigot backend on a proxy.
        if (isBungee || (!isProxyMode && adapter.type() == PlatformAdapter.Type.SPIGOT)) {
            adapter.registerCommandsAndEvents();
        } else {
            // Spigot in proxy-backend mode: commands are handled by the proxy, not locally.
            adapter.getLogger().info("Running in proxy backend mode: commands handled by Bungee.");
        }

        // Validate server registration asynchronously.
        isValid(valid -> {
            if (valid) {
                // Server is registered - proceed with full startup.

                // Send a verification ping to the backend.
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", getId());
                api.send("POST", "verify-platform", payload, ignored -> {});

                // Start the reward fetcher (polls for pending grants).
                fetcher.start();

                // Log a welcome message (with Minecraft color codes converted to ANSI for console).
                adapter.getLogger().info(toAnsi(String.format(messager.get("welcome"), getName())));

                // Check for updates after a short delay (100 ticks = 5 seconds).
                adapter.runTaskLater(() -> version.checkVersion(null), 100L);
            } else {
                // Server is not registered - prompt admin to link it.
                adapter.getLogger().info(toAnsi(messager.get("configurePlatform")));
            }
        });
    }

    /**
     * Checks if debug mode is enabled in config.
     *
     * <p>When true, additional diagnostic logging is output.
     *
     * @return true if debug mode is enabled
     */
    public boolean isDebug() {
        return config.getMainConfig().getBoolean("debug");
    }

    /**
     * Checks if proxy mode is enabled in config.
     *
     * <p>When true, this Spigot server acts as a backend on a proxy network.
     *
     * @return true if proxy mode is enabled
     */
    public boolean isProxy() {
        return config.getMainConfig().getBoolean("proxy");
    }

    /**
     * Checks if message translation is enabled.
     *
     * <p>When true, message strings are sent to the backend translator to match
     * the player's language.
     *
     * @return true if translator is enabled
     */
    public boolean isTranslator() {
        return config.getMainConfig().getBoolean("translator");
    }

    /**
     * Returns the language/locale code for this server.
     *
     * <p>Extracted from the config "language" setting (e.g. "en_US" -> "en").
     * Defaults to "en" if not set.
     *
     * @return the language code (e.g. "en", "it", "es")
     */
    public String getLanguage() {
        String locale = config.getMainConfig().getString("language");
        if (locale == null || locale.isEmpty()) return "en";
        return locale.split("_")[0].toLowerCase();
    }

    /**
     * Validates this server's registration with the backend.
     *
     * <p>Makes an async HTTP call to the "platform" endpoint to check if the server
     * is registered. Caches the result for 30 seconds to avoid repeated API calls.
     * If cached result is recent, returns it immediately without calling the backend.
     *
     * <p>On success, extracts the server name from the response and caches it.
     * On failure, reports false to the callback.
     *
     * @param callback invoked with true if valid/registered, false otherwise
     */
    public void isValid(Consumer<Boolean> callback) {
        // Guard: API must be initialized first.
        if (!api.init()) {
            callback.accept(Boolean.FALSE);
            return;
        }

        long now = System.currentTimeMillis();
        // Check if cached result is still fresh (within 30 seconds).
        if (cachedValidation != null && (now - lastValidationTime) < CACHE_DURATION) {
            if (isDebug()) {
                System.out.println("Using cached validation result: " + cachedValidation);
            }
            callback.accept(cachedValidation);
            return;
        }

        // Cached result expired or doesn't exist - query the backend.
        Map<String, Object> payload = new HashMap<>();
        api.send("GET", "platform", payload, response -> {
            if (response == null) {
                System.err.println("Request failed or returned null");
                callback.accept(Boolean.FALSE);
                return;
            }

            // Check the "success" field from the response.
            boolean valid = "true".equalsIgnoreCase(String.valueOf(response.get("success")));
            if (valid) {
                // Extract server name from the platform object.
                @SuppressWarnings("unchecked")
                Map<String, Object> platformObj = (Map<String, Object>) response.get("platform");
                this.name = String.valueOf(platformObj.get("name_server"));
                // Cache the validation result.
                cachedValidation = (Boolean) true;
                lastValidationTime = System.currentTimeMillis();

                if (isDebug()) {
                    System.out.println("Validation result from API: true, name: " + name);
                }
            }

            callback.accept(Boolean.valueOf(valid));
        });
    }

    /**
     * Converts Minecraft color codes to ANSI escape sequences for console output.
     *
     * <p>Minecraft uses § (section symbol) followed by a hex digit for colors.
     * This method translates them to ANSI escape codes so console output displays
     * colors correctly. Example: §c (red) becomes \u001B91m (ANSI red).
     *
     * <p>Used to colorize console log messages that include Minecraft formatting.
     *
     * @param msg the message with Minecraft color codes (§0-§f, §r)
     * @return the message with ANSI escape codes substituted
     */
    private static String toAnsi(String msg) {
        return msg.replace("§0", "\u001B[30m").replace("§1", "\u001B[34m").replace("§2", "\u001B[32m")
                .replace("§3", "\u001B[36m").replace("§4", "\u001B[31m").replace("§5", "\u001B[35m")
                .replace("§6", "\u001B[33m").replace("§7", "\u001B[37m").replace("§8", "\u001B[90m")
                .replace("§9", "\u001B[94m").replace("§a", "\u001B[92m").replace("§b", "\u001B[96m")
                .replace("§c", "\u001B[91m").replace("§d", "\u001B[95m").replace("§e", "\u001B[93m")
                .replace("§f", "\u001B[97m").replace("§r", "\u001B[0m");
    }
}