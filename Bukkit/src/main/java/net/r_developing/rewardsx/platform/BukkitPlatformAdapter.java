package net.r_developing.rewardsx.platform;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.platform.PlatformPlugin;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;
import net.r_developing.rewardsx.commands.Commands;
import net.r_developing.rewardsx.gui.RewardsGUI;
import net.r_developing.rewardsx.listener.SpigotProxyListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;

public class BukkitPlatformAdapter implements PlatformAdapter {
    private final Plugin plugin;
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;

    private PlatformGUI gui;
    private CoreCommands coreCommands;
    private AbstractProxyListener coreProxyListener;

    public BukkitPlatformAdapter(Plugin plugin, PlatformLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.scheduler = new BukkitSchedulerWrapper(plugin);
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        scheduler.runTaskLater(task, delayTicks);
    }

    @Override
    public PlatformPlugin getInstance() {
        return new BukkitPlatformPlugin(plugin);
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
        this.coreProxyListener = proxyListener;
    }

    @Override
    public void registerCommandsAndEvents() {
        if (gui == null || coreCommands == null || coreProxyListener == null) {
            logger.warning("Cannot register commands/events: Dependencies not injected (gui="
                    + gui + ", coreCommands=" + coreCommands + ", coreProxyListener=" + coreProxyListener + ")");
            return;
        }

        // 1. Registra i comandi del plugin (/rewardsx)
        Commands spigotCommands = new Commands(coreCommands, plugin);

        try {
            Constructor<PluginCommand> constructor = PluginCommand.class
                    .getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);

            PluginCommand pluginCommand = constructor.newInstance("rewardsx", plugin);
            pluginCommand.setExecutor(spigotCommands);
            pluginCommand.setTabCompleter(spigotCommands);

            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(plugin.getServer());

            commandMap.register(plugin.getDescription().getName(), pluginCommand);
        } catch (Exception e) {
            logger.warning("Failed to register command via reflection: " + e.getMessage());
        }
    }

    @Override
    public Type type() {
        return Type.SPIGOT;
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
        logger.info(ChatColor.AQUA + "[DEBUG] " + msg);
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
        return plugin.getDescription().getName();
    }

    @Override
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isDbEnabled() {
        return false;
    }

    @Override
    public Path getDataPath() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Object pluginInstance() {
        return plugin;
    }
}