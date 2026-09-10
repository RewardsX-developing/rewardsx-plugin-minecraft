package net.r_developing.rewardsx.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import net.r_developing.rewardsx.api.core.commands.CoreCommands;
import net.r_developing.rewardsx.api.core.platform.PlatformCommandExecutor;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.player.SpigotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Commands implements CommandExecutor, TabCompleter, PlatformCommandExecutor {
    private final CoreCommands coreCommands;
    private final JavaPlugin plugin;
    private final boolean isPapiEnabled;

    public Commands(CoreCommands coreCommands, JavaPlugin plugin) {
        this.coreCommands = coreCommands;
        this.plugin = plugin;
        this.isPapiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    // ==================== CommandExecutor (per /rewardsx) ====================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            RPlayer rPlayer = new SpigotPlayer(player);
            return coreCommands.execute(rPlayer, args);
        } else {
            // Console - gestisce comandi admin da console
            return coreCommands.executeConsole(args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // La console non ha tab complete o ha solo comandi admin
        if (!(sender instanceof Player)) {
            if (args.length == 1) {
                return List.of("reload", "version");
            }
            return List.of();
        }
        return coreCommands.getTabCompletions(args);
    }

    // ==================== PlatformCommandExecutor (per Buy, ecc.) ====================

    @Override
    public void dispatchConsoleCommand(String command, RPlayer targetPlayer, String targetName) {
        String finalCmd = command;

        // Rimpiazza %player% nativamente
        if (targetName != null) {
            finalCmd = finalCmd.replace("%player%", targetName);
        }

        // Risolvi PlaceholderAPI se il giocatore è online e PAPI è abilitato
        if (isPapiEnabled && targetPlayer != null && targetPlayer.getPlayer() instanceof Player) {
            Player bukkitPlayer = (Player) targetPlayer.getPlayer();
            finalCmd = PlaceholderAPI.setPlaceholders(bukkitPlayer, finalCmd);
        }

        // Esegue da console
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
    }

    // ==================== Metodi non usati (lasciati vuoti o rimossi) ====================

    // Questi metodi non servono se usi già onCommand ed executeConsole del CoreCommands
    @Override
    public boolean execute(RPlayer sender, String[] args) {
        // Non usato, delega a onCommand
        return false;
    }

    @Override
    public boolean executeConsole(String senderName, String[] args) {
        // Non usato, delega a onCommand -> coreCommands.executeConsole
        return false;
    }

    @Override
    public List<String> getTabCompletions(String[] args) {
        // Non usato, delega a onTabComplete
        return null;
    }
}