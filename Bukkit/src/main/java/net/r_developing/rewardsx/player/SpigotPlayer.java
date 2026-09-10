package net.r_developing.rewardsx.player;

import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class SpigotPlayer implements RPlayer {
    private final Player player;

    public SpigotPlayer(Player player) {
        this.player = player;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public Object getPlayer() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getPlayerName() {
        return player.getName();
    }

    @Override
    public void sendMessage(String msg) {
        player.sendMessage(msg);
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        player.sendTitle(title, subtitle, 10, 70, 20);
    }

    @Override
    public void performCommand(String cmd) {
        player.performCommand(cmd);
    }

    @Override
    public void closeInventory() {
        player.closeInventory();
    }

    @Override
    public void openInventory(Object inv) {
        if (inv instanceof Inventory) {
            player.openInventory((Inventory) inv);
        }
    }

    @Override
    public boolean hasPermission(String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public void sendPluginMessage(PlatformAdapter plugin, String channel, byte[] byteArray) {
        // Bukkit richiede la connessione di almeno un giocatore per inviare il pacchetto al Proxy
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);

        if (player == null) {
            plugin.getLogger().debug(
                    "[RewardsX] sendPluginMessage: nessun giocatore online, impossibile inviare su channel=" + channel
            );
            return;
        }

        JavaPlugin nativePlugin = (JavaPlugin) plugin.getInstance().getPlugin();

        plugin.getLogger().debug(
                "[RewardsX] sendPluginMessage -> channel=" + channel +
                        " | player=" + player.getName() +
                        " | dataLength=" + (byteArray != null ? byteArray.length : 0)
        );

        if (byteArray == null || byteArray.length == 0) {
            plugin.getLogger().debug("[RewardsX] sendPluginMessage: data is null or empty, skipping.");
            return;
        }

        player.sendPluginMessage(nativePlugin, channel, byteArray);

        plugin.getLogger().debug(
                "[RewardsX] sendPluginMessage: packet inviato da player=" + player.getName() + " su channel=" + channel
        );
    }

    @Override
    public void sendMessageWithUrl(String message, String url) {
        net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', message)
        );
        component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url
        ));
        player.spigot().sendMessage(component);
    }
}