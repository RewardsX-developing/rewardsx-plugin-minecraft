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

public class Messager {
    private final Platform platform;
    private final Translator translator;
    private final PlatformScheduler scheduler;
    private final PlatformLogger logger;

    private PlatformConfig messagesConfig;
    private String prefix;

    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loadedLanguages = new ConcurrentHashMap<>();
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    // Dependency Injection tramite costruttore
    public Messager(PlatformConfig messagesConfig, Platform platform, Translator translator, PlatformScheduler scheduler, PlatformAdapter adapter) {
        this.messagesConfig = messagesConfig;
        this.platform = platform;
        this.translator = translator;
        this.scheduler = scheduler;
        this.logger = adapter.getLogger();

        this.prefix = this.messagesConfig.getString("prefix");

        if (platform.isTranslator()) {
            loadTranslations(platform.getLanguage());
        }
    }

    private void loadTranslations(String langCode) {
        if (loadedLanguages.getOrDefault(langCode, false)) {
            if (platform.isDebug()) {
                logger.info("Language already loaded: " + langCode);
            }
            return;
        }

        // Astrazione dello scheduler
        scheduler.runAsync(() -> {
            try {
                Set<String> keys = messagesConfig.getKeys(false);

                if (keys == null || keys.isEmpty()) {
                    loadedLanguages.put(langCode, true);
                    isLoading.set(false);
                    return;
                }

                translationCache.computeIfAbsent(langCode, k -> new ConcurrentHashMap<>());

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                AtomicInteger completed = new AtomicInteger(0);
                int total = keys.size();

                for (String key : keys) {
                    String message = messagesConfig.getString(key);

                    if (message == null || message.isEmpty()) {
                        continue;
                    }

                    CompletableFuture<Void> future = translator.translate(message, langCode)
                            .thenAccept(translated -> {
                                translationCache.get(langCode).put(key, translated);

                                if (platform.isDebug()) {
                                    int count = completed.incrementAndGet();
                                    logger.info("Translated [" + langCode + "] " + count + "/" + total + " - " + key);
                                }
                            })
                            .exceptionally(ex -> {
                                translationCache.get(langCode).put(key, message);
                                if (platform.isDebug()) {
                                    logger.warning("Translation failed for " + key + ": " + ex.getMessage());
                                }
                                return null;
                            });

                    futures.add(future);
                }

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );

                allFutures.thenRun(() -> {
                    loadedLanguages.put(langCode, true);
                    isLoading.set(false);
                    if (platform.isDebug()) {
                        logger.info("All translations loaded for " + langCode + ": " +
                                translationCache.get(langCode).size() + " entries");
                    }
                }).join();

            } catch (Exception e) {
                if (platform.isDebug()) {
                    logger.warning("Error during translation loading: " + e.getMessage());
                    e.printStackTrace();
                }
                isLoading.set(false);
            }
        });
    }

    public String get(String key) {
        if (platform.isDebug()) {
            logger.info("Requested message key: " + key);
        }

        Map<String, String> langCache = translationCache.get(platform.getLanguage());
        if (langCache != null) {
            String cached = langCache.get(key);
            if (cached != null) {
                return (prefix + cached).replace("&", "§").trim();
            }
        }

        String message = this.messagesConfig.getString(key);
        if (message == null) return prefix + key;
        return (prefix + message).replace("&", "§").trim();
    }

    public String getNoPrefix(String key) {
        if (platform.isDebug()) logger.info("Requested message key (no prefix): " + key);

        Map<String, String> langCache = translationCache.get(platform.getLanguage());
        if (langCache != null) {
            String cached = langCache.get(key);
            if (cached != null) {
                return cached.replace("&", "§").trim();
            }
        }

        String message = this.messagesConfig.getString(key);
        if (message == null) return key;
        return message.replace("&", "§").trim();
    }

    public String custom(String message) {
        return message.replace("&", "§").trim();
    }

    public void reload() {
        this.messagesConfig.reload(); // Delegato all'adapter
        this.prefix = this.messagesConfig.getString("prefix");

        translationCache.clear();
        loadedLanguages.clear();

        if (platform.isTranslator()) {
            loadTranslations(platform.getLanguage());
        }
    }
}