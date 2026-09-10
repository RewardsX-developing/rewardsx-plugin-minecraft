package net.r_developing.rewardsx.platform;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.PlatformPlugin;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.commands.Commands;
import net.r_developing.rewardsx.listener.VelocityPluginMessageListener;

import java.nio.file.Path;

public class VelocityPlatformAdapter implements PlatformAdapter {
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("rewardsx:command");

    @Getter
    private final Plugin plugin;
    @Getter
    private final ProxyServer proxyServer;
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;
    private final Path dataPath;

    private PlatformGUI gui;
    private Buy buy;
    private CoreCommands coreCommands;
    private AbstractProxyListener proxyListener;

    public VelocityPlatformAdapter(Plugin plugin, ProxyServer proxyServer, PlatformLogger logger, Path dataPath, Buy buy) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataPath = dataPath;
        this.buy = buy;
        this.scheduler = new VelocitySchedulerWrapper(plugin, proxyServer);
    }

    @Override
    public void cancelAllTasks() {
        scheduler.cancelAll();
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        scheduler.runTaskLater(task, delayTicks);
    }

    @Override
    public PlatformPlugin getInstance() {
        return new VelocityPlatformPlugin(plugin);
    }

    @Override
    public PlatformGUI getGUI() {
        return gui;
    }

    @Override
    public PlatformLogger getLogger() {
        return logger;
    }

    @Override
    public void setupDependencies(PlatformGUI gui, CoreCommands coreCommands, AbstractProxyListener proxyListener) {
        this.gui = gui;
        this.coreCommands = coreCommands;
        this.proxyListener = proxyListener;
    }

    @Override
    public void registerCommandsAndEvents() {
        // 1. Registra il canale per i Plugin Messages
        proxyServer.getChannelRegistrar().register(CHANNEL);

        // 2. Registra il listener per i Plugin Messages in arrivo
        proxyServer.getEventManager().register(
                plugin,
                new VelocityPluginMessageListener(proxyServer, logger, buy)
        );

        // 3. Registra il comando /rewardsx
        CommandManager commandManager = proxyServer.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("rewardsx")
                .plugin(plugin)
                .build();

        commandManager.register(meta, new Commands(coreCommands, logger, proxyServer));
    }

    @Override
    public Type type() {
        return Type.VELOCITY;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void error(String msg) {
        logger.warning("[ERROR] " + msg);
    }

    @Override
    public void debug(String msg) {
        logger.info("[DEBUG] " + msg);
    }

    @Override
    public void warning(String msg) {
        logger.warning(msg);
    }

    @Override
    public void severe(String msg) {
        logger.warning("[SEVERE] " + msg);
    }

    @Override
    public String getName() {
        return plugin.getPluginContainer().getDescription().getName().orElse("unknown");
    }

    @Override
    public String getPluginVersion() {
        return plugin.getPluginContainer().getDescription().getVersion().orElse("unknown");
    }

    @Override
    public boolean isDbEnabled() {
        return false;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public Object pluginInstance() {
        return plugin;
    }

}