package net.r_developing.rewardsx.api.core.language;

import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.Platform;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.network.Translator;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;

/**
 * Centralized message lookup and translation system.
 *
 * <p>Provides localized strings (stored in messages.yml) with optional async
 * translation to match the player's language. Messages are keyed by a string
 * (e.g. "rewardReceived", "noPermission") and fetched via get(key) or getNoPrefix(key).
 *
 * <p>Translators are only active if platform.isTranslator() is true. When active,
 * loadTranslations() fetches each message from the config and sends it to a backend
 * translator (probably a cloud service), then caches the result. If translation fails,
 * falls back to the original message.
 *
 * <p>All messages get color code conversion (&amp; to §) applied on retrieval, and
 * an optional prefix (e.g. "[RewardsX]") prepended (unless getNoPrefix() is used).
 */
public class Messager {
    /** Platform detection and config access. */
    private final Platform platform;
    /** HTTP client for backend translation requests. */
    private final Translator translator;
    /** Scheduler to run async tasks. */
    private final PlatformScheduler scheduler;
    /** Console logger. */
    private final PlatformLogger logger;

    /** The messages.yml config file. */
    private final PlatformConfig messagesConfig;
    /** Message prefix (e.g. "[RewardsX]") prepended to all get() calls. */
    private String prefix;

    /**
     * Translation cache: langCode -> (key -> translated message).
     *
     * <p>Thread-safe (ConcurrentHashMap) since translations are loaded async
     * while messages may be requested from the main thread.
     */
    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();

    /**
     * Tracks which languages have been loaded.
     *
     * <p>Used to avoid redundant translation requests if the same language
     * is requested multiple times.
     */
    private final Map<String, Boolean> loadedLanguages = new ConcurrentHashMap<>();

    /** Flag to prevent multiple simultaneous translation loads. */
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    /**
     * Constructor injection - dependencies provided by the platform bootstrap.
     *
     * @param messagesConfig the messages.yml config
     * @param platform       platform detection and language info
     * @param translator     backend translation client
     * @param scheduler      async task scheduler
     * @param adapter        platform-specific utilities (for the logger)
     */
    public Messager(PlatformConfig messagesConfig, Platform platform, Translator translator, PlatformScheduler scheduler, PlatformAdapter adapter) {
        this.messagesConfig = messagesConfig;
        this.platform = platform;
        this.translator = translator;
        this.scheduler = scheduler;
        this.logger = adapter.getLogger();

        this.prefix = this.messagesConfig.getString("prefix");

        // If translation is enabled, kick off async loading of the current language.
        if (platform.isTranslator()) {
            loadTranslations(platform.getLanguage());
        }
    }

