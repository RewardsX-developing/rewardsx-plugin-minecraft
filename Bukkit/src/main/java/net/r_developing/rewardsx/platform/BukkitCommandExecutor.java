package net.r_developing.rewardsx.platform;

import net.r_developing.rewardsx.api.core.platform.PlatformCommandExecutor;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BukkitCommandExecutor implements PlatformCommandExecutor {
    private final JavaPlugin plugin;
    private final boolean isPapiEnabled;

    public BukkitCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
        // Controlla se PlaceholderAPI è presente e abilitato
        this.isPapiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public void dispatchConsoleCommand(String command, RPlayer targetPlayer, String targetName) {
        String finalCmd = command;

        // 1. Rimpiazza %player% nativamente (fallback se PAPI non è installato)
        if (targetName != null) {
            finalCmd = finalCmd.replace("%player%", targetName);
        }

        // 2. Risolvi PlaceholderAPI se il giocatore è online e PAPI è abilitato
        if (isPapiEnabled && targetPlayer != null && targetPlayer.getPlayer() instanceof Player) {
            Player bukkitPlayer = (Player) targetPlayer.getPlayer();
            try {
                finalCmd = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(bukkitPlayer, finalCmd);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse PlaceholderAPI placeholders: " + e.getMessage());
            }
        }

        // 3. Esegue il comando da console
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute command '" + finalCmd + "': " + e.getMessage());
        }
    }

    @Override
    public boolean execute(RPlayer sender, String[] args) {
        return false;
    }

    @Override
    public boolean executeConsole(String senderName, String[] args) {
        return false;
    }

    @Override
    public List<String> getTabCompletions(String[] args) {
        return null;
    }
}