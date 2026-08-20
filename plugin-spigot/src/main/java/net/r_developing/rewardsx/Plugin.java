package net.r_developing.rewardsx;

import net.r_developing.rewardsx.api.core.RewardsXCore;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    // Istanza del Core che gestisce la logica
    private RewardsXCore core;

    @Override
    public void onEnable() {
        // Inizializza il core passando "this" (il plugin) come contesto
        this.core = new SpigotRewardsxCore(this);
        this.core.onEnable();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.onDisable();
        }
    }
}