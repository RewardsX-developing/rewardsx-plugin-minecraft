package net.r_developing.rewardsx.player;

import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayerOffline;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class SpigotPlayerOffline implements RPlayerOffline {
    private final OfflinePlayer offlinePlayer;

    public SpigotPlayerOffline(OfflinePlayer offlinePlayer) {
        this.offlinePlayer = offlinePlayer;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public Object getPlayer() {
        return null;
    }

    @Override
    public UUID getUniqueId() {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public String getPlayerName() {
        return null;
    }

    @Override
    public void sendMessage(String msg) {

    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {

    }

    @Override
    public void sendTitle(String title, String subtitle) {

    }

    @Override
    public void performCommand(String cmd) {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public void openInventory(Object inv) {

    }

    @Override
    public boolean hasPermission(String perm) {
        return false;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public void sendPluginMessage(PlatformAdapter plugin, String s, byte[] byteArray) {

    }

    @Override
    public void sendMessageWithUrl(String message, String url) {

    }

    @Override
    public String getName() {
        return offlinePlayer.getName();
    }

    @Override
    public boolean isBanned() {
        return offlinePlayer.isBanned();
    }

    @Override
    public boolean hasPlayedBefore() {
        return offlinePlayer.hasPlayedBefore();
    }

    @Override
    public long getFirstPlayed() {
        return offlinePlayer.getFirstPlayed();
    }

    @Override
    public long getLastPlayed() {
        return offlinePlayer.getLastPlayed();
    }
}