    /**
     * Asynchronously loads translations for a language.
     *
     * <p>Fetches all message keys from the config, sends each one to the translator,
     * and caches the results. Falls back to the original message if translation fails.
     * Uses CompletableFuture to parallelize translation requests, then waits for all
     * to complete before marking the language as loaded.
     *
     * <p>Skips loading if the language is already loaded (checked via loadedLanguages map).
     * Runs async via scheduler to avoid blocking the main thread.
     *
     * @param langCode the language code (e.g. "en", "it", "es")
     */
    private void loadTranslations(String langCode) {
        // If this language is already loaded, skip to avoid redundant work.
        if (loadedLanguages.getOrDefault(langCode, false)) {
            if (platform.isDebug()) {
                logger.info("Language already loaded: " + langCode);
            }
            return;
        }

        // Run translation loading on an async thread to avoid blocking.
        scheduler.runAsync(() -> {
            try {
                // Fetch all message keys from the config file.
                Set<String> keys = messagesConfig.getKeys(false);

                // If there are no keys, mark the language as loaded and exit early.
                if (keys == null || keys.isEmpty()) {
                    loadedLanguages.put(langCode, true);
                    isLoading.set(false);
                    return;
                }

                // Initialize an empty cache for this language.
                translationCache.computeIfAbsent(langCode, ignored -> new ConcurrentHashMap<>());

                // Collect translation futures so we can wait for all to complete.
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                AtomicInteger completed = new AtomicInteger(0);
                int total = keys.size();

                // For each message key, initiate an async translation request.
                for (String key : keys) {
                    String message = messagesConfig.getString(key);

                    // Skip empty or null messages.
                    if (message == null || message.isEmpty()) {
                        continue;
                    }

                    // Send the message to the translator and get a CompletableFuture.
                    CompletableFuture<Void> future = translator.translate(message, langCode)
                            // On success, store the translation in the cache.
                            .thenAccept(translated -> {
                                translationCache.get(langCode).put(key, translated);

                                // Debug logging: print progress.
                                if (platform.isDebug()) {
                                    int count = completed.incrementAndGet();
                                    logger.info("Translated [" + langCode + "] " + count + "/" + total + " - " + key);
                                }
                            })
                            // On failure, fall back to the original message and log the error.
                            .exceptionally(ex -> {
                                translationCache.get(langCode).put(key, message);
                                if (platform.isDebug()) {
                                    logger.warning("Translation failed for " + key + ": " + ex.getMessage());
                                }
                                return null;
                            });

                    futures.add(future);
                }

                // Wait for all translation requests to complete.
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );

                // Once all are done, mark the language as loaded and log stats.
                allFutures.thenRun(() -> {
                    loadedLanguages.put(langCode, true);
                    isLoading.set(false);
                    if (platform.isDebug()) {
                        logger.info("All translations loaded for " + langCode + ": " +
                                translationCache.get(langCode).size() + " entries");
                    }
                }).join(); // Block this async thread until all translations complete.

            } catch (Exception e) {
                if (platform.isDebug()) {
                    logger.warning("Error during translation loading: " + e.getMessage());
                    e.printStackTrace();
                }
                isLoading.set(false);
            }
        });
    }

    /**
     * Retrieves a localized message by key, with prefix prepended.
     *
     * <p>Lookup order:
     *   1. Check the translation cache for the current language
     *   2. Fall back to the messagesConfig (original language)
     *   3. If not found, return the key itself (as a fallback)
     *
     * <p>Color codes (&amp;) are converted to Minecraft section symbols (§) and
     * whitespace is trimmed. The prefix is prepended (unless the message is just the key).
     *
     * @param key the message key (e.g. "rewardReceived")
     * @return the localized message with prefix
     */
    public String get(String key) {
        if (platform.isDebug()) {
            logger.info("Requested message key: " + key);
        }

        // Try the translation cache first.
        Map<String, String> langCache = translationCache.get(platform.getLanguage());
        if (langCache != null) {
            String cached = langCache.get(key);
            if (cached != null) {
                // Return the cached translation with prefix applied.
                return (prefix + cached).replace("&", "§").trim();
            }
        }

        // Fall back to the config file.
        String message = this.messagesConfig.getString(key);
        if (message == null) return prefix + key;
        return (prefix + message).replace("&", "§").trim();
    }

    /**
     * Retrieves a localized message by key, without prefix.
     *
     * <p>Same lookup logic as get(), but the prefix is not prepended.
     * Useful for messages that should not have a plugin prefix (e.g. reward descriptions).
     *
     * @param key the message key
     * @return the localized message without prefix
     */
    public String getNoPrefix(String key) {
        if (platform.isDebug()) logger.info("Requested message key (no prefix): " + key);

        // Try the translation cache first.
        Map<String, String> langCache = translationCache.get(platform.getLanguage());
        if (langCache != null) {
            String cached = langCache.get(key);
            if (cached != null) {
                // Return the cached translation without prefix.
                return cached.replace("&", "§").trim();
            }
        }

        // Fall back to the config file.
        String message = this.messagesConfig.getString(key);
        if (message == null) return key;
        return message.replace("&", "§").trim();
    }

    /**
     * Utility to process a custom message string (not from config).
     *
     * <p>Applies color code conversion and trimming, but does not add a prefix
     * or look up translations. Used when a message is constructed dynamically
     * (e.g. "Reward " + rewardName).
     *
     * @param message the raw message string
     * @return the processed message with colors converted
     */
    public String custom(String message) {
        return message.replace("&", "§").trim();
    }

    /**
     * Reloads all messages from disk and clears the translation cache.
     *
     * <p>Called on /rewardsx reload. This lets admins edit messages.yml and see
     * changes immediately without a restart. Also, re-triggers translation loading
     * if translation is enabled.
     */
    public void reload() {
        // Reload the config from disk.
        this.messagesConfig.reload();
        // Re-read the prefix (in case it changed).
        this.prefix = this.messagesConfig.getString("prefix");

        // Clear the translation cache and language tracking so translations are reloaded.
        translationCache.clear();
        loadedLanguages.clear();

        // Re-trigger translation loading if enabled.
        if (platform.isTranslator()) {
            loadTranslations(platform.getLanguage());
        }
    }
}