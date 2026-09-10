package net.r_developing.rewardsx.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.r_developing.rewardsx.api.core.platform.PlatformAdapter;
import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class VelocityPlayer implements RPlayer {
    private final Player player;

    public VelocityPlayer(Player player) {
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
        return player.getUsername();
    }

    @Override
    public void sendMessage(String msg) {
        if (msg == null) return;
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(
                LegacyComponentSerializer.legacySection().serialize(
                        LegacyComponentSerializer.legacySection().deserialize(msg)
                )
        );
        player.sendMessage(component);
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComp = title != null ? LegacyComponentSerializer.legacySection().deserialize(title) : Component.empty();
        Component subComp = subtitle != null ? LegacyComponentSerializer.legacySection().deserialize(subtitle) : Component.empty();

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        player.showTitle(Title.title(titleComp, subComp, times));
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        sendTitle(title, subtitle, 10, 70, 20);
    }

    @Override
    public void performCommand(String cmd) {
        // Su Velocity si usa spoiler/command execution senza slash iniziale
        String cleanCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        player.spoofChatInput("/" + cleanCmd);
    }

    @Override
    public void closeInventory() {
        // Su Velocity non esistono inventory GUI
    }

    @Override
    public void openInventory(Object inv) {
        // Non supportato su Velocity
    }

    @Override
    public boolean hasPermission(String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public boolean isOnline() {
        return player.isActive();
    }

    @Override
    public void sendPluginMessage(PlatformAdapter plugin, String channel, byte[] data) {
        Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) {
            plugin.getLogger().debug("[RewardsX] sendPluginMessage: player has no active backend server connection");
            return;
        }

        ServerConnection server = currentServer.get();
        String serverName = server.getServerInfo().getName();

        plugin.getLogger().debug(
                "[RewardsX] sendPluginMessage -> channel=" + channel +
                        " | server=" + serverName +
                        " | player=" + player.getUsername() +
                        " | dataLength=" + (data != null ? data.length : 0)
        );

        if (data == null || data.length == 0) {
            plugin.getLogger().debug("[RewardsX] sendPluginMessage: data is null or empty, skipping.");
            return;
        }

        MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier.from(channel);
        server.sendPluginMessage(identifier, data);

        plugin.getLogger().debug("[RewardsX] sendPluginMessage: packet sent to backend server=" + serverName);
    }

    @Override
    public void sendMessageWithUrl(String message, String url) {
        Component text = LegacyComponentSerializer.legacySection().deserialize(message);
        Component clickableUrl = Component.text(" -> " + url)
                .clickEvent(ClickEvent.openUrl(url));

        player.sendMessage(text.append(clickableUrl));
    }
}