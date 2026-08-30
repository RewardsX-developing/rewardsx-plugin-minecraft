package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.proxy.AbstractProxyListener;

import java.nio.file.Path;

public interface PlatformAdapter {
    void cancelAllTasks();
    void runTaskLater(Runnable task, long delayTicks);

    PlatformPlugin getIstance();

    PlatformGUI getGUI();
    PlatformLogger getLogger();

    void setupDependencies(PlatformGUI gui, CoreCommands coreCommands, AbstractProxyListener proxyListener);

    void registerCommandsAndEvents();

    enum Type { SPIGOT, BUNGEECORD, VELOCITY }

    PlatformAdapter.Type type();

    // logger indipendente da SLF4J / java.util
    void info(String msg);
    void error(String msg);
    void debug(String msg);
    void warning(String msg);
    void severe(String msg);

    String getName();

    String getPluginVersion();

    boolean isDbEnabled();

    Path getDataPath();

    /** Ritorna l’oggetto “plugin” da dare al costruttore di SQLite */
    Object pluginInstance();
}