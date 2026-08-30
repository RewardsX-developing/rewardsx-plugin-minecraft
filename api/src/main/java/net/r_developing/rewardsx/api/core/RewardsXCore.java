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
 * Classe principale astratta del Core.
 */
public abstract class RewardsXCore {

    // --- Istanze del Core (Condivise) ---
    protected Platform platform;
    protected PlatformGUI rewardsGUI;
    protected Config config;
    protected CoreCommands coreCommands;
    protected Messager messager;
    protected Fetcher fetcher;
    protected Buy buy;
    protected Version version;
    protected RewardPollingTask pollingTask;

    // --- Dipendenze iniettate dalle sottoclassi ---
    protected abstract PlatformLogger getPlatformLogger();
    protected abstract PlatformScheduler getScheduler();
    protected abstract RServer getRServer();
    protected abstract PlatformAdapter getAdapter();
    protected abstract Api getApi();

    // Configurazioni
    protected abstract PlatformConfig getMainConfigWrapper();
    protected abstract PlatformConfig getMessagesConfigWrapper();
    protected abstract PlatformConfig getRewardsConfigWrapper();
    protected abstract PlatformConfig getUserDataConfigWrapper();

    protected abstract Translator getTranslator();
    protected abstract ProxySender getProxySender();
    protected abstract PlatformCommandExecutor getCommandExecutor();

    protected abstract PlatformGUI createGui();
    protected abstract AbstractProxyListener getCoreProxyListener();

    protected abstract RewardPollingTask getPollingTask();

    public void onEnable() {
        getPlatformLogger().info("Initializing RewardsX Core...");

        try {
            // 1. Config
            this.config = new Config(
                    getRServer(),
                    getAdapter(),
                    new MainConfig(),
                    new MessagesConfig(),
                    getMainConfigWrapper(),
                    getMessagesConfigWrapper(),
                    getRewardsConfigWrapper(),
                    getUserDataConfigWrapper()
            );

            getPlatformLogger().info("Config initialized.");

            // 2. Platform
            this.platform = new Platform(config, getApi(), getAdapter());
            getPlatformLogger().info("Platform initialized: " + (this.platform != null));

            // 3. API Platform
            getApi().setPlatform(platform);

            // 4. Inizializza il Messager
            this.messager = new Messager(
                    getMessagesConfigWrapper(),
                    platform,
                    getTranslator(),
                    getScheduler(),
                    getAdapter()
            );

            // 5. Inizializza il Fetcher
            this.fetcher = new Fetcher(
                    getAdapter(),
                    getScheduler(),
                    getApi(),
                    config,
                    platform,
                    null,
                    60
            );

            // 6. Inizializza Buy e collegalo al Fetcher
            this.buy = new Buy(
                    getAdapter(),
                    getRServer(),
                    fetcher,
                    getApi(),
                    platform,
                    config,
                    messager,
                    getProxySender(),
                    getScheduler(),
                    getCommandExecutor()
            );

            RewardFetcher rewardFetcher = new RewardFetcher(getApi(), getPlatformLogger(), buy);

            fetcher.setBuy(buy);
            fetcher.setRewardFetcher(rewardFetcher);

            // 7. Inizializza la GUI
            this.rewardsGUI = createGui();

            // 8. Inizializza Version Checker
            this.version = new Version(fetcher, messager, getAdapter());

            // 9. Inizializza CoreCommands
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

            // 10. INIETTA TUTTE LE DIPENDENZE COMPLESSIVE NELL'ADAPTER
            getAdapter().setupDependencies(rewardsGUI, coreCommands, getCoreProxyListener());

            this.pollingTask = new RewardPollingTask(
                    getScheduler(),
                    rewardFetcher,
                    this.config,
                    this.buy,
                    this.fetcher.getInFlight()
            );

            // 11. Avvia la validazione della piattaforma (che chiama adapter.registerCommandsAndEvents())
            platform.checkAndStart(fetcher, messager, version);

            coreCommands.checkPlatformOnStartup();

            getPlatformLogger().info("RewardsX Core enabled successfully!");

        } catch (Exception e) {
            getPlatformLogger().warning("Failed to enable RewardsX Core: " + e.getMessage());
            e.printStackTrace();
            onDisable();
        }
    }

    public void onDisable() {
        getPlatformLogger().info("RewardsX is shutting down...");
    }
}