package net.r_developing.rewardsx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;

public class Messager {
    private final Config config;
    private String prefix;
    private FileConfiguration messagesConfig;
    private final Platform platform;
    private final Translator translator;
    private final Plugin plugin;

    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loadedLanguages = new ConcurrentHashMap<>();
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    public Messager(Config config, Platform platform, Translator translator, Plugin plugin) {
        this.config = config;
        this.platform = platform;
        this.messagesConfig = config.getMessagesConfig();
        this.prefix = this.messagesConfig.getString("prefix");
        this.translator = translator;
        this.plugin = plugin;

        if(platform.isTranslator()) loadTranslations(platform.getLanguage());
    }

    private void loadTranslations(String langCode) {
        if (loadedLanguages.getOrDefault(langCode, false)) {
            if(platform.isDebug()) {
                System.out.println("Language already loaded: " + langCode);
            }
            return;
        }

        /*if(!isLoading.compareAndSet(false, true)) {
            if(platform.isDebug()) {
                System.out.println("Translation loading already in progress");
            }
            return;
        }*/

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Set<String> keys = messagesConfig.getKeys(true);

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
                                    System.out.println("Translated [" + langCode + "] " + count + "/" + total + " - " + key);
                                }
                            })
                            .exceptionally(ex -> {
                                translationCache.get(langCode).put(key, message);
                                if (platform.isDebug()) {
                                    System.out.println("Translation failed for " + key + ": " + ex.getMessage());
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
                        System.out.println("All translations loaded for " + langCode + ": " +
                                translationCache.get(langCode).size() + " entries");
                    }
                }).join();

            } catch (Exception e) {
                if(platform.isDebug()) {
                    System.out.println("Error during translation loading: " + e.getMessage());
                    e.printStackTrace();
                }
                isLoading.set(false);
            }
        });
    }

    public String get(String key) {
        if(platform.isDebug()) {
            System.out.println(key);
        }

        Map<String, String> langCache = translationCache.get(platform.getLanguage());
        if(langCache != null) {
            String cached = langCache.get(key);
            if(cached != null) {
                return (prefix + cached).replace("&", "§").trim();
            }
        }

        String message = this.messagesConfig.getString(key);
        if(message == null) return prefix + key;
        return (prefix + message).replace("&", "§").trim();
    }

    public String getNoPrefix(String key) {
        if(platform.isDebug()) System.out.println(key);

        Map<String, String> langCache = translationCache.get(platform.getLanguage());
        if (langCache != null) {
            String cached = langCache.get(key);
            if (cached != null) {
                return cached.replace("&", "§").trim();
            }
        }

        String message = this.messagesConfig.getString(key);
        if(message == null) return key;
        return message.replace("&", "§").trim();
    }

    public String custom(String message) {
        return message.replace("&", "§").trim();
    }

    public void reload() {
        this.messagesConfig = config.getMessagesConfig();
        this.prefix = this.messagesConfig.getString("prefix");

        translationCache.clear();
        loadedLanguages.clear();

        if(platform.isTranslator()) loadTranslations(platform.getLanguage());
    }
}

