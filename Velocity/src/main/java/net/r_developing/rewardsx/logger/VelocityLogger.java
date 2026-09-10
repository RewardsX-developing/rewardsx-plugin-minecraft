package net.r_developing.rewardsx.logger;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.config.Config;
import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import org.slf4j.Logger;

public class VelocityLogger implements PlatformLogger {
    private final Logger logger;
    private final PlatformConfig config;
    private final Plugin plugin;

    public VelocityLogger(Plugin plugin, Logger logger, PlatformConfig config) {
        this.plugin = plugin;
        this.logger = logger;
        this.config = config;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void debug(String message) {
        if (config != null && config.getBoolean("debug", false)) {
            logger.info("[DEBUG] {}", message);
        }
    }

    @Override
    public void warning(String msg) {
        logger.warn(msg);
    }
